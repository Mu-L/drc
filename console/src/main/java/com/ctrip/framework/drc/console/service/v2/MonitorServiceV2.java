package com.ctrip.framework.drc.console.service.v2;

import java.sql.SQLException;
import java.util.List;

public interface MonitorServiceV2 {
    
    List<String> getMhaNamesToBeMonitored() throws SQLException;

    /**
     * @return dstMha whose srcMha is monitored
     */
    List<String> getDestMhaNamesToBeMonitored() throws SQLException;

    void switchMonitors(String mhaName, String status) throws SQLException;

}
