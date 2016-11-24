package org.qcode.taskmanager;

/**
 * 任务执行器的辅助能力；
 * 此接口定义一些框架无法处理的能力，
 * 比如判断两个任务是否相同，执行某个任务等；
 *
 * qqliu
 * 2016/7/14.
 */
public interface ITaskExecutorAbility<T> extends ITaskComparator<T> {

    /***
     * 执行任务
     *
     * @param task
     */
    void executeTask(T task);
}
