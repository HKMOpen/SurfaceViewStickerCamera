package com.surfaceviewstickercamera.utils;

import com.surfaceviewstickercamera.CameraApplicaton;

public class DeviceUtils
{
    /**
     * 获取设备的宽度像素点
     */
    public static int getDeviceWidthPixels()
    {
        return CameraApplicaton.getApplication().getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取设备的高度像素点
     *
     */
    public static int getDeviceHeightPixels()
    {
        return CameraApplicaton.getApplication().getResources().getDisplayMetrics().heightPixels;
    }
}
