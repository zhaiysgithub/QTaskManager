package org.qcode.taskmanager.taskpool.impl;

import org.qcode.taskmanager.ITaskComparator;

/**
 * qqliu
 * 2016/11/24.
 */

public class DefaultTaskComparator<T> implements ITaskComparator<T> {

    @Override
    public boolean isSame(T taskLeft, T taskRight) {
        if(null == taskLeft && null == taskRight) {
            return true;
        }

        if(null == taskLeft || null == taskRight) {
            return false;
        }

        return taskLeft.equals(taskRight);
    }
}
