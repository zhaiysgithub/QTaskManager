package org.qcode.taskmanager.taskpool;

import org.qcode.taskmanager.entities.TaskInfo;

/**
 * 任务节点变化监听器
 * qqliu
 * 2016/8/12.
 */
public interface ITaskNodeChangeListener<T> {

    /***
     * 主节点添加的通知
     */
    void onMainNodeAdded(TaskInfo<T> task);

    /***
     * 主节点移除的通知
     */
    void onMainNodeRemoved(TaskInfo<T> task);
}
