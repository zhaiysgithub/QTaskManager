package org.qcode.qtaskmodule.entities;

import org.qcode.qtaskmodule.taskmanager.IRemoveListener;
import org.qcode.qtaskmodule.utils.Logging;
import org.qcode.qtaskmodule.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 队列内的一个任务的封装
 * qqliu
 * 2016/7/14.
 */
public class Task<T> {

    private static final String TAG = "Task";

    private long mExecuteTime;
    private T mCurrTask;
    private T mAnchorTask;
    private CopyOnWriteArrayList<Task<T>> mPreviousTaskList;
    private CopyOnWriteArrayList<Task<T>> mSubsequentTaskList;

    private boolean waitAnchorFinish = false;

    private boolean isCurrentTaskPopOut = false;

    public Task(T task) {
        this.mCurrTask = task;
    }

    public Task(T task, long executeTime) {
        this.mCurrTask = task;
        this.mExecuteTime = executeTime;
    }

    public Task(T task, T anchorTask, boolean waitAnchorFinish) {
        this.mCurrTask = task;
        this.mAnchorTask = anchorTask;
        this.waitAnchorFinish = waitAnchorFinish;
    }

    public T getBaseTask() {
        return mCurrTask;
    }

    public long getExecuteTime() {
        return mExecuteTime;
    }

    public T getAnchorTask() {
        return mAnchorTask;
    }

    public void addToPrevious(T task) {
        if (null == task) {
            throw new RuntimeException("addToPrevious() | task is null");
        }

        if (mExecuteTime > 0) {
            throw new RuntimeException("addToPrevious() | current task is scheduled by time, CANNOT recept previous task");
        }

        if (isCurrentTaskPopOut) {
            Logging.d(TAG, "", "addToPrevious()| current task has executed, cannot add to previous");
            return;
        }

        if (null == mPreviousTaskList) {
            mPreviousTaskList = new CopyOnWriteArrayList<Task<T>>();
        }
        mPreviousTaskList.add(new Task<T>(task));
    }

    public void addToSubsequent(T task) {
        if (null == task) {
            throw new RuntimeException("addToSubsequent() | task is null");
        }

        if (null == mSubsequentTaskList) {
            mSubsequentTaskList = new CopyOnWriteArrayList<Task<T>>();
        }
        mSubsequentTaskList.add(new Task<T>(task));
    }

    public void addToSubsequent(T task, boolean waitAnchorFinish) {
        if (null == task) {
            throw new RuntimeException("addToSubsequent() | task is null");
        }

        if (null == mSubsequentTaskList) {
            mSubsequentTaskList = new CopyOnWriteArrayList<Task<T>>();
        }
        //添加到第一个元素，保证主元素执行后，就执行此元素
        mSubsequentTaskList.add(0, new Task<T>(task, mCurrTask, waitAnchorFinish));
    }

    public Task<T> findTask(T task) {
        if (null == task) {
            return null;
        }

        Task<T> objTask = new Task<T>(task);

        if (equals(objTask)) {
            return this;
        }

        if (null != mPreviousTaskList) {
            for (Task<T> existTask : mPreviousTaskList) {
                Task<T> candicate = existTask.findTask(task);
                if (null != candicate) {
                    return candicate;
                }
            }
        }

        if (null != mSubsequentTaskList) {
            for (Task<T> existTask : mSubsequentTaskList) {
                Task<T> candicate = existTask.findTask(task);
                if (null != candicate) {
                    return candicate;
                }
            }
        }

        return null;
    }

    public boolean containTask(T task) {
        if (null == task) {
            return false;
        }

        Task<T> candicate = findTask(task);

        return null != candicate;
    }

    public void removeTask(T task, IRemoveListener<T> removeListener) {
        if (null == task) {
            throw new RuntimeException("removeTask() | task is null");
        }

        removeTaskFromList(mPreviousTaskList, task, removeListener);
        removeTaskFromList(mSubsequentTaskList, task, removeListener);
    }

    //try delete task in list recursively
    private static <T> void removeTaskFromList(List<Task<T>> list, T task, IRemoveListener<T> removeListener) {
        if (null == task) {
            throw new RuntimeException("removeTask() | task is null");
        }

        if (null != list) {
            ArrayList<Task<T>> removeList = new ArrayList<Task<T>>();
            Iterator<Task<T>> iterator = list.iterator();
            while (iterator.hasNext()) {
                Task<T> itemTask = iterator.next();
                if (removeListener.needRemove(itemTask, task)) {
                    removeList.add(itemTask);
                } else {
                    removeTaskFromList(itemTask.mPreviousTaskList, task, removeListener);
                    removeTaskFromList(itemTask.mSubsequentTaskList, task, removeListener);
                }
            }
            list.removeAll(removeList);
        }
    }

    public Task<T> popTask() {
        //尚未执行到当前任务
        if (!isCurrentTaskPopOut) {
            ArrayList<Task<T>> removeList = new ArrayList<Task<T>>();
            if (!Utils.isEmpty(mPreviousTaskList)) {
                Iterator<Task<T>> iterator = mPreviousTaskList.iterator();
                while (iterator.hasNext()) {
                    Task<T> currTask = iterator.next();
                    //尝试第0个元素是否还能取出数据
                    Task<T> candicateTask = currTask.popTask();
                    if (null != candicateTask) {
                        //能取出则返回
                        return candicateTask;
                    } else {
                        removeList.add(currTask);
                    }
                }
                mPreviousTaskList.removeAll(removeList);
            }

            if(mExecuteTime > System.currentTimeMillis()) {
                //定时消息不能提前执行
                return null;
            }

            isCurrentTaskPopOut = true;
            return this;
        } else {
            if (!Utils.isEmpty(mSubsequentTaskList)) {
                ArrayList<Task<T>> removeList = new ArrayList<Task<T>>();

                Iterator<Task<T>> iterator = mSubsequentTaskList.iterator();
                while (iterator.hasNext()) {
                    Task<T> currTask = iterator.next();
                    //尝试第0个元素是否还能取出数据
                    Task<T> candicateTask = currTask.popTask();
                    if (null != candicateTask) {
                        //能取出则返回
                        return candicateTask;
                    } else {
                        removeList.add(currTask);
                    }
                }
                mSubsequentTaskList.removeAll(removeList);
            }

            return null;
        }
    }

    public void popOutSelf() {
        isCurrentTaskPopOut = true;
    }

    public boolean isTotallyConsumed() {
        return isCurrentTaskPopOut && Utils.isEmpty(mSubsequentTaskList);
    }

    public boolean isSelfConsumed() {
        return isCurrentTaskPopOut;
    }

    public boolean isWaitAnchorFinish() {
        return waitAnchorFinish;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task<?> task = (Task<?>) o;

        return mCurrTask != null ? mCurrTask.equals(task.mCurrTask) : task.mCurrTask == null;
    }

    @Override
    public int hashCode() {
        return mCurrTask != null ? mCurrTask.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Task{" +
                "mExecuteTime=" + mExecuteTime +
                ", mCurrTask=" + mCurrTask +
                ", mAnchorTask=" + mAnchorTask +
                ", mPreviousTaskList=" + mPreviousTaskList +
                ", mSubsequentTaskList=" + mSubsequentTaskList +
                ", waitAnchorFinish=" + waitAnchorFinish +
                ", isCurrentTaskPopOut=" + isCurrentTaskPopOut +
                '}';
    }
}
