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

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * <p>这个类将一个个关联的{@code Task}，组织成PERT网路图的方式进行执行。可以通过{@link Project.Builder}
 * 将{@code Task}组装成完整的{@code Project}，该{@code Project}可以直接执行，也可以嵌套在另外一个{@code Project}
 * 中作为其中一个{@code Task}执行。</p>
 *
 * Created by zhangshuliang.zsl on 15/8/18.
 */
public abstract class Task {

    /**
     * {@code Task}执行状态，{@code Task}尚未执行
     */
    public static final int STATE_IDLE = 0;

    /**
     * {@code Task}执行状态，{@code Task}正在执行中
     */
    public static final int STATE_RUNNING = 1;

    /**
     * {@code Task}执行状态，{@code Task}已经执行完毕
     */
    public static final int STATE_FINISHED = 2;

    /**
     * {@code Task}执行状态，{@code Task}等待执行
     */
    public static final int STATE_WAIT = 3;

    /**
     * 默认的执行优先级
     */
    public static final int DEFAULT_EXECUTE_PRIORITY = 0;

    /**
     * 执行优先级，由于线程池是有限的，对于同一时机执行的task，其执行也可能存在先后顺序。值越小，越先执行。
     */
    private int mExecutePriority = DEFAULT_EXECUTE_PRIORITY;

    /**
     * 线程优先级，优先级高，则能分配到更多的cpu时间
     */
    private int mThreadPriority;

    private static ExecutorService sExecutor = AlphaConfig.getExecutor();

    private static Handler sHandler = new Handler(Looper.getMainLooper());

    /**
     * 是否在主线程执行
     */
    private boolean mIsInUiThread;

    private Runnable mInternalRunnable;

    protected String mName;

    private List<OnTaskFinishListener> mTaskFinishListeners = new ArrayList<OnTaskFinishListener>();

    private volatile int mCurrentState = STATE_IDLE;

    private List<Task> mSuccessorList = new ArrayList<Task>();
    protected Set<Task> mPredecessorSet = new HashSet<Task>();

    private ExecuteMonitor mTaskExecuteMonitor;


    /**
     * 构造{@code Task}对象，必须要传入{@code name}，便于确定当前是在哪一个任务中。该{@code Task}在异步线程
     * 中执行。
     *
     * @param name {@code Task}名字
     */
    public Task(String name) {
        this(name, android.os.Process.THREAD_PRIORITY_DEFAULT);
    }

    /**
     * 构造{@code Task}对象。
     *
     * @param name           {@code Task}名字
     * @param threadPriority 线程优先级。对应的是{@link android.os.Process}里面的定义，
     *                       例如{@link android.os.Process#THREAD_PRIORITY_DEFAULT},
     *                       {@link android.os.Process#THREAD_PRIORITY_BACKGROUND},
     *                       {@link android.os.Process#THREAD_PRIORITY_FOREGROUND}等。
     */
    public Task(String name, int threadPriority) {
        mName = name;
        mThreadPriority = threadPriority;
    }

    /**
     * 构造{@code Task}对象。
     *
     * @param name {@code Task}名字
     * @param isInUiThread 是否在UI线程执行，true表示在UI线程执行，false表示在非UI线程执行，默认在非UI线程执行。
     *                     <strong>注意：如果在UI线程执行，则不能再使用{@link AlphaManager#waitUntilFinish()}，否则会造成死锁。</strong>
     */
    public Task(String name, boolean isInUiThread) {
        mName = name;
        mIsInUiThread = isInUiThread;
    }

    //==============================================================================================
    // PUBLIC API
    //==============================================================================================

    /**
     * 执行当前{@code Task}的任务，这里会调用用户自定义的{@link #run()}。
     */
    public synchronized void start() {
        if (mCurrentState != STATE_IDLE) {
            throw new RuntimeException("You try to run task " + mName + " twice, is there a circular dependency?");
        }

        switchState(STATE_WAIT);

        if (mInternalRunnable == null) {
            mInternalRunnable = new Runnable() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(mThreadPriority);
                    long startTime = System.currentTimeMillis();

                    switchState(STATE_RUNNING);
                    Task.this.run();
                    switchState(STATE_FINISHED);

                    long finishTime = System.currentTimeMillis();
                    recordTime((finishTime - startTime));

                    notifyFinished();
                    recycle();
                }
            };
        }

        if (mIsInUiThread) {
            sHandler.post(mInternalRunnable);
        } else {
            sExecutor.execute(mInternalRunnable);
        }
    }

    /**
     * <p>增加{@code Task}执行结束的监听，当该{@code Task}执行结束时，会回调
     * {@link Task.OnTaskFinishListener#onTaskFinish(String)}。</p>
     * <strong>注意：</strong>回调函数在{@code Task}所在线程中回调，注意线程安全。
     *
     * @param listener 监听{@code Task}执行结束的{@code listener}
     */
    public void addOnTaskFinishListener(OnTaskFinishListener listener) {
        if (!mTaskFinishListeners.contains(listener)) {
            mTaskFinishListeners.add(listener);
        }
    }

    /**
     * 在其中实现该{@code Task}具体执行的逻辑。<br>
     * <strong>注意：</strong>该函数应该只由框架的{@link #start()}来调用。
     */
    public abstract void run();

    /**
     * 查询当前该{@code Task}执行的状态。
     *
     * @return 当前执行状态，状态码如下：
     * {@link #STATE_FINISHED}, {@link #STATE_IDLE}, {@link #STATE_RUNNING}
     */
    public int getCurrentState() {
        return mCurrentState;
    }

    /**
     * 判断当前{@code Task}是否正在运行，即状态是否是{@link #STATE_RUNNING}
     *
     * @return {@code true}表示当前正在执行中，否则表示为执行或者执行已经结束。
     */
    public boolean isRunning() {
        return mCurrentState == STATE_RUNNING;
    }

    /**
     * 判断当前{@code Task}已经完成，即状态是否是{@link #STATE_FINISHED}
     *
     * @return {@code true}表示已经执行结束，否则表示尚未执行或者正在执行中。
     */
    public boolean isFinished() {
        return mCurrentState == STATE_FINISHED;
    }


    /**
     * 设置执行优先级。对于<strong>同一时机</strong>执行的{@code task}，由于线程池的线程数有限，
     * 部分{@code task}可能不能立即执行，所以对于此种情况，需要设置执行顺序优先级。值越小，越先执行。
     *
     * @param executePriority 执行优先级。数字越大，优先级越高，执行时机越早。
     */
    public void setExecutePriority(int executePriority) {
        mExecutePriority = executePriority;
    }

    /**
     * @return 同一时机执行的task的执行顺序优先级。数字越小，执行时机越早。
     */
    public int getExecutePriority() {
        return mExecutePriority;
    }

    //==============================================================================================
    // INNER API
    //==============================================================================================

    /**
     * 增加紧前{@code Task}
     *
     * @param task 紧前{@code Task}
     */
    /*package*/ void addPredecessor(Task task) {
        mPredecessorSet.add(task);
    }


    /**
     * 移除紧前{@code Task}
     *
     * @param task 紧前{@code Task}
     */
    /*package*/ void removePredecessor(Task task) {
        mPredecessorSet.remove(task);
    }


    /**
     * 增加紧后{@code Task}
     *
     * @param task 紧后{@code Task}
     */
    /*package*/ void addSuccessor(Task task) {
        if (task == this) {
            throw new RuntimeException("A task should not after itself.");
        }

        task.addPredecessor(this);
        mSuccessorList.add(task);
    }


    /**
     * 通知所有紧后{@code Task}以及{@code OnTaskFinishListener}自己执行完成。
     */
    /*package*/ void notifyFinished() {
        if (!mSuccessorList.isEmpty()) {
            AlphaUtils.sort(mSuccessorList);

            for (Task task : mSuccessorList) {
                task.onPredecessorFinished(this);
            }
        }

        if (!mTaskFinishListeners.isEmpty()) {
            for (OnTaskFinishListener listener : mTaskFinishListeners) {
                listener.onTaskFinish(mName);
            }

            mTaskFinishListeners.clear();
        }
    }


    /**
     * 某一个紧前{@code Task}执行完成，如果所有的紧前{@code Task}都执行完成，则调用自己的{@link #start()}
     * 执行自己的任务。<br>
     * 该函数由紧前{@code Task}的{@link #notifyFinished()}来调用。
     */
    /*package*/
    synchronized void onPredecessorFinished(Task beforeTask) {

        if (mPredecessorSet.isEmpty()) {
            return;
        }

        mPredecessorSet.remove(beforeTask);
        if (mPredecessorSet.isEmpty()) {
            start();
        }

    }

    /*package*/ void setName(String name) {
        mName = name;
    }

    /*package*/ void setThreadPriority(int threadPriority) {
        mThreadPriority = threadPriority;
    }

    /*package*/ void setExecuteMonitor(ExecuteMonitor monitor) {
        mTaskExecuteMonitor = monitor;
    }

    /**
     * 这个函数在执行结束时被调用，及时释放占用的资源
     */
    /*package*/ void recycle() {
        mSuccessorList.clear();
        mTaskFinishListeners.clear();
    }

    protected void recordTime(long costTime) {
        if (mTaskExecuteMonitor != null) {
            mTaskExecuteMonitor.record(mName, costTime);
        }
    }


    //==============================================================================================
    // PRIVATE METHOD
    //==============================================================================================
    private void switchState(int state) {
        mCurrentState = state;
    }


    //==============================================================================================
    // INNER CLASSES
    //==============================================================================================

    /**
     * 一个task完成时的回调
     */
    public interface OnTaskFinishListener {

        /**
         * 当task完成时，会回调这个函数。
         * <strong>注意：</strong>该函数会在{@code Task}所在线程中回调，注意线程安全。
         *
         * @param taskName 当前结束的{@code Task}名称
         */
        public void onTaskFinish(String taskName);
    }


}
