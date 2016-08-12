package org.qcode.qtaskmodule.taskexecutor;

/**
 * qqliu
 * 2016/7/15.
 */
public interface IDefaultTaskExecutorHelper<T> extends ISerialTaskExecutorHelper<T> {

    void addTaskAfterAnchor(T newTask, T anchorTask, boolean waitAnchorFinish);

}
