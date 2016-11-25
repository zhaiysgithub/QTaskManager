package org.qcode.taskmanager.taskexecutor;

import org.qcode.taskmanager.ITaskExecutor;
import org.qcode.taskmanager.ITaskExecutorAbility;
import org.qcode.taskmanager.ITaskManager;
import org.qcode.taskmanager.base.utils.Logging;
import org.qcode.taskmanager.base.utils.UITaskRunner;
import org.qcode.taskmanager.entities.DuplicateTaskStrategy;
import org.qcode.taskmanager.entities.TaskInfo;
import org.qcode.taskmanager.taskpool.ITaskPool;
import org.qcode.taskmanager.taskpool.impl.TaskPoolImpl;

/**
 * 抽象的任务执行器；
 * 封装公共的操作逻辑；
 *
 * qqliu
 * 2016/7/14.
 */
public abstract class AbsTaskExecutor<T> implements ITaskExecutor<T> {

    private static final String TAG = "TaskExecutorHelper";

    //任务执行器的辅助能力
    protected ITaskExecutorAbility<T> mTaskExecutorHelper;

    //待执行任务池
    protected ITaskPool<T> mTaskPool;

    //任务执行器是否在工作状态
    private boolean isRunning = false;

    //设置重复任务的处理策略
    protected DuplicateTaskStrategy mDuplicateTaskStrategy
            = DuplicateTaskStrategy.KEEP_ALL;

    //表示任务是否运行在UI线程
    private boolean mRunOnUIThread = false;

    //切换到UI线程执行任务时的锁
    private Object mSwitchUIThreadRunningLock = new Object();

    public AbsTaskExecutor() {
        mTaskPool = new TaskPoolImpl<T>();
    }

    @Override
    public void setDuplicateTaskStrategy(DuplicateTaskStrategy strategy) {
        mDuplicateTaskStrategy = strategy;
        mTaskPool.setDuplicateTaskStrategy(mDuplicateTaskStrategy);
    }

    @Override
    public void setTaskExecutorAbility(ITaskExecutorAbility<T> executor) {
        mTaskExecutorHelper = executor;

        //设置TaskComparator
        mTaskPool.setTaskComparator(mTaskExecutorHelper);
    }

    @Override
    public void setRunOnUIThread(boolean runOnUIThread) {
        mRunOnUIThread = runOnUIThread;
    }

    @Override
    public void startExecute() {
        isRunning = true;
        mWorkerThread.start();
    }

    @Override
    public void stopExecute() {
        isRunning = false;
    }

    @Override
    public ITaskManager<T> getTaskManager() {
        return mTaskPool;
    }

    protected Thread mWorkerThread = new Thread() {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    final TaskInfo<T> task = mTaskPool.popTask();
                    if(mRunOnUIThread) {
                        //切换到UI线程执行任务
                        UITaskRunner.getHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                executeTask(task);

                                //UI线程任务执行触发后开始唤醒下一个任务执行
                                synchronized (mSwitchUIThreadRunningLock) {
                                    mSwitchUIThreadRunningLock.notifyAll();
                                }
                            }
                        });

                        //等待UI线程任务执行触发后在唤醒进行下一个任务执行
                        synchronized (mSwitchUIThreadRunningLock) {
                            mSwitchUIThreadRunningLock.wait();
                        }

                    } else {
                        //在当前线程执行任务
                        executeTask(task);
                    }
                } catch (Exception ex) {
                    Logging.d(TAG, "exception happened", ex);
                }
            }
        }
    };

    /***
     * 执行任务
     * @param task
     */
    protected abstract void executeTask(TaskInfo<T> task);
}
