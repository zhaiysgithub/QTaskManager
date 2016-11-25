package org.qcode.taskmanager.taskexecutor.impl;

import org.qcode.taskmanager.base.utils.LockWaitNotifyHelper;
import org.qcode.taskmanager.base.utils.Logging;
import org.qcode.taskmanager.base.utils.Utils;
import org.qcode.taskmanager.entities.TaskInfo;
import org.qcode.taskmanager.taskexecutor.AbsTaskExecutor;

/**
 * 串行任务执行管理，严格保证一个任务执行完成后再执行下一个任务
 * qqliu
 * 2016/7/15.
 */
public class SerialTaskExecutorImpl<T> extends AbsTaskExecutor<T> {

    private static final String TAG = "SerialTaskExecutorImpl";

    private LockWaitNotifyHelper mLockWaitNotifyHelper;

    //当前执行的任务
    private T mCurrentRunningTask;

    public SerialTaskExecutorImpl() {
        mLockWaitNotifyHelper = new LockWaitNotifyHelper(new Object());
    }

    @Override
    protected void executeTask(TaskInfo<T> task) {
        if (null == task) {
            return;
        }

        Utils.assertNotNull(mTaskExecutorHelper, "ITaskExecutorAbility is not set");

        mLockWaitNotifyHelper.beginLockAction();

        //设置当前执行的任务
        mCurrentRunningTask = task.getTask();

        //执行任务
        mTaskExecutorHelper.executeTask(mCurrentRunningTask);

        mLockWaitNotifyHelper.waitForSignal();
    }

    @Override
    public void notifyTaskBegin(T task) {
        //do nothing
    }

    @Override
    public void notifyTaskFinish(T task) {
        if(!mCurrentRunningTask.equals(task)){
            Logging.d(TAG, "notifyTaskFinish()| wait for running task back, running is: "
                    + mCurrentRunningTask + " while notified task is: "  + task);
            return;
        }

        mLockWaitNotifyHelper.signalWaiter();
    }
}
