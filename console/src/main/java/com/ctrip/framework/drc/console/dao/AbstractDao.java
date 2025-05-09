package com.ctrip.framework.drc.console.dao;

import com.ctrip.framework.drc.console.config.DefaultConsoleConfig;
import com.ctrip.framework.drc.console.enums.BooleanEnum;
import com.ctrip.platform.dal.dao.DalHints;
import com.ctrip.platform.dal.dao.KeyHolder;
import com.ctrip.platform.dal.dao.base.SQLResultSpec;
import com.ctrip.platform.dal.dao.helper.DalDefaultJpaParser;
import com.ctrip.platform.dal.dao.sqlbuilder.SelectSqlBuilder;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by jixinwang on 2020/6/22
 */
public class AbstractDao<T> {

    private static final boolean ASC = true;
    protected final BaseDalTableDao<T> client;

    protected static final String ID = "id";

    public AbstractDao(Class<T> clazz) throws SQLException {
        DefaultConsoleConfig config = DefaultConsoleConfig.getInstance();
        String region = config.getRegion();
        Set<String> publicCloudRegion = config.getPublicCloudRegion();
        if (publicCloudRegion.contains(region)) {
            this.client = new OverseaDalTableDao<>(clazz);
            return;
        }
        this.client = new BaseDalTableDao<>(new DalDefaultJpaParser<>(clazz));
    }

    public SelectSqlBuilder initSqlBuilder() throws SQLException {
        SelectSqlBuilder sqlBuilder = new SelectSqlBuilder();
        sqlBuilder.selectAll().equal("deleted", BooleanEnum.FALSE.getCode(), Types.TINYINT);
        return sqlBuilder;
    }

    public <K> List<K> query(String sql, DalHints dalHints, SQLResultSpec result, Object... args) throws SQLException {
        return client.query(sql, dalHints, result, args);
    }

    public <K> K queryObject(String sql, DalHints dalHints, SQLResultSpec result, Object... args) throws SQLException {
        return client.queryObject(sql, dalHints, result, args);
    }

    public List<T> queryByPk(List<Long> ids) throws SQLException {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        SelectSqlBuilder sqlBuilder = new SelectSqlBuilder();
        sqlBuilder.and().in("id", ids, Types.BIGINT);
        return queryList(sqlBuilder);
    }

    public List<T> queryByIds(List<Long> ids) throws SQLException {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        SelectSqlBuilder sqlBuilder = initSqlBuilder();
        sqlBuilder.and().in("id", ids, Types.BIGINT);
        return queryList(sqlBuilder);
    }

    /**
     * Insert T with return ID
     */
    public Long insertWithReturnId(T daoPojo) throws SQLException {
        KeyHolder keyHolder = new KeyHolder();
        insert(new DalHints(), keyHolder, daoPojo);
        return (Long) keyHolder.getKey();
    }

    /**
     * Query by ID
     * Deleted default false
     */
    public T queryById(Long id) throws SQLException {
        SelectSqlBuilder sqlBuilder = initSqlBuilder();
        sqlBuilder.and().equal("id", id, Types.BIGINT);
        return client.queryFirst(sqlBuilder, new DalHints());
    }

    /**
     * Query one
     */
    public T queryOne(SelectSqlBuilder sqlBuilder) throws SQLException {
        return client.queryFirst(sqlBuilder, new DalHints());
    }

    /**
     * Query list
     */
    public List<T> queryList(SelectSqlBuilder sqlBuilder) throws SQLException {
        return client.query(sqlBuilder, new DalHints());
    }

    /**
     * Query T by the specified ID
     * The ID must be a number
     */
    public T queryByPk(Number id)
            throws SQLException {
        return queryByPk(id, null);
    }

    /**
     * Query T by the specified ID
     * The ID must be a number
     */
    public T queryByPk(Number id, DalHints hints)
            throws SQLException {
        hints = DalHints.createIfAbsent(hints);
        return client.queryByPk(id, hints);
    }

    /**
     * Query T by T instance which the primary key is set
     */
    public T queryByPk(T pk)
            throws SQLException {
        return queryByPk(pk, null);
    }

    /**
     * Query T by T instance which the primary key is set
     */
    public T queryByPk(T pk, DalHints hints)
            throws SQLException {
        hints = DalHints.createIfAbsent(hints);
        return client.queryByPk(pk, hints);
    }

    /**
     * Query against sample pojo. All not null attributes of the passed in pojo
     * will be used as search criteria.
     */
    public List<T> queryBy(T sample)
            throws SQLException {
        return queryBy(sample, null);
    }

    /**
     * Query against sample pojo. All not null attributes of the passed in pojo
     * will be used as search criteria.
     */
    public List<T> queryBy(T sample, DalHints hints)
            throws SQLException {
        hints = DalHints.createIfAbsent(hints);
        return client.queryBy(sample, hints);
    }

    /**
     * Get the all records count
     */
    public int count() throws SQLException {
        return count(null);
    }

    /**
     * Get the all records count
     */
    public int count(DalHints hints) throws SQLException {
        hints = DalHints.createIfAbsent(hints);
        SelectSqlBuilder builder = new SelectSqlBuilder().selectCount();
        return client.count(builder, hints).intValue();
    }

    /**
     * Query T with paging function
     * The pageSize and pageNo must be greater than zero.
     */
    public List<T> queryAllByPage(int pageNo, int pageSize) throws SQLException {
        return queryAllByPage(pageNo, pageSize, null);
    }

    /**
     * Query T with paging function
     * The pageSize and pageNo must be greater than zero.
     */
    public List<T> queryAllByPage(int pageNo, int pageSize, DalHints hints) throws SQLException {
        hints = DalHints.createIfAbsent(hints);

        SelectSqlBuilder builder = new SelectSqlBuilder();
        builder.selectAll().atPage(pageNo, pageSize).orderBy("id", ASC);

        return client.query(builder, hints);
    }

    /**
     * Get all records from table
     */
    public List<T> queryAll() throws SQLException {
        return queryAll(null);
    }

    /**
     * Get all records from table
     */
    public List<T> queryAll(DalHints hints) throws SQLException {
        hints = DalHints.createIfAbsent(hints);

        SelectSqlBuilder builder = new SelectSqlBuilder().selectAll().orderBy("id", ASC);

        return client.query(builder, hints);
    }


    public final List<T> queryAllExist() throws SQLException {
        SelectSqlBuilder builder = initSqlBuilder();
        return client.query(builder, new DalHints());
    }

    public List<T> queryBySQL(String sql) throws SQLException {
        return client.query(sql, new DalHints());
    }

    /**
     * Insert single pojo
     *
     * @param daoPojo pojo to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int insert(T daoPojo) throws SQLException {
        return insert(null, daoPojo);
    }

    /**
     * Insert single pojo
     *
     * @param hints   Additional parameters that instruct how DAL Client perform database operation.
     * @param daoPojo pojo to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int insert(DalHints hints, T daoPojo) throws SQLException {
        if (null == daoPojo) {
            return 0;
        }
        hints = DalHints.createIfAbsent(hints);
        return client.insert(hints, daoPojo);
    }

    /**
     * Insert pojos one by one. If you want to inert them in the batch mode,
     * user batchInsert instead. You can also use the combinedInsert.
     *
     * @param daoPojos list of pojos to be inserted
     * @return how many rows been affected
     */
    public int[] insert(List<T> daoPojos) throws SQLException {
        return insert(null, daoPojos);
    }

    /**
     * Insert pojos one by one. If you want to inert them in the batch mode,
     * user batchInsert instead. You can also use the combinedInsert.
     *
     * @param hints    Additional parameters that instruct how DAL Client perform database operation.
     *                 DalHintEnum.continueOnError can be used
     *                 to indicate that the inserting can be go on if there is any
     *                 failure.
     * @param daoPojos list of pojos to be inserted
     * @return how many rows been affected
     */
    public int[] insert(DalHints hints, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return new int[0];
        }
        hints = DalHints.createIfAbsent(hints);
        return client.insert(hints, daoPojos);
    }

    /**
     * Insert pojo and get the generated PK back in keyHolder.
     * If the "set no count on" for MS SqlServer is set, the operation may fail.
     * Please don't pass keyholder for MS SqlServer to avoid the failure in such case.
     *
     * @param keyHolder holder for generated primary keys
     * @param daoPojo   pojo to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int insertWithKeyHolder(KeyHolder keyHolder, T daoPojo) throws SQLException {
        return insert(null, keyHolder, daoPojo);
    }

    /**
     * Insert pojo and get the generated PK back in keyHolder.
     * If the "set no count on" for MS SqlServer is set, the operation may fail.
     * Please don't pass keyholder for MS SqlServer to avoid the failure in such case.
     *
     * @param hints     Additional parameters that instruct how DAL Client perform database operation.
     * @param keyHolder holder for generated primary keys
     * @param daoPojo   pojo to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int insert(DalHints hints, KeyHolder keyHolder, T daoPojo) throws SQLException {
        if (null == daoPojo) {
            return 0;
        }
        hints = DalHints.createIfAbsent(hints);
        return client.insert(hints, keyHolder, daoPojo);
    }

    /**
     * Insert pojos and get the generated PK back in keyHolder.
     * If the "set no count on" for MS SqlServer is set, the operation may fail.
     * Please don't pass keyholder for MS SqlServer to avoid the failure in such case.
     *
     * @param keyHolder holder for generated primary keys
     * @param daoPojos  list of pojos to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] insertWithKeyHolder(KeyHolder keyHolder, List<T> daoPojos) throws SQLException {
        return insert(null, keyHolder, daoPojos);
    }

    /**
     * Insert pojos and get the generated PK back in keyHolder.
     * If the "set no count on" for MS SqlServer is set, the operation may fail.
     * Please don't pass keyholder for MS SqlServer to avoid the failure in such case.
     *
     * @param hints     Additional parameters that instruct how DAL Client perform database operation.
     *                  DalHintEnum.continueOnError can be used
     *                  to indicate that the inserting can be go on if there is any
     *                  failure.
     * @param keyHolder holder for generated primary keys
     * @param daoPojos  list of pojos to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] insert(DalHints hints, KeyHolder keyHolder, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return new int[0];
        }
        hints = DalHints.createIfAbsent(hints);
        return client.insert(hints, keyHolder, daoPojos);
    }

    /**
     * Insert pojos in batch mode.
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param daoPojos list of pojos to be inserted
     * @return how many rows been affected for inserting each of the pojo
     * @throws SQLException
     */
    public int[] batchInsert(List<T> daoPojos) throws SQLException {
        return batchInsert(null, daoPojos);
    }

    /**
     * Insert pojos in batch mode.
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param hints    Additional parameters that instruct how DAL Client perform database operation.
     * @param daoPojos list of pojos to be inserted
     * @return how many rows been affected for inserting each of the pojo
     * @throws SQLException
     */
    public int[] batchInsert(DalHints hints, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return new int[0];
        }
        hints = DalHints.createIfAbsent(hints);
        return client.batchInsert(hints, daoPojos);
    }

    /**
     * Insert multiple pojos in one INSERT SQL
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param daoPojos list of pojos to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int combinedInsert(List<T> daoPojos) throws SQLException {
        return combinedInsert(null, daoPojos);
    }

    /**
     * Insert multiple pojos in one INSERT SQL
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param hints    Additional parameters that instruct how DAL Client perform database operation.
     * @param daoPojos list of pojos to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int combinedInsert(DalHints hints, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return 0;
        }
        hints = DalHints.createIfAbsent(hints);
        return client.combinedInsert(hints, daoPojos);
    }

    /**
     * Insert multiple pojos in one INSERT SQL and get the generated PK back in keyHolder.
     * If the "set no count on" for MS SqlServer is set, the operation may fail.
     * Please don't pass keyholder for MS SqlServer to avoid the failure in such case.
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param keyHolder holder for generated primary keys
     * @param daoPojos  list of pojos to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int combinedInsertWithKeyHolder(KeyHolder keyHolder, List<T> daoPojos) throws SQLException {
        return combinedInsert(null, keyHolder, daoPojos);
    }

    /**
     * Insert multiple pojos in one INSERT SQL and get the generated PK back in keyHolder.
     * If the "set no count on" for MS SqlServer is set, the operation may fail.
     * Please don't pass keyholder for MS SqlServer to avoid the failure in such case.
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param hints     Additional parameters that instruct how DAL Client perform database operation.
     * @param keyHolder holder for generated primary keys
     * @param daoPojos  list of pojos to be inserted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int combinedInsert(DalHints hints, KeyHolder keyHolder, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return 0;
        }
        hints = DalHints.createIfAbsent(hints);
        return client.combinedInsert(hints, keyHolder, daoPojos);
    }

    /**
     * Delete the given pojo.
     *
     * @param daoPojo pojo to be deleted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int delete(T daoPojo) throws SQLException {
        return delete(null, daoPojo);
    }

    /**
     * Delete the given pojo.
     *
     * @param hints   Additional parameters that instruct how DAL Client perform database operation.
     * @param daoPojo pojo to be deleted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int delete(DalHints hints, T daoPojo) throws SQLException {
        if (null == daoPojo) {
            return 0;
        }
        hints = DalHints.createIfAbsent(hints);
        return client.delete(hints, daoPojo);
    }

    /**
     * Delete the given pojos list one by one.
     *
     * @param daoPojos list of pojos to be deleted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] delete(List<T> daoPojos) throws SQLException {
        return delete(null, daoPojos);
    }

    /**
     * Delete the given pojos list one by one.
     *
     * @param hints    Additional parameters that instruct how DAL Client perform database operation.
     * @param daoPojos list of pojos to be deleted
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] delete(DalHints hints, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return new int[0];
        }
        hints = DalHints.createIfAbsent(hints);
        return client.delete(hints, daoPojos);
    }

    /**
     * Delete the given pojo list in batch.
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param daoPojos list of pojos to be deleted
     * @return how many rows been affected for deleting each of the pojo
     * @throws SQLException
     */
    public int[] batchDelete(List<T> daoPojos) throws SQLException {
        return batchDelete(null, daoPojos);
    }

    /**
     * Delete the given pojo list in batch.
     * The DalDetailResults will be set in hints to allow client know how the operation performed in each of the shard.
     *
     * @param hints    Additional parameters that instruct how DAL Client perform database operation.
     * @param daoPojos list of pojos to be deleted
     * @return how many rows been affected for deleting each of the pojo
     * @throws SQLException
     */
    public int[] batchDelete(DalHints hints, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return new int[0];
        }
        hints = DalHints.createIfAbsent(hints);
        return client.batchDelete(hints, daoPojos);
    }

    /**
     * Update the given pojo . By default, if a field of pojo is null value,
     * that field will be ignored, so that it will not be updated. You can
     * overwrite this by set updateNullField in hints.
     *
     * @param daoPojo pojo to be updated
     * @return how many rows been affected
     * @throws SQLException
     */
    public int update(T daoPojo) throws SQLException {
        return update(null, daoPojo);
    }

    /**
     * Update the given pojo . By default, if a field of pojo is null value,
     * that field will be ignored, so that it will not be updated. You can
     * overwrite this by set updateNullField in hints.
     *
     * @param hints   Additional parameters that instruct how DAL Client perform database operation.
     *                DalHintEnum.updateNullField can be used
     *                to indicate that the field of pojo is null value will be update.
     * @param daoPojo pojo to be updated
     * @return how many rows been affected
     * @throws SQLException
     */
    public int update(DalHints hints, T daoPojo) throws SQLException {
        if (null == daoPojo) {
            return 0;
        }
        hints = DalHints.createIfAbsent(hints);
        return client.update(hints, daoPojo);
    }

    /**
     * Update the given pojo list one by one. By default, if a field of pojo is null value,
     * that field will be ignored, so that it will not be updated. You can
     * overwrite this by set updateNullField in hints.
     *
     * @param daoPojos list of pojos to be updated
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] update(List<T> daoPojos) throws SQLException {
        return update(null, daoPojos);
    }

    /**
     * Update the given pojo list one by one. By default, if a field of pojo is null value,
     * that field will be ignored, so that it will not be updated. You can
     * overwrite this by set updateNullField in hints.
     *
     * @param hints    Additional parameters that instruct how DAL Client perform database operation.
     *                 DalHintEnum.updateNullField can be used
     *                 to indicate that the field of pojo is null value will be update.
     * @param daoPojos list of pojos to be updated
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] update(DalHints hints, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return new int[0];
        }
        hints = DalHints.createIfAbsent(hints);
        return client.update(hints, daoPojos);
    }

    /**
     * Update the given pojo list in batch.
     *
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] batchUpdate(List<T> daoPojos) throws SQLException {
        return batchUpdate(null, daoPojos);
    }

    /**
     * Update the given pojo list in batch.
     *
     * @return how many rows been affected
     * @throws SQLException
     */
    public int[] batchUpdate(DalHints hints, List<T> daoPojos) throws SQLException {
        if (null == daoPojos || daoPojos.size() <= 0) {
            return new int[0];
        }
        hints = DalHints.createIfAbsent(hints);
        return client.batchUpdate(hints, daoPojos);
    }
}
