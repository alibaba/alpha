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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>多个前后依赖的{@code task}组成的有序集合。一个{@code project}本身是一个{@code task},可以像普通{@code task}
 * 一样嵌入到别的{@code project}中。</p>
 *
 * Created by zhangshuliang.zsl on 15/9/30.
 */
public class Project extends Task implements OnProjectExecuteListener {
    private Task mStartTask;
    private AnchorTask mFinishTask;
    private List<OnProjectExecuteListener> mExecuteListeners = new ArrayList<OnProjectExecuteListener>();
    private static final String DEFAULT_NAME = "AlphaProject";
    private ExecuteMonitor mProjectExecuteMonitor;
    private OnGetMonitorRecordCallback mOnGetMonitorRecordCallback;

    public Project() {
        super(DEFAULT_NAME);
    }

    public Project(String name) {
        super(name);
    }

    @Override
    public void run() {
        //do nothing
    }

    @Override
    public void start() {
        mStartTask.start();
    }

    @Override
    synchronized void addSuccessor(Task task) {
        mFinishTask.addSuccessor(task);
    }

    @Override
    public int getCurrentState() {
        if (mStartTask.getCurrentState() == STATE_IDLE) {
            return STATE_IDLE;
        } else if (mFinishTask.getCurrentState() == STATE_FINISHED) {
            return STATE_FINISHED;
        } else {
            return STATE_RUNNING;
        }
    }

    @Override
    public boolean isRunning() {
        return getCurrentState() == STATE_RUNNING;
    }

    @Override
    public boolean isFinished() {
        return getCurrentState() == STATE_FINISHED;
    }

    @Override
    public void addOnTaskFinishListener(final OnTaskFinishListener listener) {
        mFinishTask.addOnTaskFinishListener(new OnTaskFinishListener() {
            @Override
            public void onTaskFinish(String taskName) {
                listener.onTaskFinish(mName);
            }
        });
    }


    @Override
    public void onProjectStart() {
        mProjectExecuteMonitor.recordProjectStart();

        if (mExecuteListeners != null && !mExecuteListeners.isEmpty()) {
            for (OnProjectExecuteListener listener : mExecuteListeners) {
                listener.onProjectStart();
            }
        }
    }

    @Override
    public void onTaskFinish(String taskName) {
        if (mExecuteListeners != null && !mExecuteListeners.isEmpty()) {
            for (OnProjectExecuteListener listener : mExecuteListeners) {
                listener.onTaskFinish(taskName);
            }
        }
    }

    @Override
    public void onProjectFinish() {

        mProjectExecuteMonitor.recordProjectFinish();
        recordTime(mProjectExecuteMonitor.getProjectCostTime());

        if (mExecuteListeners != null && !mExecuteListeners.isEmpty()) {
            for (OnProjectExecuteListener listener : mExecuteListeners) {
                listener.onProjectFinish();
            }
        }

        if (mOnGetMonitorRecordCallback != null) {
            mOnGetMonitorRecordCallback.onGetProjectExecuteTime(mProjectExecuteMonitor.getProjectCostTime());
            mOnGetMonitorRecordCallback.onGetTaskExecuteRecord(mProjectExecuteMonitor.getExecuteTimeMap());
        }
    }


    /**
     * 设置{@code Project}执行生命周期的回调，可以监听到{@code Project}开始执行与结束执行，{@code Task}执行结束.
     *
     * @param listener {@code Project}执行生命周期的回调
     */
    public void addOnProjectExecuteListener(OnProjectExecuteListener listener) {
        mExecuteListeners.add(listener);
    }

    /**
     * 设置获取{@code Project}执行监控数据的回调接口。
     *
     * @param callback 获取{@code Project}执行监控数据的回调接口。
     */
    public void setOnGetMonitorRecordCallback(OnGetMonitorRecordCallback callback) {
        mOnGetMonitorRecordCallback = callback;
    }

    void setStartTask(Task startTask) {
        mStartTask = startTask;
    }

    void setFinishTask(AnchorTask finishTask) {
        mFinishTask = finishTask;
    }

    void setProjectExecuteMonitor(ExecuteMonitor monitor) {
        mProjectExecuteMonitor = monitor;
    }

    @Override
    void recycle() {
        super.recycle();
        mExecuteListeners.clear();
    }

    /**
     * <p>通过{@code Builder}将多个{@code Task}组成一个{@code Project}。它可以单独拿出去执行，也可以作为
     * 一个子{@code Task}嵌入到另外一个{@code Project}中。</p>
     */
    public static class Builder {
        private Task mCacheTask;
        private boolean mIsSetPosition;
        private AnchorTask mFinishTask;
        private AnchorTask mStartTask;
        private Project mProject;
        private ExecuteMonitor mMonitor;
        private TaskFactory mTaskFactory;

        /**
         * 构建{@code ProjectBuilder}实例。
         */
        public Builder() {
            init();
        }

        /**
         * 创建一个{@code Project}实例。
         *
         * @return {@code Project}，可以直接执行，也可以作为{@code Task}嵌入到其他{@code Project}中。
         */
        public Project create() {
            addToRootIfNeed();
            Project project = mProject;

            //创建完成一个Project，重新初始化builder，以便创建下一个Project
            init();
            return project;
        }

        /**
         * 利用TaskCreator，之后可以直接用task name来操作add和after等逻辑。
         */
        public Builder withTaskCreator(ITaskCreator creator) {
            mTaskFactory = new TaskFactory(creator);
            return Builder.this;
        }

        /**
         * 设置{@code Project}执行生命周期的回调，可以监听到{@code Project}开始执行与结束执行，{@code Task}执行结束
         *
         * @param listener {@code Project}执行生命周期的回调
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder setOnProjectExecuteListener(OnProjectExecuteListener listener) {
            mProject.addOnProjectExecuteListener(listener);
            return Builder.this;
        }

        /**
         * 设置获取{@code Project}执行监控数据的回调接口。
         *
         * @param callback 获取{@code Project}执行监控数据的回调接口。
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder setOnGetMonitorRecordCallback(OnGetMonitorRecordCallback callback) {
            mProject.setOnGetMonitorRecordCallback(callback);
            return Builder.this;
        }

        /**
         * 设置{@code Project}的名称。
         *
         * @param name {@code Project}的名称。
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder setProjectName(String name) {
            mProject.setName(name);
            return Builder.this;
        }

        /**
         * 作用同{@link #add(Task)}但直接用task名称进行操作，需要提前调用{@link #withTaskCreator(ITaskCreator)}创建名称对应的task实例
         * @param taskName 增加的{@code Task}对象的名称。
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder add(String taskName) {
            if (mTaskFactory == null) {
                throw new IllegalAccessError(
                    "You should set a ITaskCreator with withTaskCreator(), and then you can call add() and after() with task name.");
            }

            Task task = mTaskFactory.getTask(taskName);
            add(task);

            return Builder.this;
        }

        /**
         * 作用同{@link #after(Task)}。但直接用task名称进行操作，需要提前调用{@link #withTaskCreator(ITaskCreator)}创建名称对应的task实例
         * @param taskName The task to run after.
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder after(String taskName) {
            if (mTaskFactory == null) {
                throw new IllegalAccessError(
                    "You should set a ITaskCreator with withTaskCreator(), and then you can call add() and after() with task name.");
            }

            Task task = mTaskFactory.getTask(taskName);
            after(task);

            return Builder.this;
        }


        /**
         * 作用同{@link #after(Task...)}。但直接用task名称进行操作，需要提前调用{@link #withTaskCreator(ITaskCreator)}创建名称对应的task实例
         * @param taskNames The task to run after.
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder after(String... taskNames) {
            if (mTaskFactory == null) {
                throw new IllegalAccessError(
                    "You should set a ITaskCreator with withTaskCreator(), and then you can call add() and after() with task name.");
            }

            Task[] tasks = new Task[taskNames.length];
            for (int i = 0, len = taskNames.length; i < len; i++) {
                String taskName = taskNames[i];
                Task task = mTaskFactory.getTask(taskName);
                tasks[i] = task;
            }
            after(tasks);

            return Builder.this;
        }

        /**
         * 增加一个{@code Task}，在调用该方法后，需要调用{@link #after(Task)}来确定。
         * 它在图中的位置，如果不显式指定，则默认添加在最开始的位置。
         *
         * @param task 增加的{@code Task}对象.
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder add(Task task) {
            addToRootIfNeed();
            mCacheTask = task;
            mCacheTask.setExecuteMonitor(mMonitor);
            mIsSetPosition = false;
            mCacheTask.addOnTaskFinishListener(new InnerOnTaskFinishListener(mProject));
            mCacheTask.addSuccessor(mFinishTask);

            return Builder.this;
        }


        /**
         * 指定紧前{@code Task}，必须该{@code Task}执行完才能执行自己。如果不指定具体的紧前{@code Task}默认会最开始执行。
         *
         * @param task The task to run after.
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder after(Task task) {
            task.addSuccessor(mCacheTask);
            mFinishTask.removePredecessor(task);
            mIsSetPosition = true;
            return Builder.this;
        }

        /**
         * 指定紧前{@code Task}，必须这些{@code Task}执行完才能执行自己。如果不指定具体的紧前{@code Task}默认会最开始执行。
         *
         * @param tasks The tasks to run after.
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        public Builder after(Task... tasks) {
            for (Task task : tasks) {
                task.addSuccessor(mCacheTask);
                mFinishTask.removePredecessor(task);
            }

            mIsSetPosition = true;
            return Builder.this;
        }

        private void addToRootIfNeed() {
            if (!mIsSetPosition && mCacheTask != null) {
                mStartTask.addSuccessor(mCacheTask);
            }
        }

        private void init() {
            mCacheTask = null;
            mIsSetPosition = true;
            mProject = new Project();
            mFinishTask = new AnchorTask(false, "==AlphaDefaultFinishTask==");
            mFinishTask.setProjectLifecycleCallbacks(mProject);
            mStartTask = new AnchorTask(true, "==AlphaDefaultStartTask==");
            mStartTask.setProjectLifecycleCallbacks(mProject);
            mProject.setStartTask(mStartTask);
            mProject.setFinishTask(mFinishTask);
            mMonitor = new ExecuteMonitor();
            mProject.setProjectExecuteMonitor(mMonitor);
        }

    }

    private static class InnerOnTaskFinishListener implements OnTaskFinishListener{
        private Project mProject;

        InnerOnTaskFinishListener(Project project) {
            mProject = project;
        }
        @Override
        public void onTaskFinish(String taskName) {
            mProject.onTaskFinish(taskName);
        }
    }


    /**
     * <p>从图的执行角度来讲，应该要有唯一的开始位置和唯一的结束位置。这样就可以准确衡量一个图的开始和结束。并且可以
     * 通过开始点和结束点，方便地将这个图嵌入到另外一个图中去。</p>
     * <p>但是从用户的角度来理解，他可能会有多个{@code task}可以同时开始，也可以有多个{@code task}作为结束点。</p>
     * <p>为了解决这个矛盾，框架提供一个默认的开始节点和默认的结束节点。并且将这两个点称为这个{@code project}的锚点。
     * 用户添加的{@code task}都是添加在开始锚点后，用户的添加的{@code task}后也都会有一个默认的结束锚点。</p>
     * <p>如前面提到，锚点的作用有两个：
     * <li>标记一个{@code project}的开始和结束。</li>
     * <li>当{@code project}需要作为一个{@code task}嵌入到另外一个{@code project}里面时，锚点可以用来和其他{@code task}
     * 进行连接。</li>
     * </p>
     */
    private static class AnchorTask extends Task {
        private boolean mIsStartTask = true;
        private OnProjectExecuteListener mExecuteListener;

        public AnchorTask(boolean isStartTask, String name) {
            super(name);
            mIsStartTask = isStartTask;
        }

        public void setProjectLifecycleCallbacks(OnProjectExecuteListener callbacks) {
            mExecuteListener = callbacks;
        }

        @Override
        public void run() {
            if (mExecuteListener != null) {

                if (mIsStartTask) {
                    mExecuteListener.onProjectStart();
                } else {
                    mExecuteListener.onProjectFinish();
                }
            }
        }

    }


}
