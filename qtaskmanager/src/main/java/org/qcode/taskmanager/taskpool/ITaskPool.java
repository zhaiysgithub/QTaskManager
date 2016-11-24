package org.qcode.taskmanager.taskpool;

import org.qcode.taskmanager.ITaskManager;
import org.qcode.taskmanager.entities.TaskInfo;

/**
 * 任务池的抽象接口
 *
 * qqliu
 * 2016/7/14.
 */
public interface ITaskPool<T> extends ITaskManager<T> {

    /***
     * 任务池内的任务发生变化的监听器；
     * 仅支持一级任务的监听，
     * 其他附属于一级任务的任务变化无通知
     *
     * @param listener
     */
    void setTaskNodeChangeListener(ITaskNodeChangeListener<T> listener);

    /***
     * 从任务池内取出一个当前应当执行的任务
     * @return
     * @throws InterruptedException
     */
    TaskInfo<T> popTask() throws InterruptedException;
}
