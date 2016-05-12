package com.surfaceviewstickercamera;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.surfaceviewstickercamera.view.CameraView;
import com.surfaceviewstickercamera.view.CameraView.CameraViewCallback;

import java.io.File;

public class CameraActivity extends FragmentActivity
{
	private CameraView cameraView;
	@Override
	protected void onCreate(@Nullable Bundle arg0)
	{
		super.onCreate(arg0);
		cameraView=new CameraView(this);
		cameraView.setPictureFile(new File("/mnt/sdcard/test.jpg"));
		setContentView(cameraView);
		cameraView.setCameraViewCallback(new CameraViewCallback()
		{
			@Override
			public void onLight()
			{
				cameraView.toggleLight();
			}

			@Override
			public void onCancel()
			{

			}

			@Override
			public void onUsePhoto()
			{

			}

			@Override
			public void onTakePictured(String picturePath)
			{
				Toast.makeText(CameraActivity.this,"picturePath="+picturePath,Toast.LENGTH_LONG).show();
			}
		});
	}
}
