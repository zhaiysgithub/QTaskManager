package org.qcode.taskmanager;

/**
 * 任务比较器
 * qqliu
 * 2016/11/16.
 */

public interface ITaskComparator<T> {

    /***
     * 判断两个任务是否相同
     * @param taskLeft
     * @param taskRight
     * @return
     */
    boolean isSame(T taskLeft, T taskRight);
}
