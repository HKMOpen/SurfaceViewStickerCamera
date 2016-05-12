package com.surfaceviewstickercamera.view;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

import com.surfaceviewstickercamera.utils.Logs;
import com.surfaceviewstickercamera.utils.TaskProgressDialog;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


@SuppressWarnings("deprecation")
public class CameraSurfaceView extends SurfaceView implements OnClickListener
{
	//相机对象
	private Camera cameraObject = null;
	//相机参数
	private Camera.Parameters parameters = null;
	//图片大小
	private Camera.Size pictureSize = null;
	//预览大小
	private Camera.Size previewSize = null;
	//最小预览界面的分辨率
	private static final int MIN_PREVIEW_PIXELS = 480 * 320;
	//最大宽高比差
	private static final double MAX_ASPECT_DISTORTION = 0.15;
	//绘制回调
	private SurfaceCallback surfaceCallback=new SurfaceCallback();
	//按下时的X,Y点
	private float pointX, pointY;
	//状态
	private int MODE_STATE;
	//状态-聚焦
	static final int MODE_FOCUS = 1;
	//状态-缩放
	static final int MODE_ZOOM = 2;
	//放大缩小值
	int curZoomValue = 0;
	//两个手指按下的距离
	private float spacingDist;
	//屏幕宽
	private int screenWidth=0;
	//屏幕高
	private int screenHeight=0;
	//文件保存
	private File mPictureFile;

	public CameraSurfaceView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initSurfaceView();
	}

	public CameraSurfaceView(Context context)
	{
		super(context);
		initSurfaceView();
	}

	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initSurfaceView();
	}

	private void initSurfaceView()
	{
		screenWidth=getResources().getDisplayMetrics().widthPixels;
		screenHeight=getResources().getDisplayMetrics().heightPixels;

		SurfaceHolder surfaceHolder = getHolder();
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceHolder.setKeepScreenOn(true);
		setFocusable(true);
		setOnClickListener(this);
		setBackgroundColor(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
		getHolder().removeCallback(surfaceCallback);
		getHolder().addCallback(surfaceCallback);
	}


	public String getFlashMode()
	{
		return parameters.getFlashMode();
	}

	public void setFlashMode(String mode)
	{
		parameters.setFlashMode(mode);
		cameraObject.setParameters(parameters);
		cameraObject.startPreview();
	}

	public void setPictureFile(File pictureFile)
	{
		this.mPictureFile=pictureFile;
	}

	private final class SurfaceCallback implements SurfaceHolder.Callback
	{
		@Override
		public void surfaceCreated(SurfaceHolder holder)
		{
			if (null == cameraObject)
			{
				try
				{
					cameraObject = Camera.open();
					cameraObject.setPreviewDisplay(holder);
					initCamera();
					cameraObject.startPreview();
				} catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
		{

		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder)
		{
			try
			{
				if (cameraObject != null)
				{
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					cameraObject.setParameters(parameters);
					cameraObject.stopPreview();
					cameraObject.release();
					cameraObject = null;
				}

			} catch (Exception e)
			{
				// 相机已经关了
			}
		}
	}

	private void initCamera()
	{
		parameters = cameraObject.getParameters();
		parameters.setPictureFormat(PixelFormat.JPEG);
		//parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
		setUpPicSize(parameters);
		setUpPreviewSize(parameters);
		if (pictureSize != null)
		{
			parameters.setPictureSize(pictureSize.width, pictureSize.height);
		}
		if (previewSize != null)
		{
			parameters.setPreviewSize(previewSize.width, previewSize.height);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
		} else
		{
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}
		setDispaly(parameters, cameraObject);
		try
		{
			cameraObject.setParameters(parameters);

		} catch (Exception e)
		{
			e.printStackTrace();
		}
		cameraObject.startPreview();
		cameraObject.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
	}


	// 控制图像的正确显示方向
	private void setDispaly(Camera.Parameters parameters, Camera camera)
	{
		if (Build.VERSION.SDK_INT >= 8)
		{
			setDisplayOrientation(camera, 90);

		} else
		{
			parameters.setRotation(90);
		}
	}

	// 实现的图像的正确显示
	private void setDisplayOrientation(Camera camera, int i)
	{
		Method downPolymorphic;
		try
		{
			downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{ int.class });
			if (downPolymorphic != null)
			{
				downPolymorphic.invoke(camera, new Object[]{ i });
			}
		} catch (Exception e)
		{
			Logs.out("图像出错");
		}
	}



	private void setUpPicSize(Camera.Parameters parameters)
	{
		if (pictureSize != null)
		{
			return;
		} else
		{
			pictureSize = findBestPictureResolution();
			return;
		}
	}

	//找出最适合的存储图片的分辨率Picture
	private Camera.Size findBestPictureResolution()
	{
		Camera.Parameters cameraParameters = cameraObject.getParameters();
		List<Camera.Size> supportedPicResolutions = cameraParameters.getSupportedPictureSizes(); // 至少会返回一个值
		StringBuilder picResolutionSb = new StringBuilder();
		for (Camera.Size supportedPicResolution : supportedPicResolutions)
		{
			picResolutionSb.append(supportedPicResolution.width).append('x').append(supportedPicResolution.height).append(" ");
		}

		Logs.out("Supported picture resolutions: " + picResolutionSb);
		Camera.Size defaultPictureResolution = cameraParameters.getPictureSize();
		Logs.out("default picture resolution " + defaultPictureResolution.width + "x"+ defaultPictureResolution.height);

		// 排序
		List<Camera.Size> sortedSupportedPicResolutions = new ArrayList<Camera.Size>(supportedPicResolutions);
		Collections.sort(sortedSupportedPicResolutions, new Comparator<Camera.Size>()
		{
			@Override
			public int compare(Camera.Size a, Camera.Size b)
			{
				int aPixels = a.height * a.width;
				int bPixels = b.height * b.width;
				if (bPixels < aPixels)
				{
					return -1;
				}
				if (bPixels > aPixels)
				{
					return 1;
				}
				return 0;
			}
		});

		// 移除不符合条件的分辨率
		double screenAspectRatio = (double) screenWidth/ (double) screenHeight;
		Iterator<Camera.Size> it = sortedSupportedPicResolutions.iterator();
		while (it.hasNext())
		{
			Camera.Size supportedPreviewResolution = it.next();
			int width = supportedPreviewResolution.width;
			int height = supportedPreviewResolution.height;
			// 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
			// 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
			// 因此这里要先交换然后在比较宽高比
			boolean isCandidatePortrait = width > height;
			int maybeFlippedWidth = isCandidatePortrait ? height : width;
			int maybeFlippedHeight = isCandidatePortrait ? width : height;
			double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
			double distortion = Math.abs(aspectRatio - screenAspectRatio);
			if (distortion > MAX_ASPECT_DISTORTION)
			{
				it.remove();
				continue;
			}
		}

		// 如果没有找到合适的，并且还有候选的像素，对于照片，则取其中最大比例的，而不是选择与屏幕分辨率相同的
		if (!sortedSupportedPicResolutions.isEmpty())
		{
			return sortedSupportedPicResolutions.get(0);
		}

		// 没有找到合适的，就返回默认的
		return defaultPictureResolution;
	}



	private void setUpPreviewSize(Camera.Parameters parameters)
	{
		if (previewSize != null)
		{
			return;
		} else
		{
			previewSize = findBestPreviewResolution();
		}
	}


	//找出最适合的预览界面分辨率Preview
	private Camera.Size findBestPreviewResolution()
	{
		Camera.Parameters cameraParameters = cameraObject.getParameters();
		Camera.Size defaultPreviewResolution = cameraParameters.getPreviewSize();

		List<Camera.Size> rawSupportedSizes = cameraParameters.getSupportedPreviewSizes();
		if (rawSupportedSizes == null)
		{
			return defaultPreviewResolution;
		}

		// 按照分辨率从大到小排序
		List<Camera.Size> supportedPreviewResolutions = new ArrayList<Camera.Size>(rawSupportedSizes);
		Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>()
		{
			@Override
			public int compare(Camera.Size a, Camera.Size b)
			{
				int aPixels = a.height * a.width;
				int bPixels = b.height * b.width;
				if (bPixels < aPixels)
				{
					return -1;
				}
				if (bPixels > aPixels)
				{
					return 1;
				}
				return 0;
			}
		});

		StringBuilder previewResolutionSb = new StringBuilder();
		for (Camera.Size supportedPreviewResolution : supportedPreviewResolutions)
		{
			previewResolutionSb.append(supportedPreviewResolution.width).append('x').append(supportedPreviewResolution.height).append(' ');
		}
		Logs.out("Supported preview resolutions: " + previewResolutionSb);

		// 移除不符合条件的分辨率
		double screenAspectRatio = (double) screenWidth
				/ (double) screenHeight;
		Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
		while (it.hasNext())
		{
			Camera.Size supportedPreviewResolution = it.next();
			int width = supportedPreviewResolution.width;
			int height = supportedPreviewResolution.height;

			// 移除低于下限的分辨率，尽可能取高分辨率
			if (width * height < MIN_PREVIEW_PIXELS)
			{
				it.remove();
				continue;
			}

			// 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
			// 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
			// 因此这里要先交换然preview宽高比后在比较
			boolean isCandidatePortrait = width > height;
			int maybeFlippedWidth = isCandidatePortrait ? height : width;
			int maybeFlippedHeight = isCandidatePortrait ? width : height;
			double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
			double distortion = Math.abs(aspectRatio - screenAspectRatio);
			if (distortion > MAX_ASPECT_DISTORTION)
			{
				it.remove();
				continue;
			}

			// 找到与屏幕分辨率完全匹配的预览界面分辨率直接返回
			if (maybeFlippedWidth == screenWidth && maybeFlippedHeight == screenHeight)
			{
				return supportedPreviewResolution;
			}
		}

		// 如果没有找到合适的，并且还有候选的像素，则设置其中最大比例的，对于配置比较低的机器不太合适
		if (!supportedPreviewResolutions.isEmpty())
		{
			Camera.Size largestPreview = supportedPreviewResolutions.get(0);
			return largestPreview;
		}

		// 没有找到合适的，就返回默认的

		return defaultPreviewResolution;
	}

	/**
	 * 两点的距离
	 */
	private float spacing(MotionEvent event)
	{
		if (event == null)
		{
			return 0;
		}
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	private void addZoomIn(int delta)
	{
		try
		{
			Camera.Parameters params = cameraObject.getParameters();
			Log.d("Camera", "Is support Zoom " + params.isZoomSupported());
			if (!params.isZoomSupported())
			{
				return;
			}
			curZoomValue += delta;
			if (curZoomValue < 0)
			{
				curZoomValue = 0;
			} else if (curZoomValue > params.getMaxZoom())
			{
				curZoomValue = params.getMaxZoom();
			}

			if (!params.isSmoothZoomSupported())
			{
				params.setZoom(curZoomValue);
				cameraObject.setParameters(params);
				return;
			} else
			{
				cameraObject.startSmoothZoom(curZoomValue);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean performClick()
	{
		return super.performClick();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		switch (event.getAction() & MotionEvent.ACTION_MASK)
		{
			// 主点按下
			case MotionEvent.ACTION_DOWN:
				pointX = event.getX();
				pointY = event.getY();
				MODE_STATE = MODE_FOCUS;
				break;
			// 副点按下
			case MotionEvent.ACTION_POINTER_DOWN:
				spacingDist = spacing(event);
				// 如果连续两点距离大于10，则判定为多点模式
				if (spacing(event) > 10f)
				{
					MODE_STATE = MODE_ZOOM;
				}
				break;
			case MotionEvent.ACTION_UP:
				performClick();
			case MotionEvent.ACTION_POINTER_UP:
				MODE_STATE = MODE_FOCUS;
				break;
			case MotionEvent.ACTION_MOVE:
				if (MODE_STATE == MODE_FOCUS)
				{
					// pointFocus((int) event.getRawX(), (int)
					// event.getRawY());
				} else if (MODE_STATE == MODE_ZOOM)
				{
					float newDist = spacing(event);
					if (newDist > 10f)
					{
						float tScale = (newDist - spacingDist) / spacingDist;
						if (tScale < 0)
						{
							tScale = tScale * 10;
						}
						addZoomIn((int) tScale);
					}
				}
				break;
		}
		return true;
	}


	@Override
	public void onClick(View v)
	{
		pointFocus((int) pointX, (int) pointY);
	}

	//定点对焦的代码
	private void pointFocus(int x, int y)
	{
		cameraObject.cancelAutoFocus();
		parameters = cameraObject.getParameters();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			showPoint(x, y);
		}
		cameraObject.setParameters(parameters);
		autoFocus();
	}

	private void showPoint(int x, int y)
	{
		if (parameters.getMaxNumMeteringAreas() > 0)
		{
			List<Camera.Area> areas = new ArrayList<Camera.Area>();
			//xy变换了
			int rectY = -x * 2000 / screenWidth + 1000;
			int rectX = y * 2000 / screenHeight - 1000;

			int left = rectX < -900 ? -1000 : rectX - 100;
			int top = rectY < -900 ? -1000 : rectY - 100;
			int right = rectX > 900 ? 1000 : rectX + 100;
			int bottom = rectY > 900 ? 1000 : rectY + 100;
			Rect area1 = new Rect(left, top, right, bottom);
			areas.add(new Camera.Area(area1, 800));
			parameters.setMeteringAreas(areas);
		}

		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
	}

	// 实现自动对焦
	private void autoFocus()
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					sleep(100);

				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				if (cameraObject == null)
				{
					return;
				}
				cameraObject.autoFocus(new Camera.AutoFocusCallback()
				{
					@Override
					public void onAutoFocus(boolean success, Camera camera)
					{
						if (success)
						{
							initCamera();// 实现相机的参数初始化
						}
					}
				});
			}
		};
	}


	private class PictureAsyncTask extends AsyncTask<Void,Void,String>
	{

		private byte[] bytes;
		private TakedPictureCallback pictureCallback;
		private TaskProgressDialog mTaskDialog;

		public PictureAsyncTask(byte[] bytes,TakedPictureCallback pictureCallback)
		{
			this.bytes=bytes;
			this.pictureCallback=pictureCallback;
		}

		@Override
		protected void onPreExecute()
		{
			showLoadDialog();
		}

		@Override
		protected String doInBackground(Void... voids)
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
			int width = options.outHeight > options.outWidth ? options.outWidth : options.outHeight;
			int height = options.outHeight > options.outWidth ? options.outHeight : options.outWidth;
			Logs.out("width="+width+",height="+height);
			options.inJustDecodeBounds = false;
			FileOutputStream outputStream=null;
			try
			{
				if(mPictureFile!=null)
				{
					outputStream = new FileOutputStream(mPictureFile);
					Bitmap croppedImage = decodeRegionCrop(bytes,width,height);
					croppedImage.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
					return mPictureFile.getAbsolutePath();
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if(outputStream!=null)
				{
					try
					{
						outputStream.close();

					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(String path)
		{
			dismissDialog();
			pictureCallback.onPictureTacked(path);
		}

		private void showLoadDialog()
		{
			if(getContext() instanceof FragmentActivity)
			{
				FragmentActivity fragmentActivity= (FragmentActivity) getContext();
				if(fragmentActivity!=null && !fragmentActivity.isFinishing())
				{
					mTaskDialog = new TaskProgressDialog(fragmentActivity);
					mTaskDialog.setMessage("处理中,请稍后...");
					mTaskDialog.setProgressVisiable(true);
					mTaskDialog.setCancelable(false);
					mTaskDialog.setCanceledOnTouchOutside(false);
					mTaskDialog.show();
				}
			}
		}

		private void dismissDialog()
		{
			if(getContext() instanceof FragmentActivity)
			{
				FragmentActivity fragmentActivity= (FragmentActivity) getContext();
				if(fragmentActivity!=null && !fragmentActivity.isFinishing())
				{
					mTaskDialog.dismiss();
				}
			}
		}

		private Bitmap decodeRegionCrop(byte[] data, int width,int height)
		{
			InputStream is = null;
			System.gc();
			Bitmap croppedImage = null;
			try
			{
				is = new ByteArrayInputStream(data);
				BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);

				try
				{
					//options
					BitmapFactory.Options options = new BitmapFactory.Options();
					//565解码
					options.inPreferredConfig = Bitmap.Config.RGB_565;
					//这里的Rect宽高要调换
					croppedImage = decoder.decodeRegion(new Rect(0, 0, height,width),options);
					//旋转90度
					croppedImage=rotateBitmapByDegree(croppedImage,90);

					Logs.out("croppedImage width="+croppedImage.getWidth()+",height="+croppedImage.getHeight());
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
				}
			} catch (Throwable e)
			{
				e.printStackTrace();

			} finally
			{
				try
				{
					is.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			return croppedImage;
		}

		public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
			Bitmap returnBm = null;

			// 根据旋转角度，生成旋转矩阵
			Matrix matrix = new Matrix();
			matrix.postRotate(degree);
			try {
				// 将原始图片按照旋转矩阵进行旋转，并得到新的图片
				returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				return bm;
			}
			if (returnBm != bm) {
				bm.recycle();
			}
			return returnBm;
		}
	}

	public void takePicture(final TakedPictureCallback pictureCallback)
	{
		cameraObject.takePicture(null, null, new Camera.PictureCallback()
		{
			@Override
			public void onPictureTaken(byte[] bytes, Camera camera)
			{
				camera.stopPreview();
				new PictureAsyncTask(bytes,pictureCallback).execute();
			}
		});
	}

	public static interface TakedPictureCallback
	{
		void onPictureTacked(String picturePath);
	}
}
