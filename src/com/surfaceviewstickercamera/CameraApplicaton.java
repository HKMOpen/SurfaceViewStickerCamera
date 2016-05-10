package com.surfaceviewstickercamera;

import android.app.Application;

public class CameraApplicaton extends Application
{
	private static CameraApplicaton mApplication;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		mApplication=this;
	}
	
	public static CameraApplicaton getApplication()
	{
		return mApplication;
	}
}
