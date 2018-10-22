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
import android.util.Log;

import java.util.Map;

/**
 * Created by zhangshuliang.zsl on 15/8/24.
 */

public class ConfigTest {
    private static final String TAG = "StartUpConfig";
    private Context mContext;

    private OnProjectExecuteListener mOnProjectExecuteListener;

    public ConfigTest(Context context) {
        mContext = context;
    }

    public void start() {
        config();
        MyLog.e("==ALPHA==", "start -->" + System.currentTimeMillis());
        AlphaManager.getInstance(mContext).start();
    }

    public void setOnProjectExecuteListener(OnProjectExecuteListener listener) {
        mOnProjectExecuteListener = listener;
    }


    private void config() {


        Project.Builder builder = new Project.Builder().withTaskCreator(new MyTaskCreator());
        builder.add(TASK_A);
        builder.add(TASK_B).after(TASK_A);
        builder.add(TASK_C).after(TASK_A);
        builder.add(TASK_D).after(TASK_B, TASK_C);
        builder.setProjectName("innerGroup");

        builder.setOnProjectExecuteListener(new OnProjectExecuteListener() {
            @Override
            public void onProjectStart() {
                MyLog.print("project start");
            }

            @Override
            public void onTaskFinish(String taskName) {
                MyLog.print("project task finish: %s", taskName);
            }

            @Override
            public void onProjectFinish() {
                MyLog.print("project finish.");
            }
        });

        builder.setOnGetMonitorRecordCallback(new OnGetMonitorRecordCallback() {
            @Override
            public void onGetTaskExecuteRecord(Map<String, Long> result) {
                MyLog.print("monitor result: %s", result);
            }

            @Override
            public void onGetProjectExecuteTime(long costTime) {
                MyLog.print("monitor time: %s", costTime);
            }
        });

        Project group = builder.create();

        group.addOnTaskFinishListener(new Task.OnTaskFinishListener() {
            @Override
            public void onTaskFinish(String taskName) {
                MyLog.print("task group finish");
            }
        });


        builder.add(TASK_E);
        builder.add(group).after(TASK_E);
        builder.setOnGetMonitorRecordCallback(new OnGetMonitorRecordCallback() {
            @Override
            public void onGetTaskExecuteRecord(Map<String, Long> result) {
                MyLog.print("monitor result: %s", result);
            }

            @Override
            public void onGetProjectExecuteTime(long costTime) {
                MyLog.print("monitor time: %s", costTime);
            }
        });

        if (mOnProjectExecuteListener != null) {
            builder.setOnProjectExecuteListener(mOnProjectExecuteListener);
        }

        AlphaManager.getInstance(mContext).addProject(builder.create());

//        try {
//            AlphaManager.getInstance(mContext).addProjectsViaFile(mContext.getAssets().open("tasklist.xml"));
//        } catch (Exception e) {
//            AlphaLog.w(e);
//        }

    }

    private static final String TASK_A = "TaskA";
    private static final String TASK_B = "TaskB";
    private static final String TASK_C = "TaskC";
    private static final String TASK_D = "TaskD";
    private static final String TASK_E = "TaskE";
    private static final String TASK_F = "TaskF";
    private static final String TASK_G = "TaskG";
    public static class MyTaskCreator implements ITaskCreator {
        @Override
        public Task createTask(String taskName) {
            Log.d("==ALPHA==", taskName);
            switch (taskName) {
                case TASK_A:
                    return new TaskA();
                case TASK_B:
                    return new TaskB();
                case TASK_C:
                    return new TaskC();
                case TASK_D:
                    return new TaskD();
                case TASK_E:
                    return new TaskE();
                case TASK_F:
                    return new TaskF();
                case TASK_G:
                    return new TaskG();
            }

            return null;
        }
    }

    public static class TaskA extends Task {
        public TaskA() {
            super(TASK_A);
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task A in " + Thread.currentThread().getName());


        }
    }

    public static class TaskB extends Task {
        public TaskB() {
            super(TASK_B);
            setExecutePriority(9);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MyLog.d(TAG, "run task B in " + Thread.currentThread().getName());
        }
    }


    public static class TaskC extends Task {
        public TaskC() {
            super(TASK_C);
            setExecutePriority(1);
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task C in " + Thread.currentThread().getName());
        }
    }


    public static class TaskD extends Task {
        public TaskD() {
            super(TASK_D);
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task D in " + Thread.currentThread().getName());
        }
    }

    public static class TaskE extends Task {
        public TaskE() {
            super(TASK_E, true);
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task E in " + Thread.currentThread().getName());
        }
    }

    public static class TaskF extends Task {
        public TaskF() {
            super(TASK_F);
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task F in " + Thread.currentThread().getName());
        }
    }

    public static class TaskG extends Task {
        public TaskG() {
            super(TASK_G);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MyLog.d(TAG, "run task G in " + Thread.currentThread().getName());
        }
    }



}
