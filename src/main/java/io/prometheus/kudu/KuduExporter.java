package io.prometheus.kudu;

import io.prometheus.kudu.config.KuduExporterConfiguration;
import io.prometheus.kudu.sink.KuduMetricPool;
import io.prometheus.kudu.task.KuduMetricFetcherRunner;
import io.prometheus.kudu.task.KuduMetricReporterRunner;
import io.prometheus.kudu.util.ArgsEntity;
import io.prometheus.kudu.util.LoggerUtils;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineParser;

import java.util.List;
import java.util.Map;

public class KuduExporter {

    private static final Logger logger = LoggerUtils.Logger();

    public static void main(String[] args) {
        try {
            // Parser the parameters into ArgsEntity
            ArgsEntity argsEntity = new ArgsEntity() {{
                new CmdLineParser(this).parseArgument(args);
            }};

            // Build configuration and metric pool
            KuduExporterConfiguration configuration = KuduExporterConfiguration.getConfiguration(argsEntity);
            KuduMetricPool<List<Map<?, ?>>> metricsPool = KuduMetricPool.build();

            // Start fetcher jobs and the reporter job by custom configuration
            KuduMetricFetcherRunner.run(configuration, metricsPool);
            KuduMetricReporterRunner.run(configuration, metricsPool);

            logger.info("Prometheus-Kudu-Exporter in Running.");

        } catch (Exception e) {
            logger.warn(String.format("Running with Exception: %s", e.getCause()));
        }
    }

}