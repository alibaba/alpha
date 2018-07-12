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

import android.util.Log;

/**
 * <p>日志输出类，其函数和{@link Log}基本一致。</p>
 *
 * Created by zhangshuliang.zsl on 15/8/24.
 */
public class MyLog {

    /**
     * 全局的日志过滤tag，{@code Alpha}库输出的日志，都可以用该tag过滤
     */
    public static final String GLOBAL_TAG = "==ALPHA==";

    private static StringBuilder mLogString = new StringBuilder();

    public static void d(String tag, Object obj) {
        Log.d(tag, obj.toString());
        mLogString.append(obj.toString()).append("\n");
    }

    public static synchronized String getLogString() {
        return mLogString.toString();
    }

    public static synchronized void d(String tag, String msg, Object... args) {
        String formattedMsg = String.format(msg, args);
        mLogString.append(formattedMsg).append("\n");
        Log.d(tag, formattedMsg);
    }

    public static void e(String tag, Object obj) {
        Log.e(tag, obj.toString());
        mLogString.append(obj.toString()).append("\n");
    }

    public static void print(Object msg) {
        d(GLOBAL_TAG, msg);
    }

    public static void print(String msg, Object... args) {
        d(GLOBAL_TAG, msg, args);
    }
}
