package com.ctrip.framework.drc.applier.resource.mysql;

import com.ctrip.framework.drc.applier.activity.monitor.MetricsActivity;
import com.ctrip.framework.drc.core.driver.pool.DrcDataSourceValidator;
import com.ctrip.framework.drc.core.driver.pool.DrcTomcatDataSource;
import com.ctrip.framework.drc.core.monitor.reporter.DefaultTransactionMonitorHolder;
import com.ctrip.framework.drc.core.server.utils.ThreadUtils;
import com.ctrip.framework.drc.fetcher.resource.thread.Executor;
import com.ctrip.framework.drc.fetcher.system.AbstractResource;
import com.ctrip.framework.drc.fetcher.system.InstanceActivity;
import com.ctrip.framework.drc.fetcher.system.InstanceConfig;
import com.ctrip.framework.drc.fetcher.system.InstanceResource;
import com.ctrip.xpipe.api.monitor.Task;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.ctrip.framework.drc.applier.server.ApplierServerInCluster.DEFAULT_APPLY_COUNT;
import static com.ctrip.framework.drc.core.monitor.datasource.AbstractDataSource.setCommonProperty;
import static com.ctrip.framework.drc.core.server.config.SystemConfig.CONNECTION_TIMEOUT;
import static com.ctrip.framework.drc.core.server.config.SystemConfig.HEARTBEAT_LOGGER;

/**
 * @Author Slight
 * May 14, 2020
 */
public class DataSourceResource extends AbstractResource implements DataSource {

    @InstanceActivity
    public MetricsActivity reporter;

    @InstanceConfig(path = "target.ip")
    public String ip = "";

    @InstanceConfig(path = "target.URL")
    public String URL;

    @InstanceConfig(path = "target.username")
    public String username;

    @InstanceConfig(path = "target.password")
    public String password;

    @InstanceResource
    public Executor executor;

    @InstanceConfig(path = "applyConcurrency")
    public int applyConcurrency = DEFAULT_APPLY_COUNT;

    private int initialPoolSize = 1;

    public int validationInterval = 30000;

    @InstanceConfig(path = "registryKey")
    public String registryKey = "unset";

    private PoolProperties properties;

    private javax.sql.DataSource inner;

    private static final ScheduledExecutorService scheduledExecutorService = ThreadUtils.newFixedThreadScheduledPool(5, "DatasourceResourceInfo");

    private ScheduledFuture<?> jdbcActiveFuture;
    private ScheduledFuture<?> heartBeatLogFuture;
    @Override
    protected void doInitialize() throws Exception {
        properties = new PoolProperties();
        properties.setName(registryKey);
        properties.setUrl(URL);
        properties.setUsername(username);
        properties.setPassword(password);
        properties.setDefaultAutoCommit(false);
        String timeout = String.format("connectTimeout=%s;socketTimeout=60000", CONNECTION_TIMEOUT);
        properties.setConnectionProperties(timeout);

        properties.setValidationInterval(validationInterval);
        setCommonProperty(properties);

        properties.setMaxActive(applyConcurrency);
        properties.setMaxIdle(applyConcurrency);
        properties.setInitialSize(initialPoolSize);
        properties.setMinIdle(applyConcurrency);
        properties.setValidator(new DrcDataSourceValidator(properties));

        inner = new DrcTomcatDataSource(properties);

        logger.info("[INIT DataSource] {}, applyConcurrency: {}", URL, applyConcurrency);

        executor.execute(()-> {
            warmUp();
        });

        jdbcActiveFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!Thread.currentThread().isInterrupted()) {
                int active = ((DrcTomcatDataSource) inner).getActive();
                if (reporter != null) {
                    reporter.report("jdbc.active", null, active);
                }
            }
        }, 1, 2, TimeUnit.SECONDS);

        heartBeatLogFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                HEARTBEAT_LOGGER.info("{} - HOST - {}", registryKey, InetAddress.getByName(ip));
            } catch (UnknownHostException e) {
                HEARTBEAT_LOGGER.error("{} - HOST ERROR - {}", registryKey, ip);
            }
        }, 60, 10, TimeUnit.SECONDS);
    }

    @Override
    protected synchronized void doDispose() {
        if (jdbcActiveFuture != null) {
            jdbcActiveFuture.cancel(true);
        }
        if (heartBeatLogFuture != null) {
            heartBeatLogFuture.cancel(true);
        }
        if (inner != null) {
            DataSourceTerminator.getInstance().close((DrcTomcatDataSource) inner);
        }
    }

    public DataSource wrap(javax.sql.DataSource dataSource) {
        this.inner = dataSource;
        return this;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return inner.getConnection();
    }

    private void warmUp() {
        try {
            DefaultTransactionMonitorHolder.getInstance().logTransaction("DRC.applier.connection.create", registryKey, new Task() {
                @Override
                public void go() throws SQLException {
                    logger.info("[Init] connection for {}:{} begin", registryKey, URL);
                    Connection connection = getConnection();
                    if (connection != null) {
                        connection.close();
                    }
                    logger.info("[Init] connection for {}:{} end", registryKey, URL);
                }
            });
        } catch (Exception e) {
            logger.error("[Init] connection for {}:{} error", registryKey, URL);
        }
    }
}
