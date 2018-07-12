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

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.alibaba.android.alpha.AlphaManager.ALL_PROCESS_MODE;
import static com.alibaba.android.alpha.AlphaManager.MAIN_PROCESS_MODE;
import static com.alibaba.android.alpha.AlphaManager.SECONDARY_PROCESS_MODE;

/**
 * <p>通用工具类。</p>
 *
 * Created by zhangshuliang.zsl on 15/8/27.
 */
public class AlphaUtils {

    private static Comparator<Task> sTaskComparator = new Comparator<Task>() {
        @Override
        public int compare(Task lhs, Task rhs) {
            return lhs.getExecutePriority() - rhs.getExecutePriority();
        }
    };

    /**
     * 根据{@code task}的执行优先级，对其进行排序。
     *
     * @param tasks 需要排序的task列表
     */
    public static void sort(List<Task> tasks) {
        if (tasks.size() <= 1) {
            return;
        }

        Collections.sort(tasks, sTaskComparator);
    }

    /**
     * Close a {@link Closeable} object safely.
     *
     * @param closeable Object to close.
     * @return True close successfully, false otherwise.
     */
    public static boolean closeSafely(Closeable closeable) {

        if (closeable == null) {
            return false;
        }

        boolean ret = false;

        try {
            closeable.close();
            ret = true;
        } catch (Exception e) {
            AlphaLog.w(e);
        }

        return ret;
    }


    /**
     * Close a {@link Cursor} object safely.
     *
     * @param cursor to close cursor
     * {@link Cursor} is not a {@link Closeable} until 4.1.1, so we should supply this method to
     * close {@link Cursor} beside {@link #closeSafely(Closeable)}
     * @return true is close successfully, false otherwise.
     */
    public static boolean closeSafely(Cursor cursor) {
        if (cursor == null) {
            return false;
        }

        boolean ret = false;

        try {
            cursor.close();
            ret = true;
        } catch (Exception e) {
            AlphaLog.w(e);
        }

        return ret;
    }

    private static String sProcessName;

    /**
     * @param context The context used to get process name.
     * @return Name of current process.
     */
    public static String getCurrProcessName(Context context) {
        String name = getCurrentProcessNameViaLinuxFile();

        if (TextUtils.isEmpty(name) && context != null) {
            name = getCurrentProcessNameViaActivityManager(context);
        }

        return name;
    }

    /**
     * Check if current process is main process.
     *
     * @param context The context used check if main process.
     * @return True if current process is main process, false otherwise.
     */
    public static boolean isInMainProcess(Context context) {
        String mainProcessName = context.getPackageName();
        String currentProcessName = getCurrProcessName(context);
        return mainProcessName != null && mainProcessName.equalsIgnoreCase(currentProcessName);
    }

    private static String getCurrentProcessNameViaLinuxFile() {
        int pid = android.os.Process.myPid();
        String line = "/proc/" + pid + "/cmdline";
        FileInputStream fis = null;
        String processName = null;
        byte[] bytes = new byte[1024];
        int read = 0;

        try {
            fis = new FileInputStream(line);
            read = fis.read(bytes);
        } catch (Exception e) {
            AlphaLog.w(e);
        } finally {
            AlphaUtils.closeSafely(fis);
        }

        if (read > 0) {
            processName = new String(bytes, 0, read);
            processName = processName.trim();
        }

        return processName;
    }

    private static String getCurrentProcessNameViaActivityManager(Context context) {
        if (context == null) {
            return null;
        }
        if (sProcessName != null) {
            return sProcessName;
        }
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (mActivityManager == null) {
            return null;
        }
        List<ActivityManager.RunningAppProcessInfo> processes = mActivityManager.getRunningAppProcesses();
        if (processes == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : processes) {
            if (appProcess != null && appProcess.pid == pid) {
                sProcessName = appProcess.processName;
                break;
            }
        }
        return sProcessName;
    }

    /**
     * 在{@link AlphaManager#addProject(Task, String)}中进行这个判断，只有进程名和当前进程相同，才有必要去持有该{@code Project}.
     *
     * @param context The context used to check process.
     * @param processName 需要配置启动{@code Project}的进程名
     * @return 进程名是否和当前进程名一致，符合返回{@code true}，否则返回{@code false}。
     */
    public static boolean isMatchProcess(Context context, String processName) {
        String currentProcessName = AlphaUtils.getCurrProcessName(context);
        return TextUtils.equals(processName, currentProcessName);
    }

    /**
     * 在{@link AlphaManager#addProject(Task, String)}中进行这个判断，只有当前进程命中指定的{@code mode}，才有必要去持有该{@code
     * Project}.
     *
     * @param context The context used to check process.
     * @param mode 启动{@code Project}的模式
     * @return 当前进程是否符合该种模式，符合返回{@code true}，否则返回{@code false}。
     */
    public static boolean isMatchMode(Context context, int mode) {
        if (mode == ALL_PROCESS_MODE) {
            return true;
        }

        if (AlphaUtils.isInMainProcess(context) && mode == MAIN_PROCESS_MODE) {
            return true;
        }

        if (!AlphaUtils.isInMainProcess(context) && mode == SECONDARY_PROCESS_MODE) {
            return true;
        }

        return false;
    }


}
