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
public class AlphaLog {

    /**
     * 全局的日志过滤tag，{@code Alpha}库输出的日志，都可以用该tag过滤
     */
    public static final String GLOBAL_TAG = "==ALPHA==";

    public static void d(String tag, Object obj) {
        if (AlphaConfig.isLoggable()) {
            Log.d(tag, obj.toString());
        }
    }

    public static void d(String tag, String msg, Object... args) {
        if (AlphaConfig.isLoggable()) {
            String formattedMsg = String.format(msg, args);
            Log.d(tag, formattedMsg);
        }
    }

    public static void d(String msg, Object... args) {
        d(GLOBAL_TAG, msg, args);
    }

    public static void e(String tag, Object obj) {
        if (AlphaConfig.isLoggable()) {
            Log.e(tag, obj.toString());
        }
    }


    public static void e(String tag, String msg, Object... args) {
        if (AlphaConfig.isLoggable()) {
            String formattedMsg = String.format(msg, args);
            Log.e(tag, formattedMsg);
        }
    }


    public static void i(String tag, Object obj) {
        if (AlphaConfig.isLoggable()) {
            Log.i(tag, obj.toString());
        }
    }

    public static void w(Exception e) {
        if (AlphaConfig.isLoggable()) {
            e.printStackTrace();
        }
    }

    public static void print(Object msg) {
        d(GLOBAL_TAG, msg);
    }

    public static void print(String msg, Object... args) {
        d(GLOBAL_TAG, msg, args);
    }
}
