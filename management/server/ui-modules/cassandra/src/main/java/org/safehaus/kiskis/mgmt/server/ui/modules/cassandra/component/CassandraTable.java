package org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.component;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.CassandraModule;
import org.safehaus.kiskis.mgmt.shared.protocol.CassandraClusterInfo;
import org.safehaus.kiskis.mgmt.shared.protocol.api.CommandManagerInterface;

import java.util.List;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA. User: daralbaev Date: 11/30/13 Time: 6:56 PM
 */
public class CassandraTable extends Table {

    private final CommandManagerInterface commandManager;
    private IndexedContainer container;
    private final CassandraModule.ModuleComponent parent;
    private NodesWindow window;

    public CassandraTable(final CommandManagerInterface commandManager, final CassandraModule.ModuleComponent window) {
        this.commandManager = commandManager;
        this.parent = window;

        this.setCaption("Cassandra clusters");
        this.setContainerDataSource(getCassandraContainer());

        this.setWidth("100%");
        this.setHeight(100, Sizeable.UNITS_PERCENTAGE);

        this.setPageLength(20);
        this.setSelectable(true);
        this.setImmediate(true);
    }

    private IndexedContainer getCassandraContainer() {
        container = new IndexedContainer();

        // Create the container properties
        container.addContainerProperty(CassandraClusterInfo.UUID_LABEL, UUID.class, "");
        container.addContainerProperty(CassandraClusterInfo.NAME_LABEL, String.class, "");
//        container.addContainerProperty(CassandraClusterInfo.DATADIR_LABEL, String.class, "");
//        container.addContainerProperty(CassandraClusterInfo.COMMITLOGDIR_LABEL, String.class, "");
//        container.addContainerProperty(CassandraClusterInfo.SAVEDCACHEDIR_LOG, String.class, "");
        container.addContainerProperty("Start", Button.class, "");
        container.addContainerProperty("Stop", Button.class, "");
        container.addContainerProperty("Destroy", Button.class, "");
//        container.addContainerProperty(CassandraClusterInfo.NODES_LABEL, List.class, 0);
//        container.addContainerProperty(CassandraClusterInfo.SEEDS_LABEL, List.class, 0);

        // Create some orders
        List<CassandraClusterInfo> cdList = commandManager.getCassandraClusterData();
        for (CassandraClusterInfo cluster : cdList) {
            addOrderToContainer(container, cluster);
        }

        return container;
    }

    private void addOrderToContainer(Container container, final CassandraClusterInfo cd) {
        Object itemId = container.addItem();
        Item item = container.getItem(itemId);
        item.getItemProperty(CassandraClusterInfo.UUID_LABEL).setValue(cd.getUuid());
        item.getItemProperty(CassandraClusterInfo.NAME_LABEL).setValue(cd.getName());
//        item.getItemProperty(CassandraClusterInfo.DATADIR_LABEL).setValue(cd.getDataDir());
//        item.getItemProperty(CassandraClusterInfo.COMMITLOGDIR_LABEL).setValue(cd.getCommitLogDir());
//        item.getItemProperty(CassandraClusterInfo.SAVEDCACHEDIR_LOG).setValue(cd.getSavedCacheDir());

        Button startButton = new Button("Start");
        startButton.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                String caption = cd.getName();
                window = new NodesWindow(caption, cd.getNodes(), commandManager);
                window.setModal(true);
                getApplication().getMainWindow().addWindow(window);
            }
        });
        Button stopButton = new Button("Start");
        stopButton.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                getWindow().showNotification("Stop cassandra cluster");
            }
        });
        Button destroyButton = new Button("Destroy");
        destroyButton.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                getWindow().showNotification("Destroy cassandra cluster");
            }
        });

        item.getItemProperty("Start").setValue(startButton);
        item.getItemProperty("Stop").setValue(stopButton);
        item.getItemProperty("Destroy").setValue(destroyButton);
//        item.getItemProperty(CassandraClusterInfo.NODES_LABEL).setValue(cd.getNodes());
//        item.getItemProperty(CassandraClusterInfo.SEEDS_LABEL).setValue(cd.getSeeds());
    }

    public void refreshDatasource() {
        this.setContainerDataSource(getCassandraContainer());
    }

    public NodesWindow getNodesWindow() {
        return window;
    }

}
