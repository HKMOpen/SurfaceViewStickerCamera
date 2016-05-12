package com.surfaceviewstickercamera.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.surfaceviewstickercamera.R;
import com.surfaceviewstickercamera.utils.Logs;

import java.io.File;

@SuppressWarnings("deprecation")
public class CameraView extends LinearLayout implements OnClickListener
{
	private CameraSurfaceView camerasurfaceView;
	private Button btn_back;
	private Button btn_take;
	private Button btn_photo;
	private Button btn_light;

	public CameraView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView();
	}

	public CameraView(Context context)
	{
		super(context);
		initView();
	}

	private void initView()
	{
		LayoutInflater.from(getContext()).inflate(R.layout.view_thirdparty_surfaceviewcamera_cameraview,this,true);

		camerasurfaceView=(CameraSurfaceView) findViewById(R.id.camerasurfaceView);

		btn_back=(Button) findViewById(R.id.btn_back);
		btn_take=(Button) findViewById(R.id.btn_take);
		btn_photo=(Button) findViewById(R.id.btn_photo);
		btn_light=(Button) findViewById(R.id.btn_light);

		btn_back.setOnClickListener(this);
		btn_take.setOnClickListener(this);
		btn_photo.setOnClickListener(this);
		btn_light.setOnClickListener(this);
	}


	public void setPictureFile(File file)
	{
		camerasurfaceView.setPictureFile(file);
	}


	public void toggleLight()
	{
		Logs.out("camerasurfaceView.getFlashMode()="+camerasurfaceView.getFlashMode());
		//打开则要关闭
		if(Camera.Parameters.FLASH_MODE_TORCH.equals(camerasurfaceView.getFlashMode()))
		{
			if(!camerasurfaceView.isEnabled() || camerasurfaceView.getFlashMode()==null || "".equals(camerasurfaceView.getFlashMode()))
			{
				return;
			}

			if(!camerasurfaceView.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF))
			{
				camerasurfaceView.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				btn_light.setText("FLASH_MODE_OFF");
			}

		}else//关闭则要打开
		{
			if(!camerasurfaceView.isEnabled() || camerasurfaceView.getFlashMode()==null)
			{
				return;
			}
			if(!camerasurfaceView.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH))
			{
				camerasurfaceView.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				btn_light.setText("FLASH_MODE_TORCH");
			}
		}
	}

	@Override
	public void onClick(View v)
	{
		if(mCallback==null)
		{
			return;
		}

		if(v==btn_back)
		{
			mCallback.onCancel();
		}
		else if(v==btn_take)
		{
			camerasurfaceView.takePicture(new CameraSurfaceView.TakedPictureCallback()
			{
				@Override
				public void onPictureTacked(String picturePath)
				{
					mCallback.onTakePictured(picturePath);
				}
			});
		}
		else if(v==btn_photo)
		{
			mCallback.onUsePhoto();
		}
		else if(v==btn_light)
		{
			mCallback.onLight();
		}
	}


	private CameraViewCallback mCallback;

	public void setCameraViewCallback(CameraViewCallback callback)
	{
		this.mCallback=callback;
	}

	public static interface CameraViewCallback
	{
		void onLight();

		void onCancel();

		void onUsePhoto();

		void onTakePictured(String picturePath);
	}
}
