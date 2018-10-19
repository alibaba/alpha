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

/**
 * Created by shangjie on 2018/10/19.
 */

public interface ITaskCreator {
    /**
     * 根据Task名称，创建Task实例。这个接口需要使用者自己实现。创建后的实例会被缓存起来。
     * @param taskName Task名称
     * @return  Task实例
     */
    public Task createTask(String taskName);
}
