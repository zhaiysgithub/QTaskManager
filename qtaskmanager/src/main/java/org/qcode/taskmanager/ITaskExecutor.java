package org.qcode.taskmanager;

import org.qcode.taskmanager.entities.DuplicateTaskStrategy;

/**
 * 任务执行器的抽象接口；
 * 定义一个任务执行器应该具有的能力；
 *
 * qqliu
 * 2016/7/14.
 */
public interface ITaskExecutor<T> {

    /**
     * 通知任务执行器遇到重复任务时的执行策略
     * @param strategy
     */
    void setDuplicateTaskStrategy(DuplicateTaskStrategy strategy);

    /***
     * 设置任务执行器的辅助能力接口
     *
     * @param executorAbility
     */
    void setTaskExecutorAbility(ITaskExecutorAbility<T> executorAbility);

    /***
     * 设置在UI
     * @param runOnUIThread
     */
    void setRunOnUIThread(boolean runOnUIThread);

    /***
     * 开始执行
     */
    void startExecute();

    /***
     * 结束执行
     */
    void stopExecute();

    /***
     * 获取任务管理器，用于添加/删除执行任务
     * @return
     */
    ITaskManager<T> getTaskManager();

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
