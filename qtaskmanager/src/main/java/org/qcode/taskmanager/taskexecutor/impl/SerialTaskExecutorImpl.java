package org.qcode.taskmanager.taskexecutor.impl;

import org.qcode.taskmanager.base.utils.Utils;
import org.qcode.taskmanager.entities.TaskInfo;
import org.qcode.taskmanager.base.utils.Logging;
import org.qcode.taskmanager.taskexecutor.AbsTaskExecutor;

/**
 * 串行任务执行管理，严格保证一个任务执行完成后再执行下一个任务
 * qqliu
 * 2016/7/15.
 */
public class SerialTaskExecutorImpl<T> extends AbsTaskExecutor<T> {

    private static final String TAG = "SerialTaskExecutorImpl";
    //任务开始执行
    private static final int STATE_BEGIN = 0;
    //任务被锁定
    private static final int STATE_LOCKED = 1;
    //任务执行结束
    private static final int STATE_FINISH = 2;

    //当前的任务状态
    private volatile int mTaskState = STATE_BEGIN;

    private Object mLock = new Object();

    //当前执行的任务
    private T mCurrentRunningTask;

    @Override
    protected void executeTask(TaskInfo<T> task) {
        if (null == task) {
            return;
        }

        Utils.assertNotNull(mTaskExecutorHelper, "ITaskExecutorAbility is not set");

        mTaskState = STATE_BEGIN;

        //设置当前执行的任务
        mCurrentRunningTask = task.getTask();

        //执行任务
        mTaskExecutorHelper.executeTask(mCurrentRunningTask);

        //等待任务执行结束
        synchronized (mLock) {
            if(mTaskState == STATE_BEGIN) {
                try {
                    mTaskState = STATE_LOCKED;
                    mLock.wait();
                } catch (InterruptedException e) {
                    //todo
                }
            }
        }
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

        synchronized (mLock) {
            if(mTaskState == STATE_BEGIN) {
                //already finished, not need to wait lock
                //如果任务执行完成后，发现当前状态还是STATE_BEGIN，
                //则不需要在任务执行完成后等待了
                mTaskState = STATE_FINISH;
            } else if(mTaskState == STATE_LOCKED) {
                //正在等待任务执行结束，此时应解锁执行下一个任务
                mLock.notify();
            } else {
                //do nothing
            }
        }
    }
}
