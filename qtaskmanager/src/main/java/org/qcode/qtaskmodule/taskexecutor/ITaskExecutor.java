package org.qcode.qtaskmodule.taskexecutor;

/**
 * qqliu
 * 2016/7/14.
 */
public interface ITaskExecutor<T> {
    boolean needRemove(T existedTask, T newAddTask);

    void executeTask(T task);
}
