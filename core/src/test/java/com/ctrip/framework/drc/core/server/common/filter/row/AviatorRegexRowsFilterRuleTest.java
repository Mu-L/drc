package com.ctrip.framework.drc.core.server.common.filter.row;

import com.ctrip.framework.drc.core.driver.binlog.impl.AbstractRowsEvent;
import com.ctrip.framework.drc.core.meta.RowsFilterConfig;
import com.ctrip.framework.drc.core.server.common.enums.RowsFilterType;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @Author limingdong
 * @create 2022/5/13
 */
public class AviatorRegexRowsFilterRuleTest extends AbstractEventTest {

    private AviatorRegexRowsFilterRule aviatorRegexRowsFilterRule;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        List<RowsFilterConfig> rowsFilterConfigList = dataMediaConfig.getRowsFilters();
        RowsFilterConfig rowsFilterConfig = rowsFilterConfigList.get(0);
        aviatorRegexRowsFilterRule = new AviatorRegexRowsFilterRule(rowsFilterConfig);
    }

    @Override
    protected RowsFilterType getRowsFilterType() {
        return RowsFilterType.AviatorRegex;
    }

    @Test
    public void filterRows() throws Exception {
        rowsFilterContext.setDrcTableMapLogEvent(drcTableMapLogEvent);
        RowsFilterResult<List<AbstractRowsEvent.Row>> res = aviatorRegexRowsFilterRule.filterRows(writeRowsEvent, rowsFilterContext);
        Assert.assertFalse(res.isNoRowFiltered().noRowFiltered());
        List<AbstractRowsEvent.Row> filteredRow = res.getRes();
        Assert.assertTrue(filteredRow.size() == 1);
    }

    @Test
    public void testArrayWrapper() {
        Object[] array = new Object[2];
        array[0] = "test";
        array[1] = Boolean.TRUE;
        AviatorRegexRowsFilterRule.ArrayWrapper arrayWrapper = new AviatorRegexRowsFilterRule.ArrayWrapper(array);
        rowsFilterContext.putIfAbsent(arrayWrapper, RowsFilterResult.Status.from(false));

        Object[] array2 = new Object[2];
        array2[0] = "test";
        array2[1] = Boolean.TRUE;
        boolean res = rowsFilterContext.get(new AviatorRegexRowsFilterRule.ArrayWrapper(array2)).noRowFiltered();
        Assert.assertFalse(res);
    }

    protected String getContext() {
        return "string.startsWith(one,'one') && math.abs(id) % 5 == 0"; // match one row
    }


    private static Expression getExpression() {
        return AviatorEvaluator.compile("orderid != 0 && userid != nil && string.length(userid) > 0", true);
    }

    @Test
    public void test(){
        Expression expression = getExpression();
        Assert.assertEquals(true, expression.execute(expression.newEnv("orderid", 10, "userid", "10")));
    }
    @Test
    public void test2(){
        Expression expression = getExpression();
        Assert.assertEquals(false, expression.execute(expression.newEnv("orderid", 0, "userid", "10")));
    }


    @Test
    public void test3(){
        Expression expression = getExpression();
        Assert.assertEquals(false, expression.execute(expression.newEnv("orderid", 10, "userid", "")));
    }
    @Test
    public void test31(){
        Expression expression = getExpression();
        Assert.assertEquals(false, expression.execute(expression.newEnv("orderid", 10, "userid", null)));
    }

    @Test
    public void test4(){
        Expression expression = getExpression();
        Assert.assertEquals(false, expression.execute(expression.newEnv("orderid", 0, "userid", "")));
    }

    @Test
    public void test5(){
        Expression expression = getExpression();
        Assert.assertEquals(false, expression.execute(expression.newEnv("orderid", 10)));
    }
    @Test
    public void test6(){
        Expression expression = getExpression();
        Assert.assertEquals(false, expression.execute(expression.newEnv("orderid", 0,"userId","jzy")));
    }
}
