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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.TextUtils;
import android.util.Xml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * <p>由于我们提供了通过XML配置文件设置启动流程的方式，所以这个类的职责是将XML配置文件解析成具体的{@code Project}</p>
 *
 * Created by zhangshuliang.zsl on 15/9/1.
 */
class ConfigParser {

    /*******************************启动流程配置文件的TAG关键字****************************************/
    private static final String TAG_PROJECTS = "projects";
    private static final String TAG_PROJECT = "project";
    private static final String TAG_TASK = "task";

    /*******************************启动流程配置文件的属性关键字***************************************/
    private static final String ATTRIBUTE_TASK_NAME = "name";
    private static final String ATTRIBUTE_TASK_CLASS = "class";
    private static final String ATTRIBUTE_TASK_PREDECESSOR = "predecessor";
    private static final String ATTRIBUTE_PROJECT_MODE = "mode";
    private static final String ATTRIBUTE_PROCESS_NAME = "process";
    private static final String ATTRIBUTE_THREAD_PRIORITY = "threadPriority";
    private static final String ATTRIBUTE_EXECUTE_PRIORITY = "executePriority";

    /*******************************启动流程配置文件的属性值关键字**************************************/
    private static final String MODE_ALL_PROCESS = "allProcess";
    private static final String MODE_MAIN_PROCESS = "mainProcess";
    private static final String MODE_SECONDARY_PROCESS = "secondaryProcess";
    private static final String PREDECESSOR_DIVIDER = ",";

    /**
     * 解析配置XML文件
     *
     * @param in 配置XML的输入流
     * @return 启动Project的信息列表
     */
    public List<ProjectInfo> parse(InputStream in) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            List<TaskBundle> taskBundles = readProjects(parser);
            List<ProjectInfo> result = new ArrayList<ProjectInfo>();

            for (TaskBundle info : taskBundles) {
                ProjectInfo projectInfo = createProject(info);
                result.add(projectInfo);
            }

            return result;
        } catch (FileNotFoundException e) {
            AlphaLog.w(e);
        } catch (XmlPullParserException e) {
            AlphaLog.w(e);
        } catch (IOException e) {
            AlphaLog.w(e);
        }

        return null;
    }


    private List<TaskBundle> readProjects(XmlPullParser parser) throws XmlPullParserException, IOException{
        List<TaskBundle> projects = new ArrayList<TaskBundle>();

        parser.require(XmlPullParser.START_TAG, null, TAG_PROJECTS);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (TAG_PROJECT.equals(name)) {
                projects.add(readProject(parser));
            } else {
                skip(parser);
            }
        }

        return projects;
    }

    private TaskBundle readProject(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, TAG_PROJECT);
        TaskBundle project;
        List<TaskInfo> taskList = new ArrayList<TaskInfo>();
        int mode = readMode(parser);
        String processName = parser.getAttributeValue(null, ATTRIBUTE_PROCESS_NAME);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (TAG_TASK.equals(name)) {
                taskList.add(readTask(parser));
            } else {
                skip(parser);
            }
        }

        project = new TaskBundle(mode, processName, taskList);
        return project;
    }

    private TaskInfo readTask(XmlPullParser parser) throws  IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, TAG_TASK);
        String name = parser.getAttributeValue(null, ATTRIBUTE_TASK_NAME);
        String path = parser.getAttributeValue(null, ATTRIBUTE_TASK_CLASS);
        String predecessors = parser.getAttributeValue(null, ATTRIBUTE_TASK_PREDECESSOR);
        String threadPriorityStr = parser.getAttributeValue(null, ATTRIBUTE_THREAD_PRIORITY);
        String executePriorityStr = parser.getAttributeValue(null, ATTRIBUTE_EXECUTE_PRIORITY);

        if (TextUtils.isEmpty(name)) {
            throw new RuntimeException("Task name is not set.");
        }

        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("The path of task : " + name + " is not set.");
        }

        TaskInfo info = new TaskInfo(name, path);

        if (!TextUtils.isEmpty(predecessors)) {
            List<String> predecessorList = parsePredecessorId(predecessors);
            info.addPredecessors(predecessorList);
        }

        if (!TextUtils.isEmpty(threadPriorityStr)) {
            info.threadPriority = Integer.parseInt(threadPriorityStr);
        }

        if (!TextUtils.isEmpty(executePriorityStr)) {
            info.executePriority = Integer.parseInt(executePriorityStr);
        }

        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, null, TAG_TASK);
        return info;
    }

    private List<String> parsePredecessorId(String predecessorIds) {
        if (TextUtils.isEmpty(predecessorIds)) {
            return null;
        }

        predecessorIds = predecessorIds.replace(" ", "");
        String[] predecessorArray = TextUtils.split(predecessorIds, PREDECESSOR_DIVIDER);
        return Arrays.asList(predecessorArray);
    }

    private int readMode(XmlPullParser parser) {
        String modeStr = parser.getAttributeValue(null, ATTRIBUTE_PROJECT_MODE);

        if (MODE_ALL_PROCESS.equals(modeStr)) {
            return AlphaManager.ALL_PROCESS_MODE;
        } else if (MODE_MAIN_PROCESS.equals(modeStr)) {
            return AlphaManager.MAIN_PROCESS_MODE;
        } else if (MODE_SECONDARY_PROCESS.equals(modeStr)) {
            return AlphaManager.SECONDARY_PROCESS_MODE;
        } else {
            return AlphaManager.ALL_PROCESS_MODE;
        }
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private ProjectInfo createProject(TaskBundle info) {
        List<TaskInfo> taskInfos = info.taskList;
        HashMap<String, Task> taskMap = new HashMap<String, Task>();

        for (TaskInfo taskInfo : taskInfos) {
            Task task = null;
            try {
                Class<?> c = Class.forName(taskInfo.path);
                task = (Task) c.newInstance();
                task.setName(taskInfo.id);

                if (taskInfo.threadPriority != 0) {
                    task.setThreadPriority(taskInfo.threadPriority);
                }

                if (taskInfo.executePriority != Task.DEFAULT_EXECUTE_PRIORITY) {
                    task.setExecutePriority(taskInfo.executePriority);
                }
            } catch (ClassNotFoundException e) {
                AlphaLog.w(e);
            } catch (InstantiationException e) {
                AlphaLog.w(e);
            } catch (IllegalAccessException e) {
                AlphaLog.w(e);
            }

            if (task == null) {
                throw new RuntimeException("Can not reflect Task: " + taskInfo.path);
            }

            taskMap.put(taskInfo.id, task);
        }

        Project.Builder builder = new Project.Builder();

        for (TaskInfo taskInfo : taskInfos) {
            Task task = taskMap.get(taskInfo.id);
            builder.add(task);
            List<String> predecessorList = taskInfo.predecessorList;

            if (!predecessorList.isEmpty()) {
                for (String predecessorName : predecessorList) {
                    Task t = taskMap.get(predecessorName);

                    if (t == null) {
                        throw new RuntimeException("No such task: " + predecessorName);
                    }

                    builder.after(t);
                }
            }
        }

        ProjectInfo result = new ProjectInfo(builder.create(), info.mode, info.processName);
        return result;
    }

    private static class TaskBundle {
        public List<TaskInfo> taskList = new ArrayList<TaskInfo>();
        public int mode = AlphaManager.ALL_PROCESS_MODE;
        public String processName = "";

        public TaskBundle(int mode, String processName, List<TaskInfo> tasks) {
            this.mode = mode;
            this.processName = processName;
            this.taskList = tasks;
        }
    }

    private static class TaskInfo{
        public String id;
        public String path;
        public List<String> predecessorList = new ArrayList<String>();
        public int threadPriority = 0;
        public int executePriority = Task.DEFAULT_EXECUTE_PRIORITY;

        public TaskInfo(String id, String path) {
            this.id = id;
            this.path = path;
        }

        public void addPredecessors(List<String> predecessors) {
            this.predecessorList.addAll(predecessors);
        }

        public boolean isFirst() {
            return predecessorList.isEmpty();
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TaskInfo ").append("id: ").append(id);
            return builder.toString();
        }
    }

    /**
     * 从XML配置文件中解析出来的某一个{@code Project}的信息，包括具体的{@code Project}对象，即其名称，执行模式。
     */
    public static class ProjectInfo {
        /**
         * {@code Project}对象
         */
        public Task project;
        /**
         * {@code Project}执行模式，对应的模式如下：<br>
         * {@link AlphaManager#ALL_PROCESS_MODE}<br>
         * {@link AlphaManager#MAIN_PROCESS_MODE}<br>
         * {@link AlphaManager#SECONDARY_PROCESS_MODE}<br>
         */
        public int mode = AlphaManager.ALL_PROCESS_MODE;
        /**
         * {@code Project}的名称
         */
        public String processName;

        public ProjectInfo(Task project, int mode, String processName) {
            this.project = project;
            this.mode = mode;
            this.processName = processName;
        }
    }

}
