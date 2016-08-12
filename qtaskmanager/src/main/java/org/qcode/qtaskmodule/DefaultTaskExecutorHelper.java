package org.qcode.qtaskmodule;

import org.qcode.qtaskmodule.entities.Task;
import org.qcode.qtaskmodule.taskexecutor.IDefaultTaskExecutorHelper;
import org.qcode.qtaskmodule.taskmanager.ITaskNodeChangeListener;
import org.qcode.qtaskmodule.utils.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 串行任务执行管理，严格结束后执行
 * qqliu
 * 2016/7/15.
 */
public class DefaultTaskExecutorHelper<T> extends TaskExecutorHelper<T> implements IDefaultTaskExecutorHelper<T> {

    private static final String TAG = "DefaultTaskExecutorHelper";

    private ReentrantLock mLock;

    private Condition mExecCondition;

    private List<T> mFinishedTask = new ArrayList<T>();

    private Task<T> mCurrentExecTask;

    public DefaultTaskExecutorHelper() {
        mLock = new ReentrantLock();
        mExecCondition = mLock.newCondition();
        mSequenceTaskManager.setTaskNodeChangeListener(mTaskNodeChangeListener);
    }

    @Override
    protected void executeTask(Task<T> task) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        if (null == task) {
            return;
        }

        mCurrentExecTask = task;

        T baseTask = task.getBaseTask();

        //等待上一个任务执行结束
        if(task.isWaitAnchorFinish()
                && !mFinishedTask.contains(task.getAnchorTask())) {
            try {
                mExecCondition.await();
            } catch (InterruptedException ex) {
                Logging.d(TAG, "executeTask()| error happened", ex);
                return;
            }
        }

        mTaskExecutor.executeTask(baseTask);

        lock.unlock();
    }

    @Override
    public void notifyTaskBegin(T task) {
        //do nothing
    }

    @Override
    public void notifyTaskFinish(T task) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        if(null == task) {
            return;
        }

        mFinishedTask.add(task);

        if(mCurrentExecTask.isWaitAnchorFinish()
                && task.equals(mCurrentExecTask.getAnchorTask())) {
            mExecCondition.signalAll();
        }

        lock.unlock();
    }

    @Override
    public void addTaskAfterAnchor(T newTask, T anchorTask, boolean waitAnchorFinish) {
        mSequenceTaskManager.addTaskAfterAnchor(newTask, anchorTask, waitAnchorFinish);
    }

    private ITaskNodeChangeListener<T> mTaskNodeChangeListener = new ITaskNodeChangeListener<T>() {
        @Override
        public void onMainNodeAdded(Task<T> task) {
            //do nothing
        }

        @Override
        public void onMainNodeRemoved(Task<T> task) {
            //remove finished task
            mFinishedTask.remove(task.getBaseTask());
        }
    };
}
