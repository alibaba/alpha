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

import java.util.Map;

/**
 * <p>获取{@code Project}执行性能记录的回调</p>
 * Created by zhangshuliang.zsl on 15/11/4.
 */
public interface OnGetMonitorRecordCallback {

    /**
     * 获取{@code task}执行的耗时。
     * @param result {@code task}执行的耗时。{@code key}是{@code task}名称，{@code value}是{@code task}执行耗时，单位是毫秒。
     */
    public void onGetTaskExecuteRecord(Map<String, Long> result);

    /**
     * 获取整个{@code Project}执行耗时。
     * @param costTime 整个{@code Project}执行耗时。
     */
    public void onGetProjectExecuteTime(long costTime);
}
