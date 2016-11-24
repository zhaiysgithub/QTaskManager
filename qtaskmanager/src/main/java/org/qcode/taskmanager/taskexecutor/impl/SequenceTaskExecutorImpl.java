package org.qcode.taskmanager.taskexecutor.impl;

import org.qcode.taskmanager.base.utils.Utils;
import org.qcode.taskmanager.entities.TaskInfo;
import org.qcode.taskmanager.taskexecutor.AbsTaskExecutor;

/**
 * 按序任务执行器
 * qqliu
 * 2016/7/14.
 */
public class SequenceTaskExecutorImpl<T> extends AbsTaskExecutor<T> {

    @Override
    protected void executeTask(TaskInfo<T> task) {
        Utils.assertNotNull(mTaskExecutorHelper, "ITaskExecutorAbility is not set");

        if(null != task) {
            mTaskExecutorHelper.executeTask(task.getTask());
        }
    }
}
