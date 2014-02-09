package org.safehaus.kiskis.mgmt.server.ui.modules.hive.action.chain;

import org.safehaus.kiskis.mgmt.server.ui.modules.hive.action.AgentInitAction;
import org.safehaus.kiskis.mgmt.server.ui.modules.hive.view.UILogger;
import org.safehaus.kiskis.mgmt.server.ui.modules.hive.common.chain.Action;
import org.safehaus.kiskis.mgmt.server.ui.modules.hive.common.chain.Chain;

public abstract class AbstractChainBuilder {

    protected static final String STATUS_COMMAND = "dpkg -l|grep ksks";

    protected  UILogger logger;
    protected  Action agentInitAction;

    protected AbstractChainBuilder(UILogger logger) {
        this.logger = logger;
        agentInitAction = new AgentInitAction(logger);
    }

    public abstract Chain getChain();

    protected boolean handleHadoopStatus(String stdOut) {

        if (stdOut == null || !stdOut.contains("ksks-hadoop")) {
            logger.complete("Hadoop NOT INSTALLED. Please install hadoop before installing Hive.");
            return false;
        }

        logger.info("Hadoop installed - OK");

        return true;
    }
}
