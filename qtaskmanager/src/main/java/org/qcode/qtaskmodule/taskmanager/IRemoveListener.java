package org.qcode.qtaskmodule.taskmanager;

import org.qcode.qtaskmodule.entities.Task;

/**
 * qqliu
 * 2016/7/14.
 */
public interface IRemoveListener<T> {
    boolean needRemove(Task<T> removeTask, T objectData);
}
