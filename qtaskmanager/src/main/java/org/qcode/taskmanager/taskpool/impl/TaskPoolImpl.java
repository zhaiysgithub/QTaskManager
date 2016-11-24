package org.qcode.taskmanager.taskpool.impl;

import org.qcode.taskmanager.ITaskComparator;
import org.qcode.taskmanager.base.utils.Utils;
import org.qcode.taskmanager.entities.DuplicateTaskStrategy;
import org.qcode.taskmanager.entities.TaskInfo;
import org.qcode.taskmanager.taskpool.ITaskNodeChangeListener;
import org.qcode.taskmanager.taskpool.ITaskPool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务池的实现
 * qqliu
 * 2016/7/14.
 */
public class TaskPoolImpl<T> implements ITaskPool<T> {

    private static final String TAG = "TaskPoolImpl";

    //任务列表
    private LinkedList<TaskInfo<T>> mTaskList = new LinkedList<TaskInfo<T>>();

    //添加/删除动作的锁
    private final ReentrantLock mLock;

    //池内数据不为空时的通知
    private final Condition mCdnNotEmpty;

    //定时器
    private final Timer mTimer;

    //任务池变化监听
    private ITaskNodeChangeListener<T> mNodeChangeListener;

    //任务比较器
    private ITaskComparator<T> mTaskComparator = new DefaultTaskComparator<T>();

    //设置重复任务的处理策略
    protected DuplicateTaskStrategy mDuplicateTaskStrategy
            = DuplicateTaskStrategy.KEEP_ALL;

    public TaskPoolImpl() {
        this.mLock = new ReentrantLock();
        this.mCdnNotEmpty = mLock.newCondition();
        this.mTimer = new Timer();
    }

    @Override
    public void setTaskNodeChangeListener(ITaskNodeChangeListener<T> listener) {
        mNodeChangeListener = listener;
    }

    @Override
    public void setTaskComparator(ITaskComparator<T> comparator) {
        if(null == comparator) {
            mTaskComparator = new DefaultTaskComparator<T>();
        } else {
            mTaskComparator = comparator;
        }
    }

    @Override
    public void setDuplicateTaskStrategy(DuplicateTaskStrategy strategy) {
        mDuplicateTaskStrategy = strategy;
    }

    @Override
    public boolean addTask(T newTask) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        //对之前的任务进行处理，根据策略判断是否可以添加任务
        boolean canAddTask = handlePreviousTaskByStrategy(newTask);
        if(!canAddTask) {
            lock.unlock();
            return false;
        }

        //添加任务
        try {
            TaskInfo<T> wrappedTask = new TaskInfo<T>(newTask);
            mTaskList.add(wrappedTask);
            mCdnNotEmpty.signal();

            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addTaskDelayed(T newTask, int delay) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        //对之前的任务进行处理，根据策略判断是否可以添加任务
        boolean canAddTask = handlePreviousTaskByStrategy(newTask);
        if(!canAddTask) {
            lock.unlock();
            return false;
        }

        try {
            long triggerTime = System.currentTimeMillis() + delay;

            mTaskList.add(new TaskInfo<T>(newTask, triggerTime));

            signalAfterDelay(delay);

            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addTaskBeforeAnchor(T newTask, T anchorTask) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        //对之前的任务进行处理，根据策略判断是否可以添加任务
        boolean canAddTask = handlePreviousTaskByStrategy(newTask);
        if(!canAddTask) {
            lock.unlock();
            return false;
        }

        try {
            for (TaskInfo<T> existTask : mTaskList) {
                TaskInfo<T> anchorTaskWrapper = existTask.findTask(anchorTask, mTaskComparator);
                if (null != anchorTaskWrapper) {
                    anchorTaskWrapper.addToPrevious(newTask);
                    return true;
                }
            }

            mTaskList.add(new TaskInfo<T>(newTask));

            mCdnNotEmpty.signal();

            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addTaskAfterAnchor(T newTask, T anchorTask) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        //对之前的任务进行处理，根据策略判断是否可以添加任务
        boolean canAddTask = handlePreviousTaskByStrategy(newTask);
        if(!canAddTask) {
            lock.unlock();
            return false;
        }

        try {
            for (TaskInfo<T> existTask : mTaskList) {
                TaskInfo<T> anchorTaskWrapper = existTask.findTask(anchorTask, mTaskComparator);
                if (null != anchorTaskWrapper) {
                    anchorTaskWrapper.addToSubsequent(newTask);
                    return true;
                }
            }

            mTaskList.add(new TaskInfo<T>(newTask));

            mCdnNotEmpty.signal();

            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addTaskAfterAnchor(T newTask, T anchorTask, boolean waitAnchorFinish) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        //对之前的任务进行处理，根据策略判断是否可以添加任务
        boolean canAddTask = handlePreviousTaskByStrategy(newTask);
        if(!canAddTask) {
            lock.unlock();
            return false;
        }

        try {
            for (TaskInfo<T> existTask : mTaskList) {
                TaskInfo<T> anchorTaskWrapper = existTask.findTask(anchorTask, mTaskComparator);
                if (null != anchorTaskWrapper) {
                    anchorTaskWrapper.addToSubsequent(newTask, waitAnchorFinish);
                    return true;
                }
            }

            mTaskList.add(new TaskInfo<T>(newTask));

            mCdnNotEmpty.signal();

            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeTask(T task) {
        final ReentrantLock lock = this.mLock;
        lock.lock();

        try {
            ArrayList<TaskInfo<T>> removeList = new ArrayList<TaskInfo<T>>();
            //删除掉列表内的所有需要删除的数据
            Iterator<TaskInfo<T>> iterator = mTaskList.iterator();
            while (iterator.hasNext()) {
                TaskInfo<T> existTask = iterator.next();
                if (mTaskComparator.isSame(
                        existTask.getTask(), task)) {
                    removeList.add(existTask);
                } else {
                    existTask.removeTask(task, mTaskComparator);
                }
            }
            mTaskList.removeAll(removeList);

            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TaskInfo<T> popTask() throws InterruptedException {
        final ReentrantLock lock = this.mLock;
        lock.lockInterruptibly();

        try {
            TaskInfo<T> task = null;
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

    private TaskInfo<T> popTaskWithoutLock() {
        TaskInfo<T> candicate = null;
        candicate = popTimeScheduledTask();
        if (null != candicate) {
            return candicate;
        }

        ArrayList<TaskInfo<T>> removeList = new ArrayList<TaskInfo<T>>();
        Iterator<TaskInfo<T>> iterator = mTaskList.iterator();
        while (iterator.hasNext()) {
            TaskInfo<T> existTask = iterator.next();
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
        mTaskList.removeAll(removeList);

        //通知节点移除操作
        for(TaskInfo<T> task : removeList) {
            if(null != mNodeChangeListener) {
                mNodeChangeListener.onMainNodeRemoved(task);
            }
        }

        return null;
    }

    //此方法不影响数据个数
    private TaskInfo<T> popTimeScheduledTask() {

        long currTime = System.currentTimeMillis();

        for (int i = 0; i < mTaskList.size(); i++) {
            TaskInfo<T> existTask = mTaskList.get(i);
            long executeTime = existTask.getExecuteTime();
            if (!existTask.isSelfConsumed()
                    && executeTime > 0
                    && existTask.getExecuteTime() <= currTime) {
                //mTaskCacheList是无序的任务队列，当前任务已经开始触发，所以挪到任务队列前方
                mTaskList.remove(i);
                mTaskList.add(0, existTask);
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
                final ReentrantLock lock = TaskPoolImpl.this.mLock;
                lock.lock();
                mCdnNotEmpty.signal();
                lock.unlock();
            }
        }, delay);
    }

    private boolean isExistTask(T newTask) {
        Utils.assertNotNull(mTaskComparator, "ITaskComparator is not set");
        for(TaskInfo<T> taskInfo: mTaskList) {
            if(mTaskComparator.isSame(taskInfo.getTask(), newTask)) {
                return true;
            }

            if(taskInfo.containTask(newTask, mTaskComparator)) {
                return true;
            }
        }
        return false;
    }

    private boolean handlePreviousTaskByStrategy(T newTask) {
        switch (mDuplicateTaskStrategy) {
            case KEEP_PREVIOUS:
                if(isExistTask(newTask)) {
                    //之前已经存在任务，则保持之前的任务
                    return false;
                }

                //之前没有任务，则添加
                break;

            case KEEP_CURRENT:
                if(isExistTask(newTask)) {
                    //之前已经存在任务，则删除之前任务
                    removeTask(newTask);
                }
                //添加任务
                break;

            case KEEP_ALL:
                break;
        }

        return true;
    }

}