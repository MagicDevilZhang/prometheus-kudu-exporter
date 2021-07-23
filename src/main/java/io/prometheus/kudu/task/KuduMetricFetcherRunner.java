/*
 * Copyright RyanCheung98@163.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.kudu.task;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricPool;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetcher runner as multi-threads
 */
public class KuduMetricFetcherRunner implements Runnable {
    private static final Logger logger = LoggerUtils.Logger();

    private final KuduExporterConfiguration configuration;
    private final KuduMetricPool<List<Map<?, ?>>> metricPool;

    /**
     * Private Constructor to init the runner
     *
     * @param configuration exporter configuration
     * @param metricPool    Concurrent-limited pool to store metric resources
     */
    private KuduMetricFetcherRunner(
            KuduExporterConfiguration configuration,
            KuduMetricPool<List<Map<?, ?>>> metricPool) {
        this.configuration = configuration;
        this.metricPool = metricPool;
    }

    /**
     * Build instance and run as a thread
     *
     * @param configuration exporter configuration
     * @param metricPool    Concurrent-limited pool to store metric resources
     */
    public static void run(
            KuduExporterConfiguration configuration,
            KuduMetricPool<List<Map<?, ?>>> metricPool) {
        Thread thread = new Thread(new KuduMetricFetcherRunner(configuration, metricPool));
        thread.start();
    }

    /**
     * Override the abstract function of Runnable to run fetcher task
     */
    @Override
    public void run() {
        try {
            Constructor<? extends KuduExporterTask> constructor = Class
                    .forName(configuration.getFetcherClassName())
                    .asSubclass(KuduExporterTask.class)
                    .getConstructor(Integer.class, KuduExporterConfiguration.class, KuduMetricPool.class);
            ExecutorService threadPool = Executors.newWorkStealingPool();
            while (true) {
                for (int i = configuration.getFetcherKuduNodes().size() - 1; i >= 0; i--) {
                    threadPool.submit(constructor.newInstance(i, this.configuration, this.metricPool));
                }
                Thread.sleep(configuration.getFetcherInterval());
            }
        } catch (ClassNotFoundException e) {
            logger.error("fetcher class not be found.", e);
        } catch (InterruptedException e) {
            logger.error("fetcher threads running fail.", e);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            logger.error("fetcher inner fatal error for invocation target or method change.", e);
        }
    }

}
