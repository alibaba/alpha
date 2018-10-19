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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shangjie on 2018/10/19.
 */

public final class TaskFactory {
    private Map<String, Task> mTasks = new HashMap<>();
    private ITaskCreator mTaskCreator;

    public TaskFactory(ITaskCreator creator) {
        mTaskCreator = creator;
    }

    public synchronized Task getTask(String taskName) {
        Task task = mTasks.get(taskName);

        if (task != null) {
            return task;
        }

        task = mTaskCreator.createTask(taskName);

        if (task == null) {
            throw new IllegalArgumentException("Create task fail, there is no task corresponding to the task name. Make sure you have create a task instance in TaskCreator.");
        }

        return task;
    }
}
