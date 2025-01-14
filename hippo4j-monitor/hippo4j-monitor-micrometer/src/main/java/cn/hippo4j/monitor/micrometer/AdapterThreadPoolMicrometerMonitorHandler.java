/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.hippo4j.monitor.micrometer;

import cn.hippo4j.adapter.base.ThreadPoolAdapterState;
import cn.hippo4j.common.config.ApplicationContextHolder;
import cn.hippo4j.common.toolkit.BeanUtil;
import cn.hippo4j.common.toolkit.CollectionUtil;
import cn.hippo4j.monitor.base.AbstractAdapterThreadPoolMonitor;
import cn.hippo4j.monitor.base.MonitorTypeEnum;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter thread-pool micrometer monitor handler.
 */
public class AdapterThreadPoolMicrometerMonitorHandler extends AbstractAdapterThreadPoolMonitor {

    private final static String METRIC_NAME_PREFIX = "adapter.thread-pool";

    private final static String ADAPTER_THREAD_POOL_ID_TAG = METRIC_NAME_PREFIX + ".id";

    private final static String APPLICATION_NAME_TAG = "application.name";

    private final Map<String, ThreadPoolAdapterState> RUN_STATE_CACHE = new ConcurrentHashMap<>();

    @Override
    protected void execute(ThreadPoolAdapterState threadPoolAdapterState) {
        ThreadPoolAdapterState stateInfo = RUN_STATE_CACHE.get(threadPoolAdapterState.getThreadPoolKey());
        if (stateInfo == null) {
            RUN_STATE_CACHE.put(threadPoolAdapterState.getThreadPoolKey(), threadPoolAdapterState);
        } else {
            BeanUtil.convert(threadPoolAdapterState, stateInfo);
        }
        Environment environment = ApplicationContextHolder.getInstance().getEnvironment();
        String applicationName = environment.getProperty("spring.application.name", "application");
        Iterable<Tag> tags = CollectionUtil.newArrayList(
                Tag.of(ADAPTER_THREAD_POOL_ID_TAG, threadPoolAdapterState.getThreadPoolKey()),
                Tag.of(APPLICATION_NAME_TAG, applicationName));
        Metrics.gauge(metricName("core.size"), tags, threadPoolAdapterState, ThreadPoolAdapterState::getCoreSize);
        Metrics.gauge(metricName("maximum.size"), tags, threadPoolAdapterState, ThreadPoolAdapterState::getMaximumSize);
        Metrics.gauge(metricName("queue.capacity"), tags, threadPoolAdapterState, ThreadPoolAdapterState::getBlockingQueueCapacity);
    }

    private String metricName(String name) {
        return String.join(".", METRIC_NAME_PREFIX, name);
    }

    @Override
    public String getType() {
        return MonitorTypeEnum.MICROMETER.name().toLowerCase();
    }
}
