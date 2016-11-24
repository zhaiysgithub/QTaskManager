package org.qcode.taskmanager.entities;


import org.qcode.taskmanager.ITaskComparator;
import org.qcode.taskmanager.base.utils.Logging;
import org.qcode.taskmanager.base.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 队列内的一个任务的封装
 * qqliu
 * 2016/7/14.
 */
public class TaskInfo<T> {

    private static final String TAG = "TaskInfo";

    private long mExecuteTime;
    private T mCurrTask;
    private T mAnchorTask;
    private CopyOnWriteArrayList<TaskInfo<T>> mPreviousTaskList;
    private CopyOnWriteArrayList<TaskInfo<T>> mSubsequentTaskList;

    private boolean waitAnchorFinish = false;

    private boolean isCurrentTaskPopOut = false;

    public TaskInfo(T task) {
        this.mCurrTask = task;
    }

    public TaskInfo(T task, long executeTime) {
        this.mCurrTask = task;
        this.mExecuteTime = executeTime;
    }

    public TaskInfo(T task, T anchorTask, boolean waitAnchorFinish) {
        this.mCurrTask = task;
        this.mAnchorTask = anchorTask;
        this.waitAnchorFinish = waitAnchorFinish;
    }

    public T getTask() {
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
            mPreviousTaskList = new CopyOnWriteArrayList<TaskInfo<T>>();
        }
        mPreviousTaskList.add(new TaskInfo<T>(task));
    }

    public void addToSubsequent(T task) {
        if (null == task) {
            throw new RuntimeException("addToSubsequent() | task is null");
        }

        if (null == mSubsequentTaskList) {
            mSubsequentTaskList = new CopyOnWriteArrayList<TaskInfo<T>>();
        }
        mSubsequentTaskList.add(new TaskInfo<T>(task));
    }

    public void addToSubsequent(T task, boolean waitAnchorFinish) {
        if (null == task) {
            throw new RuntimeException("addToSubsequent() | task is null");
        }

        if (null == mSubsequentTaskList) {
            mSubsequentTaskList = new CopyOnWriteArrayList<TaskInfo<T>>();
        }
        //添加到第一个元素，保证主元素执行后，就执行此元素
        mSubsequentTaskList.add(0, new TaskInfo<T>(task, mCurrTask, waitAnchorFinish));
    }

    public TaskInfo<T> findTask(T task, ITaskComparator<T> taskComparator) {
        if (null == task || null == taskComparator) {
            return null;
        }

        if (taskComparator.isSame(mCurrTask, task)) {
            return this;
        }

        if (null != mPreviousTaskList) {
            for (TaskInfo<T> existTask : mPreviousTaskList) {
                TaskInfo<T> candicate = existTask.findTask(task, taskComparator);
                if (null != candicate) {
                    return candicate;
                }
            }
        }

        if (null != mSubsequentTaskList) {
            for (TaskInfo<T> existTask : mSubsequentTaskList) {
                TaskInfo<T> candicate = existTask.findTask(task, taskComparator);
                if (null != candicate) {
                    return candicate;
                }
            }
        }

        return null;
    }

    public boolean containTask(T task, ITaskComparator<T> taskComparator) {
        if (null == task) {
            return false;
        }

        TaskInfo<T> destTask = findTask(task, taskComparator);

        return null != destTask;
    }

    public void removeTask(T task, ITaskComparator<T> taskComparator) {
        if (null == task) {
            throw new RuntimeException("removeTask() | task is null");
        }

        removeTaskFromList(mPreviousTaskList, task, taskComparator);
        removeTaskFromList(mSubsequentTaskList, task, taskComparator);
    }

    //try delete task in list recursively
    private static <T> void removeTaskFromList(List<TaskInfo<T>> list, T task, ITaskComparator<T> taskComparator) {
        if (null == task) {
            throw new RuntimeException("removeTask() | task is null");
        }

        if (null != list) {
            ArrayList<TaskInfo<T>> removeList = new ArrayList<TaskInfo<T>>();
            Iterator<TaskInfo<T>> iterator = list.iterator();
            while (iterator.hasNext()) {
                TaskInfo<T> itemTask = iterator.next();
                if (taskComparator.isSame(itemTask.getTask(), task)) {
                    removeList.add(itemTask);
                } else {
                    removeTaskFromList(itemTask.mPreviousTaskList, task, taskComparator);
                    removeTaskFromList(itemTask.mSubsequentTaskList, task, taskComparator);
                }
            }
            list.removeAll(removeList);
        }
    }

    public TaskInfo<T> popTask() {
        //尚未执行到当前任务
        if (!isCurrentTaskPopOut) {
            ArrayList<TaskInfo<T>> removeList = new ArrayList<TaskInfo<T>>();
            if (!Utils.isEmpty(mPreviousTaskList)) {
                Iterator<TaskInfo<T>> iterator = mPreviousTaskList.iterator();
                while (iterator.hasNext()) {
                    TaskInfo<T> currTask = iterator.next();
                    //尝试第0个元素是否还能取出数据
                    TaskInfo<T> candicateTask = currTask.popTask();
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
                ArrayList<TaskInfo<T>> removeList = new ArrayList<TaskInfo<T>>();

                Iterator<TaskInfo<T>> iterator = mSubsequentTaskList.iterator();
                while (iterator.hasNext()) {
                    TaskInfo<T> currTask = iterator.next();
                    //尝试第0个元素是否还能取出数据
                    TaskInfo<T> candicateTask = currTask.popTask();
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

        TaskInfo<?> task = (TaskInfo<?>) o;

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
