package org.qcode.taskmanager.base.utils;

import android.util.Log;

/**
 * qqliu
 * 2016/7/17.
 */
public class Logging {
    private static final String TAG_PREFIX = "TaskManager_";

    public static final void d(String tag, String module, String message) {
        Log.d(TAG_PREFIX + tag, module + " ---> " + message);
    }

    public static final void d(String tag, String message) {
        d(tag, "", message);
    }

    public static final void d(String tag, String message, Throwable throwable) {
        d(tag, "", message, throwable);
    }

    public static final void d(String tag, String module, String message, Throwable throwable) {
        Log.d(TAG_PREFIX + tag, module + " ---> " + message, throwable);
    }
}
