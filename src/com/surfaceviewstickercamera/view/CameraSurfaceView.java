package com.surfaceviewstickercamera.view;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

import com.surfaceviewstickercamera.utils.DeviceUtils;
import com.surfaceviewstickercamera.utils.F;

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
		SurfaceHolder surfaceHolder = getHolder();
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceHolder.setKeepScreenOn(true);
		setFocusable(true);
		setOnClickListener(this);
		setBackgroundColor(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
		getHolder().removeCallback(surfaceCallback);
		getHolder().addCallback(surfaceCallback);
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
			F.out("图像出错");
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

		F.out("Supported picture resolutions: " + picResolutionSb);
		Camera.Size defaultPictureResolution = cameraParameters.getPictureSize();
		F.out("default picture resolution " + defaultPictureResolution.width + "x"+ defaultPictureResolution.height);

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
		double screenAspectRatio = (double) DeviceUtils.getDeviceWidthPixels()/ (double) DeviceUtils.getDeviceHeightPixels();
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
		F.out("Supported preview resolutions: " + previewResolutionSb);

		// 移除不符合条件的分辨率
		double screenAspectRatio = (double) DeviceUtils.getDeviceWidthPixels()
				/ (double) DeviceUtils.getDeviceHeightPixels();
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
			if (maybeFlippedWidth == DeviceUtils.getDeviceWidthPixels()&& maybeFlippedHeight == DeviceUtils.getDeviceHeightPixels())
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
			int rectY = -x * 2000 / DeviceUtils.getDeviceWidthPixels() + 1000;
			int rectX = y * 2000 / DeviceUtils.getDeviceHeightPixels() - 1000;

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


	public void takePicture(Camera.PictureCallback pictureCallback)
	{
		cameraObject.takePicture(null, null, pictureCallback);
	}
}
