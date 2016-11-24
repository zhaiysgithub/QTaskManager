package org.qcode.taskmanager.taskexecutor.impl;

import org.qcode.taskmanager.ISerialTaskExecutor;
import org.qcode.taskmanager.base.utils.Logging;
import org.qcode.taskmanager.base.utils.Utils;
import org.qcode.taskmanager.entities.TaskInfo;
import org.qcode.taskmanager.taskpool.ITaskNodeChangeListener;
import org.qcode.taskmanager.taskexecutor.AbsTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认任务执行器实现；
 * 支持任务的并行串行和定时执行；
 * <p>
 * qqliu
 * 2016/7/15.
 */
public class DefaultTaskExecutorImpl<T> extends AbsTaskExecutor<T> implements ISerialTaskExecutor<T> {

    private static final String TAG = "DefaultTaskExecutorImpl";

    //任务执行控制锁
    private ReentrantLock mLock;
    //任务执行控制条件
    private Condition mExecCondition;

    //存储已完成执行的任务，如果有任务在此任务之后执行且指定需此任务
    //结束才能运行，则从此列表内判断
    private List<T> mFinishedTask = new ArrayList<T>();

    //当前正在执行的任务
    private TaskInfo<T> mCurrentExecTask;

    public DefaultTaskExecutorImpl() {
        mLock = new ReentrantLock();
        mExecCondition = mLock.newCondition();
        //设置任务池变化的监听
        mTaskPool.setTaskNodeChangeListener(mTaskNodeChangeListener);
    }

    @Override
    protected void executeTask(TaskInfo<T> task) {
        if (null == task) {
            return;
        }

        Utils.assertNotNull(mTaskExecutorHelper, "ITaskExecutorAbility is not set");

        //开始执行锁定
        final ReentrantLock lock = this.mLock;
        lock.lock();

        mCurrentExecTask = task;

        T baseTask = task.getTask();

        //此任务需要等anchorTask执行完成后才能执行，
        //但是anchorTask尚未执行完成，
        //则等待条件满足才执行
        if (task.isWaitAnchorFinish()
                && !mFinishedTask.contains(task.getAnchorTask())) {
            try {
                mExecCondition.await();
            } catch (InterruptedException ex) {
                Logging.d(TAG, "executeTask()| error happened", ex);
                return;
            }
        }

        //此任务已经可以执行了，则执行之
        mTaskExecutorHelper.executeTask(baseTask);

        //解除执行锁定
        lock.unlock();
    }

    @Override
    public void notifyTaskBegin(T task) {
        //do nothing
    }

    @Override
    public void notifyTaskFinish(T task) {
        if (null == task) {
            return;
        }

        //先取到条件锁
        final ReentrantLock lock = this.mLock;
        lock.lock();

        //添加任务到已完成任务列表内
        mFinishedTask.add(task);

        //当前任务已经满足执行条件，则触发其执行
        if (mCurrentExecTask.isWaitAnchorFinish()
                && task.equals(mCurrentExecTask.getAnchorTask())) {
            mExecCondition.signalAll();
        }

        //解锁条件锁
        lock.unlock();
    }

    private ITaskNodeChangeListener<T> mTaskNodeChangeListener = new ITaskNodeChangeListener<T>() {
        @Override
        public void onMainNodeAdded(TaskInfo<T> task) {
            //do nothing
        }

        @Override
        public void onMainNodeRemoved(TaskInfo<T> task) {
            //从已完成任务列表内移除任务，
            //因为此任务及其依赖任务已经全部完成
            mFinishedTask.remove(task.getTask());
        }
    };
}
