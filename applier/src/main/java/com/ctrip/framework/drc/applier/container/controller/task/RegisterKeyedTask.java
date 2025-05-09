package com.ctrip.framework.drc.applier.container.controller.task;

import com.ctrip.framework.drc.applier.container.ApplierServerContainer;
import com.ctrip.framework.drc.core.monitor.reporter.DefaultEventMonitorHolder;
import com.ctrip.framework.drc.core.server.config.applier.dto.ApplierConfigDto;
import com.ctrip.framework.drc.fetcher.container.controller.task.BaseRegisterKeyedTask;

/**
 * @ClassName ApplierRegisterKeyedTask
 * @Author haodongPan
 * @Date 2024/1/2 17:45
 * @Version: $
 */
public class RegisterKeyedTask extends BaseRegisterKeyedTask {

    public RegisterKeyedTask(String registryKey, ApplierConfigDto applierConfig, ApplierServerContainer serverContainer) {
        super(registryKey, applierConfig, serverContainer);
    }

    @Override
    protected void doExecute() {
        try {
            logger.info("[Register] applier instance for {} with {}", registryKey, applierConfig);
            super.doExecute();
            future().setSuccess();
        } catch (Throwable t) {
            logger.error("Register] error in applier instance for {} with {}", registryKey, t);
            DefaultEventMonitorHolder.getInstance().logEvent("DRC.applier.instance.error", "register");
            future().setFailure(t);
        }
    }
}
