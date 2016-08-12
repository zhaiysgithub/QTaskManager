package org.qcode.qtaskmodule.taskmanager;

import org.qcode.qtaskmodule.entities.Task;

/**
 * qqliu
 * 2016/7/14.
 */
public interface ISequenceTaskManager<T> {

    void setTaskNodeChangeListener(ITaskNodeChangeListener<T> listener);

    void addTask(T newTask);

    void addTaskDelayed(T newTask, int delay);

    void addTaskBeforeAnchor(T newTask, T anchorTask);

    void addTaskAfterAnchor(T newTask, T anchorTask);

    void addTaskAfterAnchor(T newTask, T anchorTask, boolean waitAnchorFinish);

    void removeTask(T task, IRemoveListener<T> removeListener);

    Task<T> popTask() throws InterruptedException;
}
