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

package cn.hippo4j.core.plugin.manager;

import cn.hippo4j.core.plugin.ThreadPoolPlugin;
import cn.hippo4j.core.plugin.impl.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Register default {@link ThreadPoolPlugin}.
 *
 * @see TaskDecoratorPlugin
 * @see TaskTimeoutNotifyAlarmPlugin
 * @see TaskRejectCountRecordPlugin
 * @see TaskRejectNotifyAlarmPlugin
 * @see ThreadPoolExecutorShutdownPlugin
 */
@NoArgsConstructor
@AllArgsConstructor
public class DefaultThreadPoolPluginRegistrar implements ThreadPoolPluginRegistrar {

    public static final String REGISTRAR_NAME = "DefaultThreadPoolPluginRegistrar";

    /**
     * execute time out
     */
    private long executeTimeOut;

    /**
     * await termination millis
     */
    private long awaitTerminationMillis;

    /**
     * Get id.
     *
     * @return id
     */
    @Override
    public String getId() {
        return REGISTRAR_NAME;
    }

    /**
     * Create and register plugin for the specified thread-pool instance.
     *
     * @param support thread pool plugin manager delegate
     */
    @Override
    public void doRegister(ThreadPoolPluginSupport support) {
        // callback when task execute
        support.register(new TaskDecoratorPlugin());
        support.register(new TaskTimeoutNotifyAlarmPlugin(support.getThreadPoolId(), executeTimeOut, support.getThreadPoolExecutor()));
        // callback when task rejected
        support.register(new TaskRejectCountRecordPlugin());
        support.register(new TaskRejectNotifyAlarmPlugin());
        // callback when pool shutdown
        support.register(new ThreadPoolExecutorShutdownPlugin(awaitTerminationMillis));
    }

}
