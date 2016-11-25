# QTaskManager抽象任务管理框架

#一、概述
QTaskManager是一个对抽象任务进行调度、分配及管理的简化管理框架，支持并行执行任务、顺序执行任务、按时执行任务、串行执行任务等基本任务操作。
在框架内，任务被抽象为一个泛型，框架不负责任务的执行细节，只要求调用方提供任务执行能力，并在任务执行开始和结束时通知任务状态即可。
使用QTaskManager做简单的任务调度管理，可以帮我们解决一些比较棘手的细节问题，比如：
>1. 假设有两个接口请求A和B，都会改变本地的数据缓存，要想保证数据缓存不随接口的返回顺序得到不同的结果，我们需要控制两个接口请求的时机，A执行完成后B再执行，或者B执行后A再执行。这时我们需要一个控制器，控制A/B谁先来谁先执行。
>2. 再细化一个场景，接口A执行时，接口B在等待A执行，此时用户再次触发了一个接口B的的操作，我们需要取消之前等待执行的接口B的执行。
>3. 或者，我们需要在300ms后执行接口A，在接口A执行完成后执行接口B，等等……
类似的需求数不胜数，其实抽象来看就是一个简单的任务调度问题，分别是任务A/B的串联执行、并行执行和按时间规划执行。

QTaskManager就是对执行策略的抽象，支持的就是以上几种执行方式。下面我们来看下，这个框架的架构方式与接口。

#二、QTaskManager的类设计
![QTaskManager类设计图](https://github.com/qqliu10u/QTaskManager/blob/master/QTaskManager_Class_Diagram.png)
- **TaskExecutorFactory**：框架的对外工厂，可以生成三种任务执行器，分别为：
>1. **默认的任务执行器**(createDefaultTaskExecutor)：支持顺序执行、串行执行和按时执行任务；
>2. **顺序任务执行器**(createSequenceTaskExecutor)：对所有添加的任务，按添加时指定的顺序执行，不论任务是不是定时任务，或者指定了某个任务必须在另一个任务完成之后执行，只要使用此执行器，所有的任务都顺序执行；
>3. **串行任务处理器**(createSerialTaskExecutor)：对所有添加的任务，按添加时指定的顺序一个接着一个执行，同样的，不论任务是不是定时任务，只要使用此执行器，所有的任务都串行执行；

- **ITaskExecutor**：框架最终创建出来的任务执行器，在任务执行的过程中，我们对此接口进行操作，包括任务执行的配置，开始/结束任务执行，添加/删除任务等。
- **ITaskComparator**：用于比较两个任务是否相同，由框架外提供此能力。
- **ITaskExecutorAbility**：指定如何真正执行任务，框架外实现。
- **AbsTaskExecutor**：抽象的任务执行器，具有三个子类实现，分别对于默认任务执行器、顺序任务执行器、串行任务执行器。
- **ITaskManager**：完成任务添加/删除等操作。
- **ITaskPool**：所有待执行任务形成的任务池的抽象接口，最终实现为TaskPoolImpl。

#三、使用方式
##1. 根据需要生成并配置一个任务执行器
``` Java
ITaskExecutor<ExecuteTask> mDefaultTaskExecutor = TaskExecutorFactory.createDefaultTaskExecutor();
```
对生成的任务执行器，我们需要做一些简单的配置：
```Java
//设置发现任务重复时的处理策略
mDefaultTaskExecutor.setDuplicateTaskStrategy(DuplicateTaskStrategy.KEEP_CURRENT);
//设置任务是否在主线程执行
mDefaultTaskExecutor.setRunOnUIThread(false);
//设置辅助框架完成任务执行的能力
mDefaultTaskExecutor.setTaskExecutorAbility(taskExecutorSequent);
```
- **setDuplicateTaskStrategy**用来指定当发现相同任务时应该执行的操作，有三种操作方式：两者皆保留**KEEP_ALL**、保持已经添加的任务**KEEP_PREVIOUS**、更新任务**KEEP_CURRENT**（删除之前的任务）。
- **setRunOnUIThread**用来指定任务执行器内的任务在主线程还是子线程执行。
- **setTaskExecutorAbility**用来配置需要框架实现的能力，包括两点：比较任务是否相同以及正在执行任务。示例如下：
```Java
ITaskExecutorAbility<ExecuteTask> mTaskExecutorAbilitySequent = new ITaskExecutorAbility<ExecuteTask>() {

        @Override
        public boolean isSame(ExecuteTask taskLeft, ExecuteTask tastRight) {
            return taskLeft.id == tastRight.id;
        }

        @Override
        public void executeTask(ExecuteTask task) {
            task.run();
        }
    };
```
##2. 启动/终止任务执行器
上面我们指定好了任务执行器的任务存储策略和任务执行方式，接下来我们就可以启动/终止任务执行器了：
```Java
mDefaultTaskExecutor.startExecute();
mDefaultTaskExecutor.stopExecute();
```
任务执行器可以不断的被开始/停止，但是建议这两个操作只执行一次。
##3. 添加/删除任务
任务执行器被启动后，并不会执行任务，因为我们并没有添加任何任务到执行器内，这时候我们需要借助ITaskManager接口添加/删除任务：
```Java
//获取TaskManager
ITaskManager<ExecuteTask> mTaskManager = mDefaultTaskExecutorHelper.getTaskManager();

//向任务执行器内添加一个待执行任务
mTaskManager.addTask(new ExecuteTask(id));

//向任务执行器内添加一个在300ms后执行的任务（定时任务）
mTaskManager.addTaskDelayed(new ExecuteTask(id), 300);

//向任务执行器内已存在的任务anchorId前面添加一个任务，此任务先于anchorId任务执行
mTaskManager.addTaskBeforeAnchor(
    new ExecuteTask(id), new ExecuteTask(anchorId));

//向任务执行器内已存在的任务anchorId后面添加一个任务，此任务后于anchorId任务执行
mTaskManager.addTaskAfterAnchor(
    new ExecuteTask(id), new ExecuteTask(anchorId));

//向任务执行器内已存在的任务anchorId后面添加一个任务，此任务后于anchorId任务执行，
//第三个参数true表示此任务等待其anchor任务执行完成后才能执行，
//所以如果是默认任务执行器，此任务会在anchorId执行完成后才会执行，但如果是顺序任务执行器，则忽略此规则
mTaskManager.addTaskAfterAnchor(
    new ExecuteTask(id), new ExecuteTask(anchorId), true);

//从任务执行器内删除任务id为anchorId的执行任务
mTaskManager.removeTask(new ExecuteTask(anchorId));
```
上面就是框架支持的几种任务添加/删除策略，这些情况基本覆盖了我们常用的任务调度策略（顺序、串行和定时执行）。
##4. 任务执行状态通知
对于串行任务执行器或默认的任务执行器，我们需要通知任务执行开始完成事件，来帮助框架执行下一个任务。
其中串行任务执行器是必须通知的，而默认任务执行器则非必须，此通知仅对调用接口addTaskAfterAnchor(T task, T anchorTask, true)的任务task有效，因为任务task需要在anchorTask执行完成后执行。
```Java
mSerialTaskExecutor.notifyTaskBegin(mCurrentTask);
mSerialTaskExecutor.notifyTaskFinish(mCurrentTask);
```
好了，经过以上几步集成，一个简单的任务执行器就启动并开始工作了。但是前文中我们说好了的支持并行任务处理呢？我们上面只提供了顺序执行、定时执行和串行执行三种执行方式，并行任务执行怎么实现？
##5. 并行任务处理
实现并行任务处理其实并不难，我们要借助的是框架外执行任务处理的接口来实现。那么为什么框架内不直接实现并行任务处理呢？我的考虑是：并行处理任务需要开很多线程，这就要设计线程池管理策略，如果框架实现，无法灵活地设置线程池管理策略，所以还是交给外面算了。
下面我们来看下一个简单的无线程池管理的并行任务处理实现：
```Java
ITaskExecutorAbility<ExecuteTask> taskExecutorParallel = new ITaskExecutorAbility<ExecuteTask>() {
        @Override
        public boolean isSame(ExecuteTask existedTask, ExecuteTask newAddTask) {
            return existedTask.id == newAddTask.id;
        }

        @Override
        public void executeTask(final ExecuteTask task) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    task.run();
                }
            }).start();
        }
    };
```
是的，就这么简单，在框架对外通知要执行任务时，给任务执行包装一个线程即可。当然复杂的做法不是这样的，否则每个任务一个线程应用直接就卡死了，一般做法封装一个线程池，由线程池来执行task。
#四、总结
QTaskManager任务管理框架的设计与使用的讲解就此结束了，框架支持顺序执行、串行执行和按时执行抽象任务，支持配置子线程和主线程内执行任务，支持任务的重复时的处理策略等，基本上能满足简单的任务管理需求了。

功能刚刚完成，应该还不太成熟，如有缺陷，请不吝指出，Thanks a lot。