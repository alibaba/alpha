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
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>监控{@code Project}执行性能的类。会记录每一个{@code Task}执行时间，以及整个{@code Project}执行时间。</p>
 * Created by zhangshuliang.zsl on 15/11/4.
 */
class ExecuteMonitor {
    private Map<String, Long> mExecuteTimeMap = new HashMap<String, Long>();
    private long mStartTime;
    private long mProjectCostTime;
    private Handler mHandler;

    /**
     * 记录{@code task}执行时间。
     *
     * @param taskName    {@code task}的名称
     * @param executeTime 执行的时间
     */
    public synchronized void record(String taskName, long executeTime) {
        AlphaLog.d(AlphaLog.GLOBAL_TAG, "AlphaTask-->Startup task %s cost time: %s ms, in thread: %s", taskName, executeTime, Thread.currentThread().getName());
        if (executeTime >= AlphaConfig.getWarmingTime()) {
            toastToWarn("AlphaTask %s run too long, cost time: %s", taskName, executeTime);
        }

        mExecuteTimeMap.put(taskName, executeTime);
    }

    /**
     * @return 已执行完的每个task的执行时间。
     */
    public synchronized Map<String, Long> getExecuteTimeMap() {
        return mExecuteTimeMap;
    }

    /**
     * 在{@code Project}开始执行时打点，记录开始时间。
     */
    public void recordProjectStart() {
        mStartTime = System.currentTimeMillis();
    }

    /**
     * 在{@code Project}结束时打点，记录耗时。
     */
    public void recordProjectFinish() {
        mProjectCostTime = System.currentTimeMillis() - mStartTime;
        AlphaLog.d("==ALPHA==", "tm start up cost time: %s ms", mProjectCostTime);
    }


    /**
     * @return {@code Project}执行时间。
     */
    public long getProjectCostTime() {
        return mProjectCostTime;
    }

    /**
     * 通过弹出{@code toast}来告警。
     *
     * @param msg     告警内容
     * @param args    format参数
     */
    private void toastToWarn(final String msg, final Object... args) {
        if (AlphaConfig.shouldShowToastToAlarm()) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    String formattedMsg;

                    if (args == null) {
                        formattedMsg = msg;
                    } else {
                        formattedMsg = String.format(msg, args);
                    }

                    Toast.makeText(AlphaConfig.getContext(), formattedMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }

        return mHandler;
    }
}
