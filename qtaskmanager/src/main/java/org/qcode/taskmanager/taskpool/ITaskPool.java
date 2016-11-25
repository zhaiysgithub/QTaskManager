package org.qcode.taskmanager.taskpool;

import org.qcode.taskmanager.ITaskComparator;
import org.qcode.taskmanager.ITaskManager;
import org.qcode.taskmanager.entities.DuplicateTaskStrategy;
import org.qcode.taskmanager.entities.TaskInfo;

/**
 * 任务池的抽象接口
 *
 * qqliu
 * 2016/7/14.
 */
public interface ITaskPool<T> extends ITaskManager<T> {

    /***
     * 设置任务比较器
     * @param comparator
     */
    void setTaskComparator(ITaskComparator<T> comparator);

    /**
     * 通知任务管理器遇到重复任务时的执行策略
     * @param strategy
     */
    void setDuplicateTaskStrategy(DuplicateTaskStrategy strategy);

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
