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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Alpha的配置类</p>
 * Created by zhangshuliang.zsl on 15/10/30.
 */
public class AlphaConfig {

    /**
     * 日志输出开关，默认是打开的
     */
    private static boolean sIsLoggable = true;
    private static int sCoreThreadNum = Runtime.getRuntime().availableProcessors();
    private static ThreadFactory sThreadFactory;
    private static ExecutorService sExecutor;
    private static int sWarningTime = 400;
    private static boolean sShowToastToAlarm = false;
    private static Context sContext;

    //==============================================================================================
    // PUBLIC API
    //==============================================================================================

    /**
     * 设置执行{@code task}的线程池的核心线程数，默认是CPU数。
     *
     * @param coreThreadNum 核心线程数
     */
    public static void setCoreThreadNum(int coreThreadNum) {
        sCoreThreadNum = coreThreadNum;
    }

    /**
     * 设置执行{@code task}的线程池的{@code ThreadFactory}，默认会有一个{@code ThreadFactory}，将创建的
     * {@code Thread}命名为“Alpha Thread #num”。
     *
     * @param threadFactory 执行{@code task}的线程池的{@code ThreadFactory}
     */
    public static void setThreadFactory(ThreadFactory threadFactory) {
        sThreadFactory = threadFactory;
    }

    /**
     * 设置执行{@code task}的线程池，默认线程池，核心线程池数是CPU数，缓存队列无穷大。包括核心线程在内，当线程空闲
     * 超过1分钟，会将线程释放。
     *
     * @param executorService 执行{@code task}的线程池
     */
    public static void setExecutorService(ExecutorService executorService) {
        sExecutor = executorService;
    }

    /**
     * 设置日志输出开关，默认是打开的
     * @param isLoggable {@code true}开启日志，否则关闭日志。
     */
    public static void setLoggable(boolean isLoggable) {
        sIsLoggable = isLoggable;
    }

    /**
     * 设置{@code task}执行时间的境界值，超过这个警戒值，则会通过toast来告警。默认值是400毫秒。
     *
     * @param warningTime {@code task}执行时间的境界值
     */
    public static void setWarningTime(int warningTime) {
        sWarningTime = warningTime;
    }

    /**
     * 设置是否通过弹出toast来告警，默认值是{@code false}。
     *
     * @param context          show toast需要Context实例。
     * @param showToastToAlarm {@code true}会弹出toast来告警，否则不会。
     */
    public static void setShowToastToAlarm(Context context, boolean showToastToAlarm) {
        sContext = context;
        sShowToastToAlarm = showToastToAlarm;
    }


    //==============================================================================================
    // INNER API
    //==============================================================================================

    /*package*/ static boolean isLoggable() {
        return sIsLoggable;
    }

    /*package*/ static ThreadFactory getThreadFactory() {
        if (sThreadFactory == null) {
            sThreadFactory = getDefaultThreadFactory();
        }

        return sThreadFactory;
    }

    /*package*/ static ExecutorService getExecutor() {
        if (sExecutor == null) {
            sExecutor = getDefaultExecutor();
        }

        return sExecutor;
    }

    /*package*/ static int getWarmingTime() {
        return sWarningTime;
    }

    /*package*/ static boolean shouldShowToastToAlarm() {
        return sShowToastToAlarm;
    }

    /*package*/ static Context getContext() {
        return sContext;
    }


    private static ThreadFactory getDefaultThreadFactory() {
        ThreadFactory defaultFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "Alpha Thread #" + mCount.getAndIncrement());
            }
        };

        return defaultFactory;
    }

    //==============================================================================================
    // PRIVATE METHOD
    //==============================================================================================

    private static ExecutorService getDefaultExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(sCoreThreadNum, sCoreThreadNum,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                getThreadFactory());
        executor.allowCoreThreadTimeOut(true);

        return executor;
    }
}
