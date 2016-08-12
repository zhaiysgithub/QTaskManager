package org.qcode.qtaskmodule.taskmanager;

import org.qcode.qtaskmodule.entities.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 顺序任务执行的管理类
 * qqliu
 * 2016/7/14.
 */
public class SequenceTaskManager<T> implements ISequenceTaskManager<T> {

    private static final String TAG = "SequenceTaskManager";

    private LinkedList<Task<T>> mTaskCacheList = new LinkedList<Task<T>>();

    /**
     * Main lock guarding all access
     */
    private final ReentrantLock mLock;

    /**
     * Condition for waiting takes
     */
    private final Condition mCdnNotEmpty;

    private final Timer mTimer;

    private ITaskNodeChangeListener<T> mNodeChangeListener;

    public SequenceTaskManager() {
        this.mLock = new ReentrantLock();
        this.mCdnNotEmpty = mLock.newCondition();
        this.mTimer = new Timer();
    }

    @Override
    public void setTaskNodeChangeListener(ITaskNodeChangeListener<T> listener) {
        mNodeChangeListener = listener;
    }

    @Override
    public void addTask(T newTask) {
        final ReentrantLock lock = this.mLock;
        lock.lock();
        try {
            Task<T> wrappedTask = new Task<T>(newTask);
            mTaskCacheList.add(wrappedTask);
            mCdnNotEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTaskDelayed(T newTask, int delay) {
        final ReentrantLock lock = this.mLock;
        lock.lock();
        try {
            long triggerTime = System.currentTimeMillis() + delay;

            mTaskCacheList.add(new Task<T>(newTask, triggerTime));

            signalAfterDelay(delay);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTaskBeforeAnchor(T newTask, T anchorTask) {
        final ReentrantLock lock = this.mLock;
        lock.lock();
        try {
            for (Task<T> existTask : mTaskCacheList) {
                Task<T> anchorTaskWrapper = existTask.findTask(anchorTask);
                if (null != anchorTaskWrapper) {
                    anchorTaskWrapper.addToPrevious(newTask);
                    return;
                }
            }

            mTaskCacheList.add(new Task<T>(newTask));

            mCdnNotEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTaskAfterAnchor(T newTask, T anchorTask) {
        final ReentrantLock lock = this.mLock;
        lock.lock();
        try {
            for (Task<T> existTask : mTaskCacheList) {
                Task<T> anchorTaskWrapper = existTask.findTask(anchorTask);
                if (null != anchorTaskWrapper) {
                    anchorTaskWrapper.addToSubsequent(newTask);
                    return;
                }
            }

            mTaskCacheList.add(new Task<T>(newTask));

            mCdnNotEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTaskAfterAnchor(T newTask, T anchorTask, boolean waitAnchorFinish) {
        final ReentrantLock lock = this.mLock;
        lock.lock();
        try {
            for (Task<T> existTask : mTaskCacheList) {
                Task<T> anchorTaskWrapper = existTask.findTask(anchorTask);
                if (null != anchorTaskWrapper) {
                    anchorTaskWrapper.addToSubsequent(newTask, waitAnchorFinish);
                    return;
                }
            }

            mTaskCacheList.add(new Task<T>(newTask));

            mCdnNotEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeTask(T task, IRemoveListener<T> removeListener) {
        final ReentrantLock lock = this.mLock;
        lock.lock();
        try {
            ArrayList<Task<T>> removeList = new ArrayList<Task<T>>();
            //删除掉列表内的所有需要删除的数据
            Iterator<Task<T>> iterator = mTaskCacheList.iterator();
            while (iterator.hasNext()) {
                Task<T> existTask = iterator.next();
                if (removeListener.needRemove(existTask, task)) {
                    removeList.add(existTask);
                } else {
                    existTask.removeTask(task, removeListener);
                }
            }
            mTaskCacheList.removeAll(removeList);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Task<T> popTask() throws InterruptedException {
        final ReentrantLock lock = this.mLock;
        lock.lockInterruptibly();

        try {
            Task<T> task = null;
            while (null == task) {
                task = popTaskWithoutLock();

                if (null == task) {
                    mCdnNotEmpty.await();
                }
            }
            return task;
        } finally {
            lock.unlock();
        }

    }

    private Task<T> popTaskWithoutLock() {
        Task<T> candicate = null;
        candicate = popTimeScheduledTask();
        if (null != candicate) {
            return candicate;
        }

        ArrayList<Task<T>> removeList = new ArrayList<Task<T>>();
        Iterator<Task<T>> iterator = mTaskCacheList.iterator();
        while (iterator.hasNext()) {
            Task<T> existTask = iterator.next();
            candicate = existTask.popTask();
            if (null == candicate) {
                //如果是因为任务定时时间未到导致没有候选，则不移除任务，否则移除任务
                if (existTask.isTotallyConsumed()) {
                    removeList.add(existTask);
                }
                continue;
            }

            return candicate;
        }
        mTaskCacheList.removeAll(removeList);

        //通知节点移除操作
        for(Task<T> task : removeList) {
            if(null != mNodeChangeListener) {
                mNodeChangeListener.onMainNodeRemoved(task);
            }
        }

        return null;
    }

    //此方法不影响数据个数
    private Task<T> popTimeScheduledTask() {

        long currTime = System.currentTimeMillis();

        for (int i = 0; i < mTaskCacheList.size(); i++) {
            Task<T> existTask = mTaskCacheList.get(i);
            long executeTime = existTask.getExecuteTime();
            if (!existTask.isSelfConsumed()
                    && executeTime > 0
                    && existTask.getExecuteTime() <= currTime) {
                //mTaskCacheList是无序的任务队列，当前任务已经开始触发，所以挪到任务队列前方
                mTaskCacheList.remove(i);
                mTaskCacheList.add(0, existTask);
                existTask.popOutSelf();
                return existTask;
            }
        }

        return null;
    }


    private void signalAfterDelay(int delay) {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final ReentrantLock lock = SequenceTaskManager.this.mLock;
                lock.lock();
                mCdnNotEmpty.signal();
                lock.unlock();
            }
        }, delay);
    }
}