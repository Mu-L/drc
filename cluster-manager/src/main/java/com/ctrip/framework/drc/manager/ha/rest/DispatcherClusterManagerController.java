package com.ctrip.framework.drc.manager.ha.rest;

import com.ctrip.framework.drc.core.entity.DbCluster;
import com.ctrip.framework.drc.core.entity.Replicator;
import com.ctrip.framework.drc.manager.ha.cluster.ClusterManager;
import com.ctrip.framework.drc.manager.ha.cluster.META_SERVER_SERVICE;
import com.ctrip.framework.drc.manager.ha.cluster.impl.RemoteClusterManager;
import com.ctrip.framework.drc.manager.ha.config.ClusterManagerConfig;
import com.ctrip.xpipe.api.endpoint.Endpoint;
import com.ctrip.xpipe.spring.AbstractSpringConfigContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * @Author limingdong
 * @create 2020/5/17
 */
@RestController
@RequestMapping(META_SERVER_SERVICE.PATH.PATH_PREFIX)
public class DispatcherClusterManagerController extends AbstractDispatcherClusterManagerController {

    @Resource(name = AbstractSpringConfigContext.GLOBAL_EXECUTOR)
    private ExecutorService executors;

    @Autowired
    private ClusterManagerConfig clusterManagerConfig;


    @RequestMapping(path = META_SERVER_SERVICE.PATH.PATH_CLUSTER_CHANGE, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void clusterAdded(@PathVariable String clusterId, @RequestParam String dcId, @RequestParam String operator, @RequestBody(required = false) DbCluster clusterMeta,
                             @ModelAttribute ForwardInfo forwardInfo, @ModelAttribute(MODEL_META_SERVER) ClusterManager clusterManager) {

        logger.info("[clusterchange] add for clusterId: {}, dcId: {}, forward: {}, operator: {}", clusterId, dcId, forwardInfo, operator);
        if (!clusterManagerConfig.refreshWhenConsoleInvoke()) {
            logger.info("[clusterchange] add for clusterId: {} ignore", clusterId);
            return;
        }

        clusterManager.clusterAdded(dcId, clusterId, forwardInfo.clone(), operator);
    }

    @RequestMapping(path = META_SERVER_SERVICE.PATH.PATH_CLUSTER_CHANGE, method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void clusterModified(@PathVariable String clusterId, @RequestParam String operator, @RequestBody(required = false) DbCluster clusterMeta,
                                @ModelAttribute ForwardInfo forwardInfo, @ModelAttribute(MODEL_META_SERVER) ClusterManager clusterManager) {

        logger.info("[clusterchange] update for clusterId: {}, forward: {}, operator: {}", clusterId, forwardInfo, operator);
        if (!clusterManagerConfig.refreshWhenConsoleInvoke()) {
            logger.info("[clusterchange] update for clusterId: {} ignore", clusterId);
            return;
        }

        clusterManager.clusterModified(clusterId, forwardInfo.clone(), operator);
    }

    @RequestMapping(path = META_SERVER_SERVICE.PATH.PATH_CLUSTER_CHANGE, method = RequestMethod.DELETE)
    public void clusterDeleted(@PathVariable String clusterId, @RequestParam String operator,
                               @ModelAttribute ForwardInfo forwardInfo, @ModelAttribute(MODEL_META_SERVER) ClusterManager clusterManager) {

        logger.info("[clusterchange] delete for clusterId: {}, forward: {}, operator: {}", clusterId, forwardInfo, operator);
        if (!clusterManagerConfig.refreshWhenConsoleInvoke()) {
            logger.info("[clusterchange] delete for clusterId: {} ignore", clusterId);
            return;
        }

        clusterManager.clusterDeleted(clusterId, forwardInfo.clone(), operator);
    }

    /**
     * @param backupClusterId: targetName.targetMhaName
     * @param clusterId
     * @param ip
     * @param port
     * @param forwardInfo
     * @param clusterManager
     */
    @RequestMapping(path = META_SERVER_SERVICE.PATH.PATH_UPSTREAM_CHANGE, method = RequestMethod.PUT)
    public void upstreamChange(@PathVariable String backupClusterId, @PathVariable String clusterId,
                               @PathVariable String ip, @PathVariable int port, @ModelAttribute ForwardInfo forwardInfo, @ModelAttribute(MODEL_META_SERVER) ClusterManager clusterManager) {

        logger.debug("[upstreamChange]{},{},{},{}", clusterId, backupClusterId, ip, port);
        clusterManager.updateUpstream(clusterId, backupClusterId, ip, port, forwardInfo);
    }

    @RequestMapping(path = META_SERVER_SERVICE.PATH.GET_ACTIVE_REPLICATOR, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public DeferredResult<Replicator> getActiveReplicator(@PathVariable String clusterId,
                                                          @ModelAttribute ForwardInfo forwardInfo, @ModelAttribute(MODEL_META_SERVER) ClusterManager clusterManager) {

        logger.debug("[getActiveReplicator]{}", clusterId);
        return createDeferredResult(new Function<ClusterManager, Replicator>() {
            @Override
            public Replicator apply(ClusterManager clusterManager) {
                return clusterManager.getActiveReplicator(clusterId, forwardInfo);
            }
        }, clusterManager);
    }

    @RequestMapping(path = META_SERVER_SERVICE.PATH.GET_ACTIVE_MYSQL, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public DeferredResult<Endpoint> getActiveMySQL(@PathVariable String clusterId,
                                                   @ModelAttribute ForwardInfo forwardInfo, @ModelAttribute(MODEL_META_SERVER) ClusterManager clusterManager) {

        logger.debug("[getActiveMySQL]{}", clusterId);
        return createDeferredResult(new Function<ClusterManager, Endpoint>() {
            @Override
            public Endpoint apply(ClusterManager clusterManager) {
                return clusterManager.getActiveMySQL(clusterId, forwardInfo);
            }
        }, clusterManager);
    }

    private <T> DeferredResult<T> createDeferredResult(Function<ClusterManager, T> function, ClusterManager metaServer) {
        DeferredResult<T> response = new DeferredResult<>();
        if (metaServer instanceof RemoteClusterManager) {
            executors.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        response.setResult(function.apply(metaServer));
                    } catch (Exception e) {
                        response.setErrorResult(e);
                    }
                }
            });
        } else {
            response.setResult(function.apply(metaServer));
        }
        return response;
    }

}
