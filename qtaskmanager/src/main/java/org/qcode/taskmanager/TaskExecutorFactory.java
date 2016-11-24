package org.qcode.taskmanager;

import org.qcode.taskmanager.taskexecutor.impl.DefaultTaskExecutorImpl;
import org.qcode.taskmanager.taskexecutor.impl.SequenceTaskExecutorImpl;
import org.qcode.taskmanager.taskexecutor.impl.SerialTaskExecutorImpl;

/**
 * 框架对外提供的接口，生成多种任务执行器(TaskExecutor)
 * qqliu
 * 2016/11/16.
 */

public class TaskExecutorFactory {

    /***
     * 生成一个默认的任务执行器；
     * @param <T>
     * @return
     */
    public static <T> ITaskExecutor<T> createDefaultTaskExecutor() {
        return new DefaultTaskExecutorImpl<T>();
    }

    /***
     * 生成一个按序任务执行器；
     * 与串行任务执行器不同的是：
     * 一个按序执行处理器只保证顺序，
     * 不保证任务在前一个任务执行完成后执行
     *
     * @param <T>
     * @return
     */
    public static <T> ITaskExecutor<T> createSequenceTaskExecutor() {
        return new SequenceTaskExecutorImpl<T>();
    }

    /***
     * 生成一个串行任务执行器；
     * @param <T>
     * @return
     */
    public static <T> ISerialTaskExecutor<T> createSerialTaskExecutor() {
        return new SerialTaskExecutorImpl<T>();
    }
}
