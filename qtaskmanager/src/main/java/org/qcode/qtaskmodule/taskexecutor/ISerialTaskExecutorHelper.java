package org.qcode.qtaskmodule.taskexecutor;

/**
 * qqliu
 * 2016/7/15.
 */
public interface ISerialTaskExecutorHelper<T> {
    void notifyTaskBegin(T task);
    void notifyTaskFinish(T task);
}
