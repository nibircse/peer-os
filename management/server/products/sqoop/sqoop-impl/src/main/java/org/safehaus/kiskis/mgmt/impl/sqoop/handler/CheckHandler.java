package org.safehaus.kiskis.mgmt.impl.sqoop.handler;

import org.safehaus.kiskis.mgmt.api.tracker.ProductOperation;
import org.safehaus.kiskis.mgmt.impl.sqoop.SqoopImpl;

public class CheckHandler extends AbstractHandler {

    public CheckHandler(SqoopImpl manager, String clusterName, ProductOperation po) {
        super(manager, clusterName, po);
    }

    public void run() {
        // TODO:
    }

}
