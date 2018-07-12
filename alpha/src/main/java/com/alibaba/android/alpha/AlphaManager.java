/*
 * Copyright 2018 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.android.alpha;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>这个类的职责是将由构造完成的{@code Project}配置不同的模式，满足不同的进程有不同的初始化的需要。</p>
 * <p>这个类提供了两种配置{@code Project}的方式，一种是用{@link Project.Builder}，
 * 自己用代码构建一个具体的{@code Project}；另外一种方式是在XML配置文件中配置，这里直接传入配置文件即可。</p>
 *
 * Created by zhangshuliang.zsl on 15/8/18.
 */
public class AlphaManager {
    /**
     * 启动流程的模式，适用于主进程
     */
    public static final int MAIN_PROCESS_MODE = 0x00000001;
    /**
     * 启动流程的模式，适用于所有非主进程
     */
    public static final int SECONDARY_PROCESS_MODE = 0x00000002;
    /**
     * 启动流程的模式，适用于所有进程，如果不指定具体的模式，默认就是所有进程。
     */
    public static final int ALL_PROCESS_MODE = 0x00000003;

    /**
     * 单独为当前进程设置的{@code Project}
     */
    private Task mProjectForCurrentProcess;

    /**
     * 各种模式对应的启动流程
     */
    private SparseArray<Task> mProjectArray = new SparseArray<Task>();

    private Context mContext;
    private static AlphaManager sInstance = null;
    private volatile boolean mIsStartupFinished = false;

    private OnProjectExecuteListener mProjectExecuteListener = new ProjectExecuteListener();

    private static byte[] sExtraTaskListLock = new byte[0];
    private static byte[] sExtraTaskMapLock = new byte[0];
    /**
     * 执行完成的{@code task}记录。
     */
    private List<String> mFinishedTask = new ArrayList<String>();

    /**
     * 待执行的{@code extra task}的{@code map}，其中{@code key}是依赖的{@code task}的名称，{@code value}
     * 是待执行的{@code task}.
     */
    private ListMultiMap<String, Task> mExtraTaskMap = new ListMultiMap<String, Task>();

    /**
     * 待启动流程完成后执行的{@code etra task}列表。
     */
    private List<Task> mExtraTaskList = new ArrayList<Task>();

    private static byte[] sWaitFinishLock = new byte[0];


    private AlphaManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null. ");
        }
        mContext = context;
    }

    //==============================================================================================
    // PUBLIC API
    //==============================================================================================

    /**
     * @param context {@code ApplicationContext}而不要是{@code Activity}
     * @return {@code StartupManager}的单实例
     */
    public static synchronized AlphaManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AlphaManager(context);
        }

        return sInstance;
    }

    /**
     * 开始启动流程，这里会根据当前的进程执行合适的启动流程。挑选的过程如下：<br>
     * 1.检查是否有为当前进程设置单独的启动流程，若有，则选择结束，执行启动流程。否则转入下一步；<br>
     * 2.检查当前是否主进程，且是否有为主进程配置启动流程，若有，则选择结束，执行启动流程。否则转入下一步；<br>
     * 3.检查当前是否是非主进程，且是否有为非主进程配置启动流程，若有，则选择结束，执行启动流程。否则转入下一步；<br>
     * 4.检查是否有配置适合所有进程的启动流程，若有，则选择结束，执行启动流程。否则选择失败，没有启动流程可以执行。<br>
     */
    public void start() {
        Project project = null;

        do {
            //1.是否有为当前进程单独配置的Project，此为最高优先级
            if (mProjectForCurrentProcess != null) {
                project = (Project) mProjectForCurrentProcess;
                break;
            }

            //2.如果当前是主进程，是否有配置主进程Project
            if (AlphaUtils.isInMainProcess(mContext)
                    && mProjectArray.indexOfKey(MAIN_PROCESS_MODE) >= 0) {
                project = (Project) mProjectArray.get(MAIN_PROCESS_MODE);
                break;
            }

            //3.如果是非主进程，是否有配置非主进程的Project
            if (!AlphaUtils.isInMainProcess(mContext)
                    && mProjectArray.indexOfKey(SECONDARY_PROCESS_MODE) >= 0) {
                project = (Project) mProjectArray.get(SECONDARY_PROCESS_MODE);
                break;
            }

            //4.是否有配置适用所有进程的Project
            if (mProjectArray.indexOfKey(ALL_PROCESS_MODE) >= 0) {
                project = (Project) mProjectArray.get(ALL_PROCESS_MODE);
                break;
            }
        } while (false);

        if (project != null) {
            addListeners(project);
            project.start();
        } else {
            AlphaLog.e(AlphaLog.GLOBAL_TAG, "No startup project for current process.");
        }
    }


    /**
     * 增加一个启动流程，默认适合所有的进程，即{@link #ALL_PROCESS_MODE}模式。由于其范围最广，其优先级也是最低的。
     * 如果后面设置了针对具体进程的启动流程，则启动对应具体进程时，会优先执行该进程对应的启动流程。
     *
     * @param project 启动流程
     */
    public void addProject(Task project) {
        addProject(project, ALL_PROCESS_MODE);
    }

    /**
     * 设置针对某个具体进程的启动流程。
     *
     * @param project     启动流程
     * @param processName 进程名称
     */
    public void addProject(Task project, String processName) {
        if (AlphaUtils.isMatchProcess(mContext, processName)) {
            mProjectForCurrentProcess = project;
        }
    }

    /**
     * 设置某种模式的启动流程。
     *
     * @param project 启动流程
     * @param mode    模式，指定了不同的进程类型。具体有以下三种：<br>
     *                {@link #ALL_PROCESS_MODE}<br>
     *                {@link #MAIN_PROCESS_MODE}<br>
     *                {@link #SECONDARY_PROCESS_MODE}<br>
     */
    public void addProject(Task project, int mode) {
        if (project == null) {
            throw new IllegalArgumentException("project is null");
        }

        if (mode < MAIN_PROCESS_MODE || mode > ALL_PROCESS_MODE) {
            throw new IllegalArgumentException("No such mode: " + mode);
        }

        if (AlphaUtils.isMatchMode(mContext, mode)) {
            mProjectArray.put(mode, project);
        }
    }


    /**
     * 通过XML配置文件来设置启动流程。
     *
     * @param path XML配置文件的路径
     */
    public void addProjectsViaFile(String path) {
        File file = new File(path);
        addProjectsViaFile(file);
    }


    /**
     * 通过XML配置文件来设置启动流程。
     *
     * @param file XML文件对象
     */
    public void addProjectsViaFile(File file) {
        if (!file.exists()) {
            //配置文件不存在，意味着初始化无法进行，所以及时崩溃。
            throw new RuntimeException("Alpha config file " + file + " is not exist!");
        }

        InputStream in = null;
        try {
            in = new FileInputStream(file);
            addProjectsViaFile(in);
        } catch (IOException e) {
            //解析配置文件出现IO错误，意味着初始化无法进行，所以及时崩溃。
            throw new RuntimeException(e);
        } finally {
            AlphaUtils.closeSafely(in);
        }

    }


    /**
     * 通过XML配置文件来设置启动流程。
     *
     * @param in XML文件输入流
     */
    public void addProjectsViaFile(InputStream in) {
        ConfigParser parser = new ConfigParser();
        List<ConfigParser.ProjectInfo> projectInfoList = parser.parse(in);

        //解析配置文件失败，意味着初始化无法进行，及时崩溃。
        if (projectInfoList == null) {
            throw new RuntimeException("Parse alpha config file fail.");
        }

        for (ConfigParser.ProjectInfo projectInfo : projectInfoList) {
            if (TextUtils.isEmpty(projectInfo.processName)) {
                addProject(projectInfo.project, projectInfo.mode);
            } else {
                addProject(projectInfo.project, projectInfo.processName);
            }
        }
    }


    /**
     * 判断当前进程的启动流程是否执行完成。
     *
     * @return true表示执行完成，否则表示未完成。
     */
    public boolean isStartupFinished() {
        return mIsStartupFinished;
    }


    /**
     * <p>配置在启动完成时自动执行的{@code task}。</p>
     * <p>如果当前启动已经完成，该{@code task}会立即执行。如果当前启动暂未完成，则先会把该{@code task}缓存起来，
     * 待启动完成后执行。</p>
     * <p><strong>注意：</strong>此函数是不区分进程的，如果需要区分进程，可以调用{@link #executeAfterStartup(Task, int)}
     * 或者{@link #executeAfterStartup(Task, String)}函数。</p>
     *
     * @param task 在启动结束时执行的{@code task}
     */
    public void executeAfterStartup(Task task) {
        executeAfterStartup(task, AlphaManager.ALL_PROCESS_MODE);
    }

    /**
     * <p>配置在启动完成时自动执行的{@code task}。</p>
     * <p>如果当前启动已经完成，该{@code task}会立即执行。如果当前启动暂未完成，则先会把该{@code task}缓存起来，
     * 待启动完成后执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code mode}判断当前的进程是否符合{@code mode}，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task 在启动结束时执行的{@code task}
     * @param mode 启动流程的执行模型。对应{@link AlphaManager#MAIN_PROCESS_MODE},
     *             {@link AlphaManager#ALL_PROCESS_MODE}, {@link AlphaManager#SECONDARY_PROCESS_MODE}
     */
    public void executeAfterStartup(Task task, int mode) {
        executeAfterStartup(task, mode, Task.DEFAULT_EXECUTE_PRIORITY);
    }


    /**
     * <p>配置在启动完成时自动执行的{@code task}。</p>
     * <p>如果当前启动已经完成，该{@code task}会立即执行。如果当前启动暂未完成，则先会把该{@code task}缓存起来，
     * 待启动完成后根据执行优先级执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code mode}判断当前的进程是否符合{@code mode}，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task            在启动结束时执行的{@code task}
     * @param mode            启动流程的执行模型。对应{@link AlphaManager#MAIN_PROCESS_MODE},
     *                        {@link AlphaManager#ALL_PROCESS_MODE}, {@link AlphaManager#SECONDARY_PROCESS_MODE}
     * @param executePriority 执行优先级。对于<strong>同一时机</strong>执行的{@code task}，由于线程池的线程数有限，
     *                        部分{@code task}可能不能立即执行，所以对于此种情况，需要设置执行优先级。数字越大，优先级越高，执行时机越早。
     */
    public void executeAfterStartup(Task task, int mode, int executePriority) {
        if (!AlphaUtils.isMatchMode(mContext, mode)) {
            return;
        }

        if (isStartupFinished()) {
            task.start();
        } else {
            task.setExecutePriority(executePriority);
            addProjectBindTask(task);
        }
    }


    /**
     * <p>配置在启动完成时自动执行的{@code task}。</p>
     * <p>如果当前启动已经完成，该{@code task}会立即执行。如果当前启动暂未完成，则先会把该{@code task}缓存起来，
     * 待启动完成后根据执行优先级执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code processName}判断当前的进程是否是该进程，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task            在启动结束时执行的{@code task}。
     * @param processName     进程名，只有当前进程符合该进程名，才会执行{@code task}。
     * @param executePriority 执行优先级。对于<strong>同一时机</strong>执行的{@code task}，由于线程池的线程数有限，
     *                        部分{@code task}可能不能立即执行，所以对于此种情况，需要设置执行优先级。数字越大，优先级越高，执行时机越早。
     */
    public void executeAfterStartup(Task task, String processName, int executePriority) {
        if (!AlphaUtils.isMatchProcess(mContext, processName)) {
            return;
        }

        if (isStartupFinished()) {
            task.start();
        } else {
            task.setExecutePriority(executePriority);
            addProjectBindTask(task);
        }
    }

    /**
     * <p>配置在启动完成时自动执行的{@code task}。</p>
     * <p>如果当前启动已经完成，该{@code task}会立即执行。如果当前启动暂未完成，则先会把该{@code task}缓存起来，
     * 待启动完成后执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code processName}判断当前的进程是否是该进程，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task        在启动结束时执行的{@code task}。
     * @param processName 进程名，只有当前进程符合该进程名，才会执行{@code task}。
     */
    public void executeAfterStartup(Task task, String processName) {
        executeAfterStartup(task, processName, Task.DEFAULT_EXECUTE_PRIORITY);
    }

    /**
     * <p>配置在某个名称为{@code taskName}的{@code task}执行结束时，自动执行参数中传入的{@code task}。</p>
     * <p>如果当前名为{@code taskName}的{@code task}已经执行完成，则直接执行传入的{@code task}，否则先缓存，
     * 待指定{@code task}执行完成后再执行。</p>
     * <p><strong>注意：</strong>此函数是不区分进程的，如果需要区分进程，可以调用{@link #executeAfterTask(Task, String, int)}
     * 或者{@link #executeAfterTask(Task, String, String)}函数。</p>
     *
     * @param task 需要执行的{@code task}，在这里配置而不在{@code Project}中是因为他本身不属于{@code Project}，
     *             只不过想尽早执行而已。
     * @param taskName 等待该{@code task}执行完成，执行参数中的{@code task}
     */
    public void executeAfterTask(Task task, String taskName) {
        executeAfterTask(task, taskName, AlphaManager.ALL_PROCESS_MODE);
    }


    /**
     * <p>配置在某个名称为{@code taskName}的{@code task}执行结束时，自动执行参数中传入的{@code task}。</p>
     * <p>如果当前名为{@code taskName}的{@code task}已经执行完成，则直接执行传入的{@code task}，否则先缓存，
     * 待指定{@code task}执行完成后再根据执行优先级执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code mode}判断当前的进程是否符合{@code mode}，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task            需要执行的{@code task}，在这里配置而不在{@code Project}中是因为他本身不属于{@code Project}，
     *                        只不过想尽早执行而已。
     * @param taskName        等待该{@code task}执行完成，执行参数中的{@code task}
     * @param mode            启动流程的执行模型。对应{@link AlphaManager#MAIN_PROCESS_MODE},
     *                        {@link AlphaManager#ALL_PROCESS_MODE}, {@link AlphaManager#SECONDARY_PROCESS_MODE}
     * @param executePriority 执行优先级。对于<strong>同一时机</strong>执行的{@code task}，由于线程池的线程数有限，
     *                        部分{@code task}可能不能立即执行，所以对于此种情况，需要设置执行优先级。数字越大，优先级越高，执行时机越早。
     */
    public void executeAfterTask(Task task, String taskName, int mode, int executePriority) {
        if (!AlphaUtils.isMatchMode(mContext, mode)) {
            return;
        }

        synchronized (sExtraTaskMapLock) {
            if (isStartupFinished() || mFinishedTask.contains(taskName)) {
                task.start();
            } else {
                task.setExecutePriority(executePriority);
                mExtraTaskMap.put(taskName, task);
            }
        }
    }

    /**
     * <p>配置在某个名称为{@code taskName}的{@code task}执行结束时，自动执行参数中传入的{@code task}。</p>
     * <p>如果当前名为{@code taskName}的{@code task}已经执行完成，则直接执行传入的{@code task}，否则先缓存，
     * 待指定{@code task}执行完成后再执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code mode}判断当前的进程是否符合{@code mode}，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task     需要执行的{@code task}，在这里配置而不在{@code Project}中是因为他本身不属于{@code Project}，
     *                 只不过想尽早执行而已。
     * @param taskName 等待该{@code task}执行完成，执行参数中的{@code task}
     * @param mode     启动流程的执行模型。对应{@link AlphaManager#MAIN_PROCESS_MODE},
     *                 {@link AlphaManager#ALL_PROCESS_MODE}, {@link AlphaManager#SECONDARY_PROCESS_MODE}
     */
    public void executeAfterTask(Task task, String taskName, int mode) {
        executeAfterTask(task, taskName, mode, Task.DEFAULT_EXECUTE_PRIORITY);
    }

    /**
     * <p>配置在某个名称为{@code taskName}的{@code task}执行结束时，自动执行参数中传入的{@code task}。</p>
     * <p>如果当前名为{@code taskName}的{@code task}已经执行完成，则直接执行传入的{@code task}，否则先缓存，
     * 待指定{@code task}执行完成后再根据执行优先级执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code processName}判断当前的进程是否是该进程，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task            需要执行的{@code task}，在这里配置而不在{@code Project}中是因为他本身不属于{@code Project}，
     *                        只不过想尽早执行而已。
     * @param taskName        等待该{@code task}执行完成，执行参数中的{@code task}
     * @param processName     进程名，只有当前进程符合该进程名，才会执行{@code task}。
     * @param executePriority 执行优先级。对于<strong>同一时机</strong>执行的{@code task}，由于线程池的线程数有限，
     *                        部分{@code task}可能不能立即执行，所以对于此种情况，需要设置执行优先级。数字越大，优先级越高，执行时机越早。
     */
    public void executeAfterTask(Task task, String taskName, String processName, int executePriority) {
        if (!AlphaUtils.isMatchProcess(mContext, processName)) {
            return;
        }

        synchronized (sExtraTaskMapLock) {
            if (isStartupFinished() || mFinishedTask.contains(taskName)) {
                task.start();
            } else {
                task.setExecutePriority(executePriority);
                mExtraTaskMap.put(taskName, task);
            }
        }
    }

    /**
     * <p>配置在某个名称为{@code taskName}的{@code task}执行结束时，自动执行参数中传入的{@code task}。</p>
     * <p>如果当前名为{@code taskName}的{@code task}已经执行完成，则直接执行传入的{@code task}，否则先缓存，
     * 待指定{@code task}执行完成后再执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code processName}判断当前的进程是否是该进程，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task        需要执行的{@code task}，在这里配置而不在{@code Project}中是因为他本身不属于{@code Project}，
     *                    只不过想尽早执行而已。
     * @param taskName    等待该{@code task}执行完成，执行参数中的{@code task}
     * @param processName 进程名，只有当前进程符合该进程名，才会执行{@code task}。
     */
    public void executeAfterTask(Task task, String taskName, String processName) {
        executeAfterTask(task, taskName, processName, Task.DEFAULT_EXECUTE_PRIORITY);
    }


    /**
     * <p>阻塞当前线程，直到初始化任务完成。</p>
     * <p><strong>注意：如果你在执行task的线程上调用该函数，则存在死锁的风险。</strong></p>
     * <p>例如: <br>
     * 有一个{@code task}在线程A中执行，然后在该线程中调用这个函数，则可能导致死锁。因为此处block需要任务执行
     * 完才能release，而任务又需要在线程A执行。所以应该确保不在执行{@code task}的线程中调用该函数。</p>
     */
    public void waitUntilFinish(){
        synchronized (sWaitFinishLock) {
            while (!mIsStartupFinished) {
                try {
                    sWaitFinishLock.wait();
                } catch (InterruptedException e) {
                    AlphaLog.w(e);
                }
            }
        }
    }


    /**
     * <p>阻塞当前线程，知道初始化任务完成或超时。</p>
     * <p><strong>注意：如果你在执行task的线程上调用该函数，则存在死锁的风险。</strong></p>
     * <p>例如: <br>
     * 有一个{@code task}在线程A中执行，然后在该线程中调用这个函数，则可能导致死锁。因为此处block需要任务执行
     * 完才能release，而任务又需要在线程A执行。所以应该确保不在执行{@code task}的线程中调用该函数。</p>
     *
     * @param timeout 等到超时，如果超时，不管任务是否完成，都继续执行。
     * @return {@code true}等待超时，启动任务有可能没有结束；{@code false}等待未超时，启动顺利结束。
     */
    public boolean waitUntilFinish(final long timeout) {
        long start = System.currentTimeMillis();
        long waitTime = 0;

        synchronized (sWaitFinishLock) {
            while (!mIsStartupFinished && waitTime < timeout) {
                try {
                    sWaitFinishLock.wait(timeout);
                } catch (InterruptedException e) {
                    AlphaLog.w(e);
                }

                waitTime = System.currentTimeMillis() - start;
            }
        }

        return waitTime > timeout;
    }


    //==============================================================================================
    // PRIVATE API
    //==============================================================================================
    private void addListeners(Project project) {
        project.addOnTaskFinishListener(new Task.OnTaskFinishListener() {
            @Override
            public void onTaskFinish(String taskName) {
                mIsStartupFinished = true;
                recycle();
                releaseWaitFinishLock();
            }
        });

        project.addOnProjectExecuteListener(mProjectExecuteListener);

    }

    private void releaseWaitFinishLock() {
        synchronized (sWaitFinishLock) {
            sWaitFinishLock.notifyAll();
        }
    }

    /**
     * 当启动流程完成，相应的资源应该及时释放。
     */
    private void recycle() {
        mProjectForCurrentProcess = null;
        mProjectArray.clear();
    }

    private void executeTaskBindRunnable(String taskName) {
        List<Task> list = mExtraTaskMap.get(taskName);
        AlphaUtils.sort(list);

        for (Task task : list) {
            task.start();
        }

        mExtraTaskMap.remove(taskName);
    }

    private void executeProjectBindRunnables() {
        AlphaUtils.sort(mExtraTaskList);

        for (Task task : mExtraTaskList) {
            task.start();
        }

        mExtraTaskList.clear();
    }

    private void addProjectBindTask(Task task) {
        synchronized (sExtraTaskListLock) {
            mExtraTaskList.add(task);
        }
    }

    private class ProjectExecuteListener implements OnProjectExecuteListener {

        @Override
        public void onProjectStart() {

        }

        @Override
        public void onTaskFinish(String taskName) {
            synchronized (sExtraTaskMapLock) {
                mFinishedTask.add(taskName);

                if (mExtraTaskMap.containsKey(taskName)) {
                    executeTaskBindRunnable(taskName);
                }
            }
        }

        @Override
        public void onProjectFinish() {
            synchronized (sExtraTaskListLock) {
                if (!mExtraTaskList.isEmpty()) {
                    executeProjectBindRunnables();
                }
            }

            synchronized (sExtraTaskMapLock) {
                mFinishedTask.clear();
            }

//            if (TMProcessUtil.isInMainProcess(TMGlobals.getApplication())) {
//                Debug.stopMethodTracing();
//            }

        }
    }

}
