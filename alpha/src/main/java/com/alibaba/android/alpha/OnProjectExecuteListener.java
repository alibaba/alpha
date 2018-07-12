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
 * <p>
 * {@code Project}执行生命周期的回调。<br>
 * <strong>注意：</strong>回调接口要考虑线程安全问题。
 * </p>
 *
 * Created by zhangshuliang.zsl on 15/9/30.
 */
public interface OnProjectExecuteListener {

    /**
     * 当{@code Project}开始执行时，调用该函数。<br>
     * <strong>注意：</strong>该回调函数在{@code Task}所在线程中回调，注意线程安全。
     */
    public void onProjectStart();


    /**
     * 当{@code Project}其中一个{@code Task}执行结束时，调用该函数。<br>
     * <strong>注意：</strong>该回调函数在{@code Task}所在线程中回调，注意线程安全。
     *
     * @param taskName 当前结束的{@code Task}名称
     */
    public void onTaskFinish(String taskName);

    /**
     * 当{@code Project}执行结束时，调用该函数。<br>
     * <strong>注意：</strong>该回调函数在{@code Task}所在线程中回调，注意线程安全。
     */
    public void onProjectFinish();
}
