package org.qcode.taskmanager.model;

/**
 * qqliu
 * 2016/7/15.
 */
public class ExecuteTask {
    public int id = 0;

    public ExecuteTask(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecuteTask that = (ExecuteTask) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "ExecuteTask{" +
                "id=" + id +
                '}';
    }
}
