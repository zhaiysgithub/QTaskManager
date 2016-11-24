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
public class SequenceTaskExecutorTest extends AndroidTestCase {

    private static final String TAG = "SequenceTaskExecutorTest";

    private static final int TEST_EXECUTE_TIME = 1000;

    private List<ExecuteResult> idSequenceList = new ArrayList<ExecuteResult>();

    private List<ExecuteResult> executeResultList = new ArrayList<ExecuteResult>();

    private boolean hasFirstTaskRuned = false;

    private int resultIndex = -1;

    public void testSimpleSequenceTaskExecutor() throws InterruptedException {
        idSequenceList.clear();
        resultIndex = -1;
        executeResultList.clear();
        hasFirstTaskRuned = false;

        ITaskExecutor<ExecuteTask> executorHelper = TaskExecutorFactory.createSequenceTaskExecutor();

        executorHelper.setTaskExecutorAbility(taskExecutorSequent);

        executorHelper.getTaskManager().addTask(new ExecuteTask(1));
        idSequenceList.add(new ExecuteResult(1));

        executorHelper.getTaskManager().addTask(new ExecuteTask(2));
        idSequenceList.add(new ExecuteResult(2, ExecuteResult.EXECUTE_END, 1));

        executorHelper.getTaskManager().addTask(new ExecuteTask(3));
        idSequenceList.add(new ExecuteResult(3, ExecuteResult.EXECUTE_END, 2));

        executorHelper.getTaskManager().addTask(new ExecuteTask(4));
        idSequenceList.add(new ExecuteResult(4, ExecuteResult.EXECUTE_END, 3));

        executorHelper.getTaskManager().addTask(new ExecuteTask(5));
        idSequenceList.add(new ExecuteResult(5, ExecuteResult.EXECUTE_END, 4));

        executorHelper.getTaskManager().addTaskBeforeAnchor(new ExecuteTask(6), new ExecuteTask(1));
        idSequenceList.add(searchIndexForId(1, true), new ExecuteResult(6, ExecuteResult.EXECUTE_BEFORE, 1));

        //id 9不存在
        executorHelper.getTaskManager().addTaskAfterAnchor(new ExecuteTask(7), new ExecuteTask(9));
        idSequenceList.add(searchIndexForId(9, false), new ExecuteResult(7, ExecuteResult.EXECUTE_END, 5));

        executorHelper.getTaskManager().addTask(new ExecuteTask(2));
        idSequenceList.add(new ExecuteResult(2, ExecuteResult.EXECUTE_END, 7));

        executorHelper.getTaskManager().addTask(new ExecuteTask(-1));
        idSequenceList.add(new ExecuteResult(-1, ExecuteResult.EXECUTE_END, 2));

        //id 100不存在
        executorHelper.getTaskManager().addTaskBeforeAnchor(new ExecuteTask(8), new ExecuteTask(100));
        idSequenceList.add(searchIndexForId(100, true), new ExecuteResult(8, ExecuteResult.EXECUTE_END, -1));

        executorHelper.getTaskManager().addTaskBeforeAnchor(new ExecuteTask(9), new ExecuteTask(8));
        idSequenceList.add(searchIndexForId(8, true), new ExecuteResult(9, ExecuteResult.EXECUTE_BEFORE, 8));

        executorHelper.getTaskManager().addTaskAfterAnchor(new ExecuteTask(10), new ExecuteTask(8));
        idSequenceList.add(searchIndexForId(8, false), new ExecuteResult(10, ExecuteResult.EXECUTE_END, 8));

        executorHelper.getTaskManager().addTaskBeforeAnchor(new ExecuteTask(11), new ExecuteTask(2));
        idSequenceList.add(searchIndexForId(2, true), new ExecuteResult(11, ExecuteResult.EXECUTE_BEFORE, 2));

        executorHelper.getTaskManager().addTaskAfterAnchor(new ExecuteTask(12), new ExecuteTask(2));
        idSequenceList.add(searchIndexForId(2, false), new ExecuteResult(12, ExecuteResult.EXECUTE_END, 2));

        executorHelper.startExecute();

        synchronized (mLock) {
            mLock.wait();
        }
    }

    public void testRandomSequenceTaskExecutor() throws InterruptedException {
        idSequenceList.clear();
        resultIndex = -1;
        boolean mIsFirst = true;
        executeResultList.clear();

        hasFirstTaskRuned = false;

        ITaskExecutor<ExecuteTask> executorHelper = TaskExecutorFactory.createSequenceTaskExecutor();

        executorHelper.setTaskExecutorAbility(taskExecutorSequent);

        //必须保证id不相等，否则后面的自动判断逻辑会出错
        Random random = new Random();
        for (int i = 0; i < TEST_EXECUTE_TIME; i++) {
            int id;
            //确保id不重复
            do {
                id = random.nextInt(TEST_EXECUTE_TIME * 10);
            } while (idSequenceList.contains(new ExecuteResult(id)));

            int addOperation = random.nextInt(5);
            if (mIsFirst) {
                //第一次从列表内找不到参照物
                executorHelper.getTaskManager().addTask(new ExecuteTask(id));
                idSequenceList.add(new ExecuteResult(id));
                mIsFirst = false;
                continue;
            }

            if (addOperation == 0) { //顺序添加任务
                executorHelper.getTaskManager().addTask(new ExecuteTask(id));
                idSequenceList.add(new ExecuteResult(id,
                        ExecuteResult.EXECUTE_END,
                        idSequenceList.get(idSequenceList.size() - 1).id));

            } else if (addOperation == 1) { //在某个存在的任务之前添加任务
                int anchorId = idSequenceList.get(random.nextInt(idSequenceList.size())).id;

                executorHelper.getTaskManager().addTaskBeforeAnchor(new ExecuteTask(id), new ExecuteTask(anchorId));
                idSequenceList.add(searchIndexForId(anchorId, true),
                        new ExecuteResult(id,
                                ExecuteResult.EXECUTE_BEFORE,
                                anchorId));
            } else if (addOperation == 2) { //在某个存在的任务之后添加任务
                int anchorId = idSequenceList.get(random.nextInt(idSequenceList.size())).id;

                executorHelper.getTaskManager().addTaskAfterAnchor(new ExecuteTask(id), new ExecuteTask(anchorId));
                int insertIndex = searchIndexForId(anchorId, false);
                Logging.d(TAG, "testRandom()| insertIndex= " + insertIndex
                        + " id = " + id + " anchorId= " + anchorId);
                idSequenceList.add(insertIndex, new ExecuteResult(id,
                        ExecuteResult.EXECUTE_END, anchorId));
            } else if (addOperation == 3) { //在某个不存在的任务之前添加任务
                int anchorId = 0;
                do {
                    anchorId = random.nextInt(TEST_EXECUTE_TIME * 10);
                } while (idSequenceList.contains(new ExecuteResult(anchorId)));

                id += TEST_EXECUTE_TIME * 10;

                executorHelper.getTaskManager().addTaskBeforeAnchor(new ExecuteTask(id), new ExecuteTask(anchorId));
                idSequenceList.add(searchIndexForId(anchorId, true),
                        new ExecuteResult(id, ExecuteResult.EXECUTE_END,
                                idSequenceList.get(idSequenceList.size() - 1).id));
            } else if (addOperation == 4) { //在某个不存在的任务之后添加任务
                int anchorId = 0;
                do {
                    anchorId = random.nextInt(TEST_EXECUTE_TIME * 10);
                } while (idSequenceList.contains(new ExecuteResult(anchorId)));

                id += TEST_EXECUTE_TIME * 10 * 2;

                executorHelper.getTaskManager().addTaskAfterAnchor(new ExecuteTask(id), new ExecuteTask(anchorId));
                idSequenceList.add(searchIndexForId(anchorId, false),
                        new ExecuteResult(id, ExecuteResult.EXECUTE_END,
                                idSequenceList.get(idSequenceList.size() - 1).id));
            }
        }

        executorHelper.startExecute();

        synchronized (mLock) {
            mLock.wait();
        }
    }

    private int searchIndexForId(int anchorId, boolean addToPrev) {
        int index = idSequenceList.size();
        for (int i = 0; i < idSequenceList.size(); i++) {
            if (addToPrev) {
                if (idSequenceList.get(i).id == anchorId) {
                    index = i;
                    break;
                }
            } else {
                if (idSequenceList.get(i).id == anchorId) {
                    index = i + 1;
                    for (int j = i + 1; j < idSequenceList.size(); j++) {
                        if (idSequenceList.get(j).anchorId != anchorId) {
                            break;
                        }
                        index = j + 1;
                    }
                    break;
                }
            }
        }
        return index;
    }

    private Object mLock = new Object();

    ITaskExecutorAbility<ExecuteTask> taskExecutorSequent = new ITaskExecutorAbility<ExecuteTask>() {
        @Override
        public boolean isSame(ExecuteTask existedTask, ExecuteTask newAddTask) {
            return existedTask.id == newAddTask.id;
        }

        @Override
        public void executeTask(ExecuteTask task) {
            Logging.d(TAG, "executeTask() id = " + task.id);
            resultIndex++;
            if (resultIndex == idSequenceList.size() - 1) {
                synchronized (mLock) {
                    mLock.notify();
                }
            }

            int index = idSequenceList.indexOf(new ExecuteResult(task.id));
            //此id在结果序列中找不到，这是不正确的
            if (index < 0 || index >= idSequenceList.size()) {
                assertEquals(true, false);
            }

            int indexAnchorInResult;

            ExecuteResult expectedResult = idSequenceList.get(index);
            Logging.d(TAG, "executeTask()| expectedResult id= " + expectedResult.id
                    + " relation= " + expectedResult.relation
                    + " anchorid= " + expectedResult.anchorId);
            switch (expectedResult.relation) {
                case ExecuteResult.EXECUTE_FIRST:
                    //no way to deal whether is right
                    if(hasFirstTaskRuned) {
                        assertTrue(false);
                        hasFirstTaskRuned = true;
                    }
                    break;
                case ExecuteResult.EXECUTE_BEFORE:
                    indexAnchorInResult = executeResultList.indexOf(
                            new ExecuteResult(expectedResult.anchorId));
                    assertTrue(indexAnchorInResult < 0);
                    break;
                case ExecuteResult.EXECUTE_END:
                    indexAnchorInResult = executeResultList.indexOf(
                            new ExecuteResult(expectedResult.anchorId));
                    Logging.d(TAG, "executeTask()| indexAnchorInResult= " + indexAnchorInResult
                     + " anchorId= " + expectedResult.anchorId);
                    assertTrue(indexAnchorInResult >= 0);
                    break;
                default:
                    throw new RuntimeException("wrong relation found= " + expectedResult.relation);
            }

            executeResultList.add(new ExecuteResult(task.id));
            Logging.d(TAG, "executeTask() passed ---------> " + task.id + " exec time : " + executeResultList.size());
        }
    };
}
