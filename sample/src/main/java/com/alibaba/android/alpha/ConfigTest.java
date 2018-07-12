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
        MyLog.e("==ALPHA==", "start");
        AlphaManager.getInstance(mContext).start();

        AlphaManager.getInstance(mContext).waitUntilFinish(2000);

        MyLog.e("==ALPHA==", "finish");
    }

    public void setOnProjectExecuteListener(OnProjectExecuteListener listener) {
        mOnProjectExecuteListener = listener;
    }


    private void config() {

        Task a = new TaskA();
        Task b = new TaskB();
        Task c = new TaskC();
        Task d = new TaskD();
        Task e = new TaskE();

        Project.Builder builder = new Project.Builder();
        builder.add(a);
        builder.add(b).after(a);
        builder.add(c).after(a);
        builder.add(d).after(b, c);
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


        builder.add(e);
        builder.add(group).after(e);
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

    public static class TaskA extends Task {
        public TaskA() {
            super("TaskA");
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task A in " + Thread.currentThread().getName());


        }
    }

    public static class TaskB extends Task {
        public TaskB() {
            super("TaskB");
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
            super("TaskC");
            setExecutePriority(1);
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task C in " + Thread.currentThread().getName());
        }
    }


    public static class TaskD extends Task {
        public TaskD() {
            super("TaskD");
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task D in " + Thread.currentThread().getName());
        }
    }

    public static class TaskE extends Task {
        public TaskE() {
            super("TaskE", true);
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task E in " + Thread.currentThread().getName());
        }
    }

    public static class TaskF extends Task {
        public TaskF() {
            super("TaskF");
        }

        @Override
        public void run() {
            MyLog.d(TAG, "run task F in " + Thread.currentThread().getName());
        }
    }

    public static class TaskG extends Task {
        public TaskG() {
            super("TaskG");
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

    public class SampleTask extends Task {
        public SampleTask() {
            super("SampleTask");
        }

        @Override
        public void run() {
            //do something, print a msg for example.
            Log.d(TAG, "run SampleTask");
        }
    }


}
