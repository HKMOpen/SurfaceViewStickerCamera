package com.surfaceviewstickercamera.view;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.surfaceviewstickercamera.R;

@SuppressWarnings("deprecation")
public class CameraView extends LinearLayout implements OnClickListener
{
	private CameraSurfaceView camerasurfaceView;
	private Button btn_back;
	private Button btn_take;
	private Button btn_photo;

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
		LayoutInflater.from(getContext()).inflate(R.layout.view_cameraview,this,true);

		camerasurfaceView=(CameraSurfaceView) findViewById(R.id.camerasurfaceView);

		btn_back=(Button) findViewById(R.id.btn_back);
		btn_take=(Button) findViewById(R.id.btn_take);
		btn_photo=(Button) findViewById(R.id.btn_photo);

		btn_back.setOnClickListener(this);
		btn_take.setOnClickListener(this);
		btn_photo.setOnClickListener(this);
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
			mCallback.onBack();
		}
		else if(v==btn_take)
		{
			camerasurfaceView.takePicture(new PictureCallback()
			{
				@Override
				public void onPictureTaken(byte[] data, Camera camera)
				{
					mCallback.onTakePictured(data);
					// 拍完照后，重新开始预览，画面则继续播放
					// camera.startPreview();
				}
			});
		}
		else if(v==btn_photo)
		{
			mCallback.onUsePhoto();
		}
	}


	private CameraViewCallback mCallback;

	public void setCameraViewCallback(CameraViewCallback callback)
	{
		this.mCallback=callback;
	}

	public static interface CameraViewCallback
	{
		void onBack();

		void onUsePhoto();

		void onTakePictured(byte[] data);
	}
}
