package org.safehaus.kiskis.mgmt.impl.accumulo;

import com.google.common.base.Strings;
import org.safehaus.kiskis.mgmt.api.accumulo.Accumulo;
import org.safehaus.kiskis.mgmt.api.accumulo.Config;
import org.safehaus.kiskis.mgmt.api.agentmanager.AgentManager;
import org.safehaus.kiskis.mgmt.api.commandrunner.AgentResult;
import org.safehaus.kiskis.mgmt.api.commandrunner.Command;
import org.safehaus.kiskis.mgmt.api.commandrunner.CommandRunner;
import org.safehaus.kiskis.mgmt.api.dbmanager.DbManager;
import org.safehaus.kiskis.mgmt.api.lxcmanager.LxcCreateException;
import org.safehaus.kiskis.mgmt.api.lxcmanager.LxcManager;
import org.safehaus.kiskis.mgmt.api.tracker.ProductOperation;
import org.safehaus.kiskis.mgmt.api.tracker.Tracker;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.Util;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.NodeState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccumuloImpl implements Accumulo {

    private CommandRunner commandRunner;
    private AgentManager agentManager;
    private DbManager dbManager;
    private Tracker tracker;
    private LxcManager lxcManager;
    private ExecutorService executor;


    public AccumuloImpl(CommandRunner commandRunner, AgentManager agentManager, DbManager dbManager, Tracker tracker, LxcManager lxcManager) {
        this.commandRunner = commandRunner;
        this.agentManager = agentManager;
        this.dbManager = dbManager;
        this.tracker = tracker;
        this.lxcManager = lxcManager;

        Commands.init(commandRunner);
    }

    public void init() {
        executor = Executors.newCachedThreadPool();
    }

    public void destroy() {
        executor.shutdown();
    }

    public UUID installCluster(final Config config) {
//        Preconditions.checkNotNull(config, "Configuration is null");

        final ProductOperation po = tracker.createProductOperation(Config.PRODUCT_KEY, "Installing Accumulo");

        executor.execute(new Runnable() {

            public void run() {

                if (config == null
                        || config.getMasterNode() == null
                        || config.getGcNode() == null
                        || Strings.isNullOrEmpty(config.getClusterName())
                        || Util.isCollectionEmpty(config.getTracers())
                        || Util.isCollectionEmpty(config.getMonitors())
                        || Util.isCollectionEmpty(config.getLoggers())
                        || Util.isCollectionEmpty(config.getTabletServers())
                        ) {
                    po.addLogFailed("Malformed configuration\nInstallation aborted");
                    return;
                }

                if (dbManager.getInfo(Config.PRODUCT_KEY, config.getClusterName(), Config.class) != null) {
                    po.addLogFailed(String.format("Cluster with name '%s' already exists\nInstallation aborted", config.getClusterName()));
                    return;
                }
                po.addLog("Updating db...");
                if (dbManager.saveInfo(Config.PRODUCT_KEY, config.getClusterName(), config)) {

                    po.addLog("Cluster info saved to DB\nInstalling Accumulo...");

                    //install
                    Command installCommand = Commands.getInstallCommand(config.getAllNodes());
                    commandRunner.runCommand(installCommand);

                    if (installCommand.hasSucceeded()) {
                        po.addLog("Installation succeeded\nSetting master node...");

                        Command setMasterCommand = Commands.getAddMasterCommand(config.getAllNodes(), config.getMasterNode());
                        commandRunner.runCommand(setMasterCommand);

                        if (setMasterCommand.hasSucceeded()) {
                            po.addLog("Setting master node succeeded\nSetting GC node...");
                            Command setGCNodeCommand = Commands.getAddGCCommand(config.getAllNodes(), config.getGcNode());
                            commandRunner.runCommand(setGCNodeCommand);
                            if (setGCNodeCommand.hasSucceeded()) {
                                po.addLog("Setting GC node succeeded\nSetting tracers...");

                                Command setTracersCommand = Commands.getAddTracersCommand(config.getAllNodes(), config.getTracers());
                                commandRunner.runCommand(setTracersCommand);

                                if (setTracersCommand.hasSucceeded()) {
                                    po.addLog("Setting tracers succeeded\nSetting monitors...");

                                    Command setMonitorsCommand = Commands.getAddMonitorsCommand(config.getAllNodes(), config.getMonitors());
                                    commandRunner.runCommand(setMonitorsCommand);

                                    if (setMonitorsCommand.hasSucceeded()) {
                                        po.addLog("Setting tracers succeeded\nSetting loggers...");

                                        Command setLoggersCommand = Commands.getAddSlavesCommand(config.getAllNodes(), config.getLoggers());
                                        commandRunner.runCommand(setLoggersCommand);

                                        if (setLoggersCommand.hasSucceeded()) {
                                            po.addLog("Setting tracers succeeded\nSetting tablet servers...");

                                            Command setTabletServersCommand = Commands.getAddSlavesCommand(config.getAllNodes(), config.getTabletServers());
                                            commandRunner.runCommand(setTabletServersCommand);

                                            if (setTabletServersCommand.hasSucceeded()) {
                                                po.addLog("Setting tablet servers succeeded\nStarting cluster...");

                                                Command startClusterCommand = Commands.getStartCommand(config.getAllNodes());
                                                commandRunner.runCommand(startClusterCommand);

                                                if (startClusterCommand.hasSucceeded()) {
                                                    po.addLogDone("Cluster started successfully\nDone");
                                                } else {
                                                    po.addLogFailed(String.format("Starting cluster failed, %s", startClusterCommand.getAllErrors()));
                                                }

                                            } else {
                                                po.addLogFailed(String.format("Setting tablet servers failed, %s", setTabletServersCommand.getAllErrors()));
                                            }

                                        } else {
                                            po.addLogFailed(String.format("Setting loggers failed, %s", setLoggersCommand.getAllErrors()));
                                        }

                                    } else {
                                        po.addLogFailed(String.format("Setting monitors failed, %s", setMonitorsCommand.getAllErrors()));
                                    }

                                } else {
                                    po.addLogFailed(String.format("Setting tracers failed, %s", setTracersCommand.getAllErrors()));
                                }

                            } else {
                                po.addLogFailed(String.format("Setting gc node failed, %s", setGCNodeCommand.getAllErrors()));
                            }
                        } else {
                            po.addLogFailed(String.format("Setting master node failed, %s", setMasterCommand.getAllErrors()));
                        }
                    } else {
                        po.addLogFailed(String.format("Installation failed, %s", installCommand.getAllErrors()));
                    }
                } else {
                    po.addLogFailed("Could not save cluster info to DB! Please see logs\nInstallation aborted");
                }

            }
        });

        return po.getId();
    }

    public UUID uninstallCluster(final String clusterName) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                String.format("Uninstalling cluster %s", clusterName));

        executor.execute(new Runnable() {

            public void run() {
                Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                po.addLog("Uninstalling cluster...");

                Command uninstallCommand = Commands.getUninstallCommand(config.getAllNodes());
                commandRunner.runCommand(uninstallCommand);

                if (uninstallCommand.hasCompleted()) {
                    if (uninstallCommand.hasSucceeded()) {
                        po.addLog("Cluster successfully uninstalled");
                    } else {
                        po.addLog(String.format("Uninstallation failed, %s, skipping...", uninstallCommand.getAllErrors()));
                    }
                    po.addLog("Updating db...");
                    if (dbManager.deleteInfo(Config.PRODUCT_KEY, config.getClusterName())) {
                        po.addLogDone("Cluster info deleted from DB\nDone");
                    } else {
                        po.addLogFailed("Error while deleting cluster info from DB. Check logs.\nFailed");
                    }
                } else {
                    po.addLogFailed("Uninstallation failed, command timed out");
                }


            }
        });

        return po.getId();
    }

    public UUID startNode(final String clusterName, final String lxcHostName) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                String.format("Starting node %s in %s", lxcHostName, clusterName));

        executor.execute(new Runnable() {

            public void run() {
                Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                final Agent node = agentManager.getAgentByHostname(lxcHostName);
                if (node == null) {
                    po.addLogFailed(String.format("Agent with hostname %s is not connected\nOperation aborted", lxcHostName));
                    return;
                }

                if (!config.getAllNodes().contains(node)) {
                    po.addLogFailed(String.format("Agent with hostname %s does not belong to cluster %s", lxcHostName, clusterName));
                    return;
                }

                po.addLog("Starting node...");

                Command startCommand = Commands.getStartCommand(Util.wrapAgentToSet(node));
                commandRunner.runCommand(startCommand);
                Command statusCommand = Commands.getStatusCommand(node);
                commandRunner.runCommand(statusCommand);
                AgentResult result = statusCommand.getResults().get(node.getUuid());
                NodeState nodeState = NodeState.UNKNOWN;
                if (result != null) {
                    if (result.getStdOut().contains("is running")) {
                        nodeState = NodeState.RUNNING;
                    } else if (result.getStdOut().contains("is not running")) {
                        nodeState = NodeState.STOPPED;
                    }
                }

                if (NodeState.RUNNING.equals(nodeState)) {
                    po.addLogDone(String.format("Node on %s started", lxcHostName));
                } else {
                    po.addLogFailed(String.format("Failed to start node %s. %s",
                            lxcHostName, startCommand.getAllErrors()
                    ));
                }

            }
        });

        return po.getId();
    }

    public UUID stopNode(final String clusterName, final String lxcHostName) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                String.format("Stopping node %s in %s", lxcHostName, clusterName));

        executor.execute(new Runnable() {

            public void run() {
                Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                final Agent node = agentManager.getAgentByHostname(lxcHostName);
                if (node == null) {
                    po.addLogFailed(String.format("Agent with hostname %s is not connected\nOperation aborted", lxcHostName));
                    return;
                }
                if (!config.getAllNodes().contains(node)) {
                    po.addLogFailed(String.format("Agent with hostname %s does not belong to cluster %s", lxcHostName, clusterName));
                    return;
                }
                po.addLog("Stopping node...");

                Command stopCommand = Commands.getStopCommand(node);
                commandRunner.runCommand(stopCommand);
                Command statusCommand = Commands.getStatusCommand(node);
                commandRunner.runCommand(statusCommand);
                AgentResult result = statusCommand.getResults().get(node.getUuid());
                NodeState nodeState = NodeState.UNKNOWN;
                if (result != null) {
                    if (result.getStdOut().contains("is running")) {
                        nodeState = NodeState.RUNNING;
                    } else if (result.getStdOut().contains("is not running")) {
                        nodeState = NodeState.STOPPED;
                    }
                }

                if (NodeState.STOPPED.equals(nodeState)) {
                    po.addLogDone(String.format("Node on %s stopped", lxcHostName));
                } else {
                    po.addLogFailed(String.format("Failed to stop node %s. %s",
                            lxcHostName, stopCommand.getAllErrors()
                    ));
                }

            }
        });

        return po.getId();
    }

    public UUID checkNode(final String clusterName, final String lxcHostName) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                String.format("Checking node %s in %s", lxcHostName, clusterName));

        executor.execute(new Runnable() {

            public void run() {
                Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                final Agent node = agentManager.getAgentByHostname(lxcHostName);
                if (node == null) {
                    po.addLogFailed(String.format("Agent with hostname %s is not connected\nOperation aborted", lxcHostName));
                    return;
                }
                if (!config.getAllNodes().contains(node)) {
                    po.addLogFailed(String.format("Agent with hostname %s does not belong to cluster %s", lxcHostName, clusterName));
                    return;
                }
                po.addLog("Checking node...");

                Command checkNodeCommand = Commands.getStatusCommand(node);
                commandRunner.runCommand(checkNodeCommand);

                NodeState nodeState = NodeState.UNKNOWN;
                if (checkNodeCommand.hasCompleted()) {
                    AgentResult result = checkNodeCommand.getResults().get(node.getUuid());
                    if (result.getStdOut().contains("is running")) {
                        nodeState = NodeState.RUNNING;
                    } else if (result.getStdOut().contains("is not running")) {
                        nodeState = NodeState.STOPPED;
                    }
                }

                if (NodeState.UNKNOWN.equals(nodeState)) {
                    po.addLogFailed(String.format("Failed to check status of %s, %s",
                            lxcHostName, checkNodeCommand.getAllErrors()
                    ));
                } else {
                    po.addLogDone(String.format("Node %s is %s",
                            lxcHostName, nodeState
                    ));
                }

            }
        });

        return po.getId();
    }

    public UUID destroyNode(final String clusterName, final String lxcHostName) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                String.format("Destroying %s in %s", lxcHostName, clusterName));

        executor.execute(new Runnable() {

            public void run() {
                final Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                Agent agent = agentManager.getAgentByHostname(lxcHostName);
                if (agent == null) {
                    po.addLogFailed(String.format("Agent with hostname %s is not connected\nOperation aborted", lxcHostName));
                    return;
                }
                if (!config.getAllNodes().contains(agent)) {
                    po.addLogFailed(String.format("Agent with hostname %s does not belong to cluster %s", lxcHostName, clusterName));
                    return;
                }

                if (config.getAllNodes().size() == 1) {
                    po.addLogFailed("This is the last node in the cluster. Please, destroy cluster instead\nOperation aborted");
                    return;
                }

                //destroy lxc
                po.addLog("Destroying lxc container...");
                Agent physicalAgent = agentManager.getAgentByHostname(agent.getParentHostName());
                if (physicalAgent == null) {
                    po.addLog(
                            String.format("Could not determine physical parent of %s. Use LXC module to cleanup, skipping...",
                                    agent.getHostname())
                    );
                } else {
                    if (!lxcManager.destroyLxcOnHost(physicalAgent, agent.getHostname())) {
                        po.addLog("Could not destroy lxc container. Use LXC module to cleanup, skipping...");
                    } else {
                        po.addLog("Lxc container destroyed successfully");
                    }
                }
                //update db
                po.addLog("Updating db...");
//                config.getNodes().remove(agent);
                if (!dbManager.saveInfo(Config.PRODUCT_KEY, config.getClusterName(), config)) {
                    po.addLogFailed(String.format("Error while updating cluster info [%s] in DB. Check logs\nFailed",
                            config.getClusterName()));
                } else {
                    po.addLogDone("Done");
                }
            }
        });

        return po.getId();
    }

    public UUID addNode(final String clusterName) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                String.format("Adding node to %s", clusterName));

        executor.execute(new Runnable() {

            public void run() {
                Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                try {

                    po.addLog("Creating lxc container...");

                    Map<Agent, Set<Agent>> lxcAgentsMap = lxcManager.createLxcs(1);

                    Agent lxcAgent = lxcAgentsMap.entrySet().iterator().next().getValue().iterator().next();

//                    config.getNodes().add(lxcAgent);
                    po.addLog("Lxc container created successfully\nUpdating db...");
                    if (dbManager.saveInfo(Config.PRODUCT_KEY, clusterName, config)) {
                        po.addLog("Cluster info updated in DB\nInstalling Accumulo...");

                        Command installCommand = Commands.getInstallCommand(Util.wrapAgentToSet(lxcAgent));
                        commandRunner.runCommand(installCommand);

                        if (installCommand.hasSucceeded()) {
                            po.addLogDone("Installation succeeded\nDone");

                        } else {
                            po.addLogFailed(String.format("Installation failed, %s",
                                    installCommand.getAllErrors()));
                        }
                    } else {
                        po.addLogFailed("Error while updating cluster info in DB. Check logs. Use LXC Module to cleanup\nFailed");
                    }

                } catch (LxcCreateException ex) {
                    po.addLogFailed(ex.getMessage());
                }
            }
        });

        return po.getId();
    }

    public List<Config> getClusters() {

        return dbManager.getInfo(Config.PRODUCT_KEY, Config.class);

    }

}
