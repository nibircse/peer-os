package org.safehaus.subutai.plugin.elasticsearch.api;


import org.safehaus.subutai.api.manager.helper.Environment;
import org.safehaus.subutai.common.protocol.ApiBase;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.tracker.ProductOperation;

import java.util.UUID;


public interface Elasticsearch extends ApiBase<Config> {

	public UUID startAllNodes(String clusterName);

	public UUID checkAllNodes(String clusterName);

	public UUID stopAllNodes(String clusterName);

    public UUID addNode(String clusterName, String lxcHostname);

    public UUID destroyNode(String clusterName, String lxcHostname);

    ClusterSetupStrategy getClusterSetupStrategy( Environment environment, Config config, ProductOperation po );

}
