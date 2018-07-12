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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>在编码过程中，我们通常会有这样的需要{@code Map<Key, List<Value>>}这样的数据结构，{@code ListMultiMap}
 * 就是对这类结构的封装，简化对它的使用。</p>
 * <p><strong>注意：</strong>目前{@code ListMultiMap}不是一个{@code Map}类型。</p>
 * Created by zhangshuliang.zsl on 15/9/30.
 */
public class ListMultiMap<K, V> {
    private HashMap<K, List<V>> mInnerMap = new HashMap<K, List<V>>();
    private int mSize = 0;

    /**
     * 清除结构中的所有元素
     */
    public void clear() {
        mInnerMap.clear();
        mSize = 0;
    }

    /**
     * 判断是否包含某一个{@code key}
     *
     * @param key 需要判断的{@code key}
     * @return {@code true}如果{@code key}存在，否则返回{@code false}。
     */
    public boolean containsKey(K key) {
        return mInnerMap.containsKey(key);
    }

    /**
     * 判断是否包含某一个{@code value}
     *
     * @param value 需要判断的{@code value}
     * @return {@code true}如果{@code value}存在，否则表示不存在。
     */
    public boolean containsValue(V value) {
        for (Map.Entry<K, List<V>> entry : mInnerMap.entrySet()) {
            List<V> values = entry.getValue();

            if (values != null && values.contains(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否包含某一对key-value。
     *
     * @param key   需要判断的{@code key}
     * @param value 需要判断的{@code value}
     * @return {@code true}如果{@code value}存在，否则表示不存在。
     */
    public boolean contains(K key, V value) {
        if (!containsKey(key)) {
            return false;
        }

        List<V> list = get(key);

        if (list == null || list.isEmpty()) {
            return false;
        }

        return list.contains(value);
    }

    /**
     * 获取符合该{@code key}的所有{@code value}。
     *
     * @param key 指定的{@code key}。
     * @return 符合该{@code key}的所有{@code value}。
     */
    public List<V> get(K key) {
        return mInnerMap.get(key);
    }


    /**
     * 判断数据结构是否元素为空。
     *
     * @return {@code true}元素是空的，否则返回{@code false}。
     */
    public boolean isEmpty() {
        return mSize <= 0;
    }


    /**
     * 将一对K-V值插入到数据结构中。
     *
     * @param key   key
     * @param value value
     */
    public void put(K key, V value) {
        if (mInnerMap.containsKey(key)) {
            List<V> list = mInnerMap.get(key);
            list.add(value);
        } else {
            List<V> list = new ArrayList<V>();
            list.add(value);
            mInnerMap.put(key, list);
        }

        mSize++;
    }


    /**
     * 移除该{@code key}所对应的所有{@code value}
     *
     * @param key 要删除的key
     * @return 返回删除的元素列表，若没有该key对应的元素，则返回null。
     */
    public List<V> remove(K key) {
        List<V> list = mInnerMap.remove(key);

        if (list != null && !list.isEmpty()) {
            mSize -= list.size();
        }

        return list;
    }

    /**
     * 移除该{@code key}所对应的{@code list}中的{@code value}
     *
     * @param key   要删除的key
     * @param value 要删除的{@code value}
     * @return 被删除的value，不包含该value，则返回null。
     */
    public V remove(K key, V value) {
        List<V> list = mInnerMap.get(key);
        V ret = null;

        if (list != null && !list.isEmpty()) {
            boolean isRemoved = list.remove(value);

            if (isRemoved) {
                mSize--;
                ret = value;
            }
        }

        return ret;
    }


    /**
     * 从所有{@code List<V>}中删除指定{@code value}。
     *
     * @param value 要删除的{@code value}
     * @return 被删除的value，不包含该value，则返回null。
     */
    public V removeValue(V value) {
        boolean contains = false;

        for (Map.Entry<K, List<V>> entry : mInnerMap.entrySet()) {
            List<V> values = entry.getValue();

            if (values != null && values.contains(value)) {
                values.remove(value);
                contains = true;
                mSize--;
            }
        }

        return contains ? value : null;
    }


    /**
     * @return 数据结构中总共的{@code value}数。
     */
    public int size() {
        return mSize;
    }

    /**
     * @return 将内容转换成字符串返回。
     */
    public String toString() {
        return mInnerMap.toString();
    }

}
