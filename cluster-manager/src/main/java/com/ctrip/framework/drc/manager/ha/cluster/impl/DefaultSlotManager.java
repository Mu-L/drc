package com.ctrip.framework.drc.manager.ha.cluster.impl;

import com.ctrip.framework.drc.manager.ha.cluster.ClusterException;
import com.ctrip.framework.drc.manager.ha.cluster.SLOT_STATE;
import com.ctrip.framework.drc.manager.ha.cluster.SlotInfo;
import com.ctrip.framework.drc.manager.ha.cluster.SlotManager;
import com.ctrip.framework.drc.manager.ha.config.ClusterManagerConfig;
import com.ctrip.framework.drc.manager.ha.config.ClusterZkConfig;
import com.ctrip.xpipe.api.codec.Codec;
import com.ctrip.xpipe.api.factory.ObjectFactory;
import com.ctrip.xpipe.api.lifecycle.TopElement;
import com.ctrip.xpipe.lifecycle.AbstractLifecycle;
import com.ctrip.xpipe.utils.MapUtils;
import com.ctrip.xpipe.utils.XpipeThreadFactory;
import com.ctrip.xpipe.zk.ZkClient;
import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author limingdong
 * @create 2020/4/19
 */
@Component
public class DefaultSlotManager extends AbstractLifecycle implements SlotManager, TopElement {

    @Autowired
    private ZkClient zkClient;

    @Autowired
    private ClusterManagerConfig config;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private Map<Integer, SlotInfo> slotsMap = new ConcurrentHashMap<Integer,SlotInfo>();

    private Map<String, Set<Integer>> serverMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(1, XpipeThreadFactory.create(getClass().getSimpleName()));

    private ScheduledFuture<?> future;

    @Override
    protected void doInitialize() throws Exception {
        super.doInitialize();

    }

    @Override
    protected void doStart() throws Exception {

        CuratorFramework client = zkClient.get();
        client.createContainers(ClusterZkConfig.getClusterManagerSlotsPath());

        refresh();

        future = scheduled.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try{
                    refresh();
                }catch(Throwable th){
                    logger.error("[run]", th);
                }
            }
        }, config.getSlotRefreshMilli(), config.getSlotRefreshMilli(), TimeUnit.MILLISECONDS);
    }


    @Override
    protected void doStop() throws Exception {

        if(future != null){
            future.cancel(true);
        }
        super.doStop();
    }
    @Override
    public String getSlotServerId(int slotId) {

        try{
            lock.readLock().lock();

            SlotInfo  slotInfo = slotsMap.get(slotId);
            if(slotInfo == null){
                return null;
            }
            return slotInfo.getServerId();
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<Integer> getSlotsByServerId(String serverId) {
        Set<Integer> slots = getSlotsByServerId(serverId, true);
        if (slots == null) {
            return Sets.newHashSet();
        }
        return Sets.newHashSet(slots);
    }

    @Override
    public Set<Integer> getSlotsByServerId(String serverId, boolean includeMoving) {

        try{
            lock.readLock().lock();

            if(includeMoving){
                return serverMap.get(serverId);
            }else{
                Set<Integer> slots = new HashSet<>(serverMap.get(serverId));


                Set<Integer> movingSlots = new HashSet<>();
                for(Integer slotId : slots){
                    SlotInfo slotInfo = slotsMap.get(slotId);
                    if(slotInfo.getSlotState() == SLOT_STATE.MOVING){
                        movingSlots.add(slotId);
                    }
                }
                slots.removeAll(movingSlots);
                return slots;
            }
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public void refresh() throws ClusterException {

        doRefresh();
    }

    @Override
    public void refresh(int... slotIds) throws ClusterException {

        String slotsPath = ClusterZkConfig.getClusterManagerSlotsPath();
        CuratorFramework client = zkClient.get();

        Map<Integer, SlotInfo> slotsInfo = new HashMap<>();
        for(int slotId : slotIds){
            byte[] slotContent = new byte[0];
            try {
                slotContent = client.getData().forPath(slotsPath + "/" + String.valueOf(slotId));
            } catch (Exception e) {
                throw new ClusterException("get data for:" + slotId, e);
            }
            SlotInfo slotInfo = Codec.DEFAULT.decode(slotContent, SlotInfo.class);
            slotsInfo.put(slotId, slotInfo);
        }

        try{
            lock.writeLock().lock();
            for(Map.Entry<Integer, SlotInfo> entry : slotsInfo.entrySet()){
                Integer slotId = entry.getKey();
                SlotInfo slotInfo = entry.getValue();
                SlotInfo oldSlotInfo = slotsMap.get(slotId);

                slotsMap.put(slotId, slotInfo);

                if(oldSlotInfo != null){
                    Set<Integer> oldServerSlot = serverMap.get(oldSlotInfo.getServerId());
                    if(oldServerSlot != null){
                        oldServerSlot.remove(slotId);
                    }
                }
                getOrCreateServerMap(serverMap, slotInfo.getServerId()).add(slotId);
            }
        }finally{
            lock.writeLock().unlock();
        }

    }

    private void doRefresh() throws ClusterException {

        logger.debug("[doRefresh]");

        Map<Integer, SlotInfo> slotsMap = new ConcurrentHashMap<>();
        Map<String, Set<Integer>> serverMap = new ConcurrentHashMap<>();

        try{
            CuratorFramework client = zkClient.get();
            String slotsPath = ClusterZkConfig.getClusterManagerSlotsPath();
            for(String slotPath : client.getChildren().forPath(slotsPath)){
                int slot = Integer.parseInt(slotPath);
                byte[] slotContent = client.getData().forPath(slotsPath + "/" + slotPath);
                SlotInfo slotInfo = Codec.DEFAULT.decode(slotContent, SlotInfo.class);


                Set<Integer> serverSlots = getOrCreateServerMap(serverMap, slotInfo.getServerId());
                serverSlots.add(slot);
                slotsMap.put(slot, slotInfo);
            }
        }catch (Exception e){
            throw new ClusterException("doRefersh", e);
        }

        try{
            lock.writeLock().lock();
            this.slotsMap = slotsMap;
            this.serverMap = serverMap;
        }finally{
            lock.writeLock().unlock();
        }
    }

    private Set<Integer> getOrCreateServerMap(Map<String, Set<Integer>> serverMap, String serverId) {

        return MapUtils.getOrCreate(serverMap, serverId, new ObjectFactory<Set<Integer>>() {

            @Override
            public Set<Integer> create() {
                return new HashSet<>();
            }
        });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void move(int slotId, String fromServer, String toServer) {


        try{
            lock.writeLock().lock();

            if(serverMap.get(fromServer) == null){
                logger.error("[fromServer not Found]" + fromServer);
                return;
            }
            slotsMap.put(slotId, new SlotInfo(toServer));
            serverMap.get(fromServer).remove(slotId);

            Set<Integer> toSlots = MapUtils.getOrCreate(serverMap, toServer, new ObjectFactory<Set<Integer>>() {

                @Override
                public Set<Integer> create() {
                    return new HashSet<>();
                }
            });

            toSlots.add(slotId);
        }finally{
            lock.writeLock().unlock();
        }
    }

    @Override
    public Set<Integer> allSlots() {
        try{
            lock.readLock().lock();
            return new HashSet<>(slotsMap.keySet());
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> allServers() {

        try{
            lock.readLock().lock();
            return new HashSet<>(serverMap.keySet());
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public int getSlotsSizeByServerId(String serverId) {

        try{
            lock.readLock().lock();
            Set<Integer> slots = getSlotsByServerId(serverId);
            if(slots == null){
                return 0;
            }
            return slots.size();
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<Integer, SlotInfo> allMoveingSlots() {

        try{
            lock.readLock().lock();

            Map<Integer, SlotInfo> result = new HashMap<>();

            for(Map.Entry<Integer, SlotInfo> entry : slotsMap.entrySet()){

                Integer slot = entry.getKey();
                SlotInfo slotInfo = entry.getValue();

                if(slotInfo.getSlotState() == SLOT_STATE.MOVING){
                    result.put(slot, slotInfo.clone());
                }
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public SlotInfo getSlotInfo(int slotId) {
        try{
            lock.readLock().lock();
            return slotsMap.get(slotId);
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public int getSlotIdByKey(Object key) {

        int hash = key.hashCode();
        if(hash == Integer.MIN_VALUE){
            return 0;
        }
        return Math.abs(hash) % TOTAL_SLOTS;
    }

    @Override
    public SlotInfo getSlotInfoByKey(Object key) {
        try{
            lock.readLock().lock();
            return slotsMap.get(getSlotIdByKey(key));
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public String getServerIdByKey(Object key) {

        int slotId = getSlotIdByKey(key);
        try{
            lock.readLock().lock();
            SlotInfo slotInfo = slotsMap.get(slotId);
            if(slotInfo == null){
                return null;
            }
            return slotInfo.getServerId();
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<Integer, SlotInfo> allSlotsInfo() {
        try{
            lock.readLock().lock();
            return new HashMap<>(slotsMap);
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public void clearAll() {
        try {
            lock.writeLock().lock();

            slotsMap.clear();
            serverMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }


}
