package com.surfaceviewstickercamera;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.surfaceviewstickercamera.utils.F;
import com.surfaceviewstickercamera.view.CameraView;
import com.surfaceviewstickercamera.view.CameraView.CameraViewCallback;

public class CameraActivity extends FragmentActivity implements CameraViewCallback
{
	private CameraView cameraView;
	@Override
	protected void onCreate(@Nullable Bundle arg0)
	{
		super.onCreate(arg0);
		cameraView=new CameraView(this);
		setContentView(cameraView);
		cameraView.setCameraViewCallback(this);
	}
	@Override
	public void onBack()
	{
		finish();
	}
	@Override
	public void onUsePhoto()
	{
		
	}
	@Override
	public void onTakePictured(byte[] data)
	{
		F.out("data="+data.length);
		Toast.makeText(this, "dataLength="+data.length,Toast.LENGTH_LONG).show();
	}
	
	
}
