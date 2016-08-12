package org.qcode.qtaskmodule.taskexecutor;

/**
 * qqliu
 * 2016/7/14.
 */
public interface ITaskExecutorHelper<T> {
    void setTaskExecutor(ITaskExecutor<T> executor);

    void startExecute();

    void stopExecute();

    void addTask(T newTask);

    void addTaskDelayed(T newTask, int delay);

    void addTaskBeforeAnchor(T newTask, T anchorTask);

    void addTaskAfterAnchor(T newTask, T anchorTask);

    void removeTask(T task);
}
