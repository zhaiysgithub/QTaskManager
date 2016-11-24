package org.qcode.taskmanager;

/**
 * 串行任务执行器接口
 *
 * qqliu
 * 2016/7/15.
 */
public interface ISerialTaskExecutor<T> extends ITaskExecutor<T> {

    /***
     * 通知某个任务开始执行
     * @param task
     */
    void notifyTaskBegin(T task);

    /***
     * 通知某个任务执行完成
     * @param task
     */
    void notifyTaskFinish(T task);
}
