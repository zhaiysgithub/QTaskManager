package org.qcode.taskmanager;

/**
 * 执行任务管理器，定义执行任务添加/删除等能力；
 * <p>
 * qqliu
 * 2016/7/14.
 */
public interface ITaskManager<T> {

    /***
     * 向任务管理器内添加一个任务
     * @param newTask
     * @return true表示添加成功；
     * false表示添加失败，添加失败一般发生在allowDuplicatedTask为false时
     */
    boolean addTask(T newTask);

    /***
     * 向任务管理器内添加一个一个延迟执行的任务
     * @param newTask
     * @param delay
     * @return
     */
    boolean addTaskDelayed(T newTask, int delay);

    /***
     * 在任务管理器内已有的任务anchorTask之前执行一个新任务newTask
     * @param newTask
     * @param anchorTask
     * @return
     */
    boolean addTaskBeforeAnchor(T newTask, T anchorTask);

    /***
     * 在任务管理器内已有任务anchorTask之后执行一个新任务newTask
     * @param newTask
     * @param anchorTask
     * @return
     */
    boolean addTaskAfterAnchor(T newTask, T anchorTask);

    /***
     * 在任务管理器内已有任务anchorTask之后执行一个新任务newTask,
     * 新任务不能需等到anchorTask执行完成再执行
     * @param newTask
     * @param anchorTask
     * @param waitAnchorFinish
     * @return
     */
    boolean addTaskAfterAnchor(T newTask, T anchorTask, boolean waitAnchorFinish);

    /***
     * 从任务管理器内移除一个任务
     * @param task
     * @return
     */
    boolean removeTask(T task);
}
