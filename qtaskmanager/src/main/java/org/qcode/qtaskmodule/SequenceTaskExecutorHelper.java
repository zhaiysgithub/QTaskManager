package org.qcode.qtaskmodule;

import org.qcode.qtaskmodule.entities.Task;

/**
 * 顺序的执行任务管理
 * qqliu
 * 2016/7/14.
 */
public class SequenceTaskExecutorHelper<T> extends TaskExecutorHelper<T> {

    @Override
    protected void executeTask(Task<T> task) {
        if(null != task) {
            mTaskExecutor.executeTask(task.getBaseTask());
        }
    }
}
