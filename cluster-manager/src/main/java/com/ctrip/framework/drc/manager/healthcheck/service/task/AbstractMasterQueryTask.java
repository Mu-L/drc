package com.ctrip.framework.drc.manager.healthcheck.service.task;

import com.ctrip.framework.drc.core.driver.healthcheck.task.AbstractQueryTask;
import com.ctrip.xpipe.api.endpoint.Endpoint;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;
import org.apache.tomcat.jdbc.pool.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.ctrip.framework.drc.core.driver.util.MySQLConstants.IS_READ_ONLY_COMMAND;

/**
 * Created by mingdongli
 * 2019/11/22 上午9:38.
 */
public abstract class AbstractMasterQueryTask<V> extends AbstractQueryTask<V> {

    protected static final String ACCESS_DENIED = "Access denied";

    public AbstractMasterQueryTask(Endpoint master) {
        super(master);
    }

    @SuppressWarnings("findbugs:RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    protected boolean isMaster(Endpoint endpoint) {
        DataSource dataSource = dataSourceManager.getDataSource(endpoint);
        try (final Connection connection = dataSource.getConnection()) {
            final Statement statement = connection.createStatement();

            // read_only
            final ResultSet readOnlyResultSet = statement.executeQuery(IS_READ_ONLY_COMMAND);
            String isReadOnly = "";
            if (readOnlyResultSet.next()) {
                isReadOnly = readOnlyResultSet.getString(1);
            }

            // read_only = 0,那么此节点即为Master
            boolean isMaster = "0".equalsIgnoreCase(isReadOnly);

            readOnlyResultSet.close();
            statement.close();

            logger.info("query successfully of {}:{}, isMaster = {}", endpoint.getHost(), endpoint.getPort(), isMaster);
            return isMaster;
        } catch (MySQLSyntaxErrorException e) {
            dataSourceManager.clearDataSource(endpoint);
            logger.warn("query master of {}:{} error and clear from dataSourceManager", endpoint.getHost(), endpoint.getPort(), e);
            return true;
        }  catch (SQLException e) {
            dataSourceManager.clearDataSource(endpoint);
            String message = e.getMessage();
            logger.error("query master of {}:{} error and clear from dataSourceManager. exception: {}", endpoint.getHost(), endpoint.getPort(), message);
            return message != null && message.contains(ACCESS_DENIED);
        } catch (Exception e) {
            logger.error("query master of {}:{} error", endpoint.getHost(), endpoint.getPort(), e);
            return false;
        }
    }
}
