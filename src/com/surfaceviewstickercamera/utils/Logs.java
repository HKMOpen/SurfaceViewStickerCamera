package com.surfaceviewstickercamera.utils;

import android.util.Log;

/**
 * @author YL
 * @Description: (这里用一句话描述这个类的作用)
 * @date 2016/5/11 11:08
 */
public class Logs
{
    public static final void out(Object object)
    {
        Log.d("Camera", object + "--".toString());
    }
}
