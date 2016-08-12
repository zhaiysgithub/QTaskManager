package org.qcode.qtaskmodule;

import org.qcode.qtaskmodule.entities.Task;
import org.qcode.qtaskmodule.taskexecutor.ISerialTaskExecutorHelper;
import org.qcode.qtaskmodule.utils.Logging;

/**
 * 串行任务执行管理，严格结束后执行
 * qqliu
 * 2016/7/15.
 */
public class SerialTaskExecutorHelper<T> extends TaskExecutorHelper<T> implements ISerialTaskExecutorHelper<T> {

    private static final String TAG = "SerialTaskExecutorHelper";

    private static final int STATE_BEGIN = 0;
    private static final int STATE_LOCKED = 1;
    private static final int STATE_FINISH = 2;

    private volatile int mTaskState = STATE_BEGIN;

    private Object mLock = new Object();

    private T mCurrentRunningTask;

    @Override
    protected void executeTask(Task<T> task) {
        if (null == task) {
            return;
        }
        mCurrentRunningTask = task.getBaseTask();

        mTaskState = STATE_BEGIN;

        mTaskExecutor.executeTask(mCurrentRunningTask);

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
                mTaskState = STATE_FINISH;
            } else if(mTaskState == STATE_LOCKED) {
                mLock.notify();
            } else {
                //do nothing
            }
        }
    }
}
