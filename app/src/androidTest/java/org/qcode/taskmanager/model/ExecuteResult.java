package org.qcode.taskmanager.model;

/**
 * qqliu
 * 2016/7/17.
 */
public class ExecuteResult {
    public static final int ID_NOT_EXIST = Integer.MIN_VALUE;
    public static final int EXECUTE_FIRST = 0;
    public static final int EXECUTE_BEFORE = 1;
    public static final int EXECUTE_END = 2;

    public int id;
    public int anchorId;
    public int relation;

    public int delay;
    public long executeTime;

    public ExecuteResult(int id, int relation, int anchorId) {
        this.id = id;
        this.anchorId = anchorId;
        this.relation = relation;
    }

    public ExecuteResult(int id) {
        this.id = id;
        this.anchorId = -1;
        this.relation = EXECUTE_FIRST;
    }

    public ExecuteResult(int id, int delay, long executeTime) {
        this.id = id;
        this.delay = delay;
        this.executeTime = executeTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecuteResult that = (ExecuteResult) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
