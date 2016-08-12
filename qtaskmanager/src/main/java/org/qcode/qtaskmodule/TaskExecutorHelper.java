package org.qcode.qtaskmodule;

import org.qcode.qtaskmodule.entities.Task;
import org.qcode.qtaskmodule.taskexecutor.ITaskExecutor;
import org.qcode.qtaskmodule.taskexecutor.ITaskExecutorHelper;
import org.qcode.qtaskmodule.taskmanager.IRemoveListener;
import org.qcode.qtaskmodule.taskmanager.SequenceTaskManager;
import org.qcode.qtaskmodule.utils.Logging;

/**
 * qqliu
 * 2016/7/14.
 */
public abstract class TaskExecutorHelper<T> implements ITaskExecutorHelper<T> {

    private static final String TAG = "TaskExecutorHelper";

    protected ITaskExecutor<T> mTaskExecutor;

    protected SequenceTaskManager<T> mSequenceTaskManager;

    private boolean isRunning = false;

    public TaskExecutorHelper() {
        mSequenceTaskManager = new SequenceTaskManager<T>();
    }

    @Override
    public void setTaskExecutor(ITaskExecutor<T> executor) {
        mTaskExecutor = executor;
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

    protected Thread mWorkerThread = new Thread() {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    Task<T> task = mSequenceTaskManager.popTask();
                    executeTask(task);
                } catch (Exception ex) {
                    Logging.d(TAG, "exception happened", ex);
                }
            }
        }
    };

    protected abstract void executeTask(Task<T> task);

    protected static void assertNotNull(Object object) {
        if (null == object) {
            throw new RuntimeException("assertNotNull() object not setted");
        }
    }

    @Override
    public void addTask(T newTask) {
        mSequenceTaskManager.addTask(newTask);
    }

    @Override
    public void addTaskDelayed(T newTask, int delay) {
        mSequenceTaskManager.addTaskDelayed(newTask, delay);
    }

    @Override
    public void addTaskBeforeAnchor(T newTask, T anchorTask) {
        mSequenceTaskManager.addTaskBeforeAnchor(newTask, anchorTask);
    }

    @Override
    public void addTaskAfterAnchor(T newTask, T anchorTask) {
        mSequenceTaskManager.addTaskAfterAnchor(newTask, anchorTask);
    }

    @Override
    public void removeTask(T task) {
        mSequenceTaskManager.removeTask(task, mTaskRemoveListener);
    }

    private IRemoveListener<T> mTaskRemoveListener = new IRemoveListener<T>() {
        @Override
        public boolean needRemove(Task<T> removeTask, T objectData) {
            assertNotNull(mTaskExecutor);

            return mTaskExecutor.needRemove(removeTask.getBaseTask(), objectData);
        }
    };
}
