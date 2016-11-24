package org.qcode.taskmanager.base.utils;

import java.util.Collection;

/**
 * qqliu
 * 2016/7/14.
 */
public class Utils {

    public static boolean isEmpty(Collection<?> collection) {
        return null == collection || collection.size() <= 0;
    }

    public static void assertNotNull(Object object, String exceptionTip) {
        if (null == object) {
            throw new RuntimeException(exceptionTip);
        }
    }
}
