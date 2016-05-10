package com.surfaceviewstickercamera.utils;

import android.util.Log;

public class F
{
	private static final String TAG = "Camera";

	public static final void out(String tag, Object object)
	{
		Log.d(TAG, tag+"--"+object + "--".toString());

	}
	
	public static final void out(Object object)
	{
		Log.d(TAG, object + "--".toString());

	}
}
