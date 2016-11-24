package org.qcode.taskmanager;

import android.test.AndroidTestCase;

import org.qcode.taskmanager.base.utils.Logging;
import org.qcode.taskmanager.model.ExecuteResult;
import org.qcode.taskmanager.model.ExecuteTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * qqliu
 * 2016/7/15.
 */
public class TimeScheduledTaskExecutorTest extends AndroidTestCase {

    private static final String TAG = "TimeScheduledTaskExecutorTest";

    private static final int TEST_EXECUTE_TIME = 1000;

    private List<ExecuteResult> idSequenceList = new ArrayList<ExecuteResult>();

    private List<ExecuteResult> executeResultList = new ArrayList<ExecuteResult>();

    private int resultIndex = -1;

    private Object mLock = new Object();

    public void testSimpleTimeScheduledTaskExecutor() throws InterruptedException {
        idSequenceList.clear();
        resultIndex = -1;

        ITaskExecutor<ExecuteTask> executorHelper = TaskExecutorFactory.createSequenceTaskExecutor();

        executorHelper.setTaskExecutorAbility(taskExecutorTimeScheduled);

        executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(1), 200);
        idSequenceList.add(new ExecuteResult(1, 200, System.currentTimeMillis()));

        executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(2), 400);
        idSequenceList.add(new ExecuteResult(2, 400, System.currentTimeMillis()));

        executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(3), 600);
        idSequenceList.add(new ExecuteResult(3, 600, System.currentTimeMillis()));

        executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(4), 900);
        idSequenceList.add(new ExecuteResult(4, 900, System.currentTimeMillis()));

        executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(5), 1100);
        idSequenceList.add(new ExecuteResult(5, 1100, System.currentTimeMillis()));

        executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(8), 3000);
        idSequenceList.add(new ExecuteResult(8, 3000, System.currentTimeMillis()));

        executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(9), 400);
        idSequenceList.add(new ExecuteResult(9, 400, System.currentTimeMillis()));

        executorHelper.startExecute();

        synchronized (mLock) {
            mLock.wait();
        }
    }

    public void testRandomTimeScheduledTaskExecutor() throws InterruptedException {
        idSequenceList.clear();
        resultIndex = -1;

        ITaskExecutor<ExecuteTask> executorHelper = TaskExecutorFactory.createSequenceTaskExecutor();

        executorHelper.setTaskExecutorAbility(taskExecutorTimeScheduled);

        //必须保证id不相等，否则后面的自动判断逻辑会出错
        Random random = new Random();
        for (int i = 0; i < TEST_EXECUTE_TIME; i++) {
            int id, delay;
            //确保id不重复
            do {
                id = random.nextInt(TEST_EXECUTE_TIME * 10);
                delay = random.nextInt(10000);
            } while (idSequenceList.contains(new ExecuteResult(id)));

            executorHelper.getTaskManager().addTaskDelayed(new ExecuteTask(id), delay);
            idSequenceList.add(new ExecuteResult(id, delay, System.currentTimeMillis()));
        }

        executorHelper.startExecute();

        synchronized (mLock) {
            mLock.wait();
        }
    }

    ITaskExecutorAbility<ExecuteTask> taskExecutorTimeScheduled = new ITaskExecutorAbility<ExecuteTask>() {
        @Override
        public boolean isSame(ExecuteTask existedTask, ExecuteTask newAddTask) {
            return existedTask.id == newAddTask.id;
        }

        @Override
        public void executeTask(ExecuteTask task) {
            Logging.d(TAG, "executeTask() taskExecutorTimeScheduled| id = " + task.id);
            long currTime = System.currentTimeMillis();
            resultIndex++;
            if (resultIndex == idSequenceList.size() - 1) {
                synchronized (mLock) {
                    mLock.notify();
                }
            }

            for (ExecuteResult result : idSequenceList) {
                if (task.id == result.id) {
                    long realDiff = currTime - result.executeTime;
                    int delay = result.delay;
                    Logging.d(TAG, "real diff = " + realDiff + " delay= " + delay);

                    //支持5ms的误差
                    assertTrue(realDiff >= delay - 5);
                }
            }

            Logging.d(TAG, "executeTask() passed ---------> " + task.id+ " exec time : " + executeResultList.size());
        }
    };
}
