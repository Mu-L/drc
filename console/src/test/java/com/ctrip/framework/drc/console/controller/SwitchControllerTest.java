package com.ctrip.framework.drc.console.controller;

import com.alibaba.fastjson.JSON;
import com.ctrip.framework.drc.console.controller.v1.SwitchController;
import com.ctrip.framework.drc.console.service.impl.SwitchServiceImpl;
import com.ctrip.framework.drc.core.server.config.console.dto.ClusterConfigDto;
import com.ctrip.framework.drc.core.service.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author shenhaibo
 * @version 1.0
 * date: 2020-03-04
 */
public class SwitchControllerTest {

    private MockMvc mvc;

    @InjectMocks
    private SwitchController controller;

    @Mock
    private SwitchServiceImpl switchService;

    private static final String DB_ENDPOINT = "127.0.0.1:3306";

    private static final String REP_ENDPOINT = "127.0.0.1:8383";

    private static final String CLUSTER_ID = "testClusterId";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        /** initialization */
        MockitoAnnotations.openMocks(this);
        /** build mvc env */
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        // for void return type mockito
        Mockito.doNothing().when(switchService).switchUpdateDb(Mockito.any());
        Mockito.doNothing().when(switchService).switchListenReplicator(Mockito.any());
    }

    @Test
    public void testBatchNotifyMasterDb() throws Exception {
        MvcResult mvcResult = mvc.perform(
                MockMvcRequestBuilders.put("/api/drc/v1/switch/clusters/dbs/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(new ClusterConfigDto()))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        String responseStr = response.getContentAsString();
        Assert.assertEquals(200, status);
        JsonNode jsonNode = objectMapper.readTree(responseStr);
        Assert.assertEquals(0, jsonNode.get("status").asInt());
        Assert.assertEquals("handle success", jsonNode.get("message").asText());
    }

    @Test
    public void testBatchNotifyMasterReplicator() throws Exception {
        String jsonStr=JSON.toJSONString(REP_ENDPOINT);
        MvcResult mvcResult = mvc.perform(
                MockMvcRequestBuilders.put("/api/drc/v1/switch/clusters/replicators/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(new ClusterConfigDto()))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        int status = response.getStatus();
        String responseStr = response.getContentAsString();
        Assert.assertEquals(200, status);
        JsonNode jsonNode = objectMapper.readTree(responseStr);
        Assert.assertEquals("true", jsonNode.get("data").toString());
    }


}