package com.ctrip.framework.drc.console.enums;

/**
 * Error message that can be shown in front-end user pages
 *
 * @author yongnian
 */
public enum ReadableErrorDefEnum implements IErrorDef {

    REQUEST_PARAM_INVALID("REQUEST_PARAM_INVALID", "invalid param! "),
    FORBIDDEN_OPERATION("FORBIDDEN_OPERATION", "forbidden operation"),
    DELETE_TBL_CHECK_FAIL_EXCEPTION("DELETE_TBL_CHECK_FAIL_EXCEPTION", "delete exception, please contact devops"),

    /**
     * query return empty result
     */
    QUERY_RESULT_EMPTY("QUERY_RESULT_EMPTY", "query result is empty"),
    QUERY_DATA_INCOMPLETE("QUERY_DATA_INCOMPLETE", "query exception, please contact devops"),

    GET_MYSQL_ENDPOINT_NULL("GET_MYSQL_ENDPOINT_NULL", "mysql endpoint null"),


    QUERY_MHA_DELAY_FAIL("QUERY_MHA_DELAY_FAIL", "query delay fail"),

    QUERY_MHA_SYNC_COUNT("QUERY_MHA_SYNC_COUNT", "query mah sync count fail"),

    QUERY_MQ_REPLICATION_FAIL("QUERY_MQ_REPLICATION_FAIL", "query mq replications fail"),

    /**
     * common dao exception
     */
    DAO_TBL_EXCEPTION("DAO_TBL_EXCEPTION", "dao exception"),

    /**
     * query datasource exception
     */
    QUERY_TBL_EXCEPTION("QUERY_TBL_EXCEPTION", "query datasource exception, please contact devops"),

    /**
     * delete datasource exception
     */
    DELETE_TBL_EXCEPTION("DELETE_TBL_EXCEPTION", "delete exception, please contact devops"),
    INSERT_TBL_EXCEPTION("INSERT_TBL_EXCEPTION", "insert exception, please contact devops"),


    DRC_TABLE_NOT_MATCH_BETWEEN_SRC_AND_DST("DRC_TABLE_NOT_MATCH","tables in src and dst not match"),
    DRC_TABLE_EMPTY("DRC_TABLE_EMPTY","table not found"),

    MHA_DB_REPLICATION_DELETE_NOT_ALLOW_FOR_EXIST_APPLIER("MHA_DB_REPLICATION_DELETE_NOT_ALLOW_FOR_EXIST_APPLIER", "delete mha db replication not allow with existing applier/messenger"),

    TIMEOUT_EXCEPTION("TIMEOUT_EXCEPTION", "timeout"),

    UNKNOWN_EXCEPTION("UNKNOWN_EXCEPTION", "unknown exception, please contact devops"),
    ;


    ReadableErrorDefEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private final String code;
    private final String message;

    @Override
    public String getCode() {
        return code;
    }


    @Override
    public String getMessage() {
        return message;
    }
}
