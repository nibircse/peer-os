/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.ui.hbase.manager;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.*;
import org.safehaus.subutai.api.hadoop.Config;
import org.safehaus.subutai.api.hbase.HBaseConfig;
import org.safehaus.subutai.api.hbase.HBaseType;
import org.safehaus.subutai.server.ui.component.ConfirmationDialog;
import org.safehaus.subutai.server.ui.component.ProgressWindow;
import org.safehaus.subutai.server.ui.component.TerminalWindow;
import org.safehaus.subutai.shared.protocol.Agent;
import org.safehaus.subutai.shared.protocol.Util;
import org.safehaus.subutai.ui.hbase.HBaseUI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author dilshat
 */
public class Manager {

    private final VerticalLayout contentRoot;
    private final ComboBox clusterCombo;
    private final Table masterTable;
    private final Table regionTable;
    private final Table quorumTable;
    private final Table bmasterTable;
    private HBaseConfig config;

    public Manager() {

        contentRoot = new VerticalLayout();
        contentRoot.setSpacing(true);
        contentRoot.setWidth(90, Sizeable.Unit.PIXELS);
        contentRoot.setHeight(100, Sizeable.Unit.PERCENTAGE);

        VerticalLayout content = new VerticalLayout();
        content.setWidth(100, Sizeable.Unit.PERCENTAGE);
        content.setHeight(100, Sizeable.Unit.PERCENTAGE);

        contentRoot.addComponent(content);
        contentRoot.setComponentAlignment(content, Alignment.TOP_CENTER);
        contentRoot.setMargin(true);

        //tables go here
        masterTable = createTableTemplate("Master", 100);
        regionTable = createTableTemplate("Region", 100);
        quorumTable = createTableTemplate("Quorum", 100);
        bmasterTable = createTableTemplate("Backup master", 100);
        //tables go here

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing(true);

        Label clusterNameLabel = new Label("Select the cluster");
        controlsContent.addComponent(clusterNameLabel);

        clusterCombo = new ComboBox();
        clusterCombo.setImmediate(true);
        clusterCombo.setTextInputAllowed(false);
        clusterCombo.setWidth(200, Sizeable.Unit.PIXELS);
        clusterCombo.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                config = (HBaseConfig) event.getProperty().getValue();
                refreshUI();
            }
        });

        controlsContent.addComponent(clusterCombo);

        Button refreshClustersBtn = new Button("Refresh clusters");
        refreshClustersBtn.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                refreshClustersInfo();
            }
        });

        controlsContent.addComponent(refreshClustersBtn);

        Button startClustersBtn = new Button("Start cluster");
        startClustersBtn.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                if (config != null) {
                    UUID trackID = HBaseUI.getHbaseManager().startCluster(config.getClusterName());
                    ProgressWindow window = new ProgressWindow(HBaseUI.getExecutor(), HBaseUI.getTracker(), trackID, Config.PRODUCT_KEY);
                    window.getWindow().addCloseListener(new Window.CloseListener() {
                        @Override
                        public void windowClose(Window.CloseEvent closeEvent) {
                            refreshClustersInfo();
                        }
                    });
                    contentRoot.getUI().addWindow(window.getWindow());
                } else {
                    show("Please, select cluster");
                }
            }
        });

        controlsContent.addComponent(startClustersBtn);

        Button stopClustersBtn = new Button("Stop cluster");
        stopClustersBtn.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                if (config != null) {
                    UUID trackID = HBaseUI.getHbaseManager().stopCluster(config.getClusterName());
                    ProgressWindow window = new ProgressWindow(HBaseUI.getExecutor(), HBaseUI.getTracker(), trackID, Config.PRODUCT_KEY);
                    window.getWindow().addCloseListener(new Window.CloseListener() {
                        @Override
                        public void windowClose(Window.CloseEvent closeEvent) {
                            refreshClustersInfo();
                        }
                    });
                } else {
                    show("Please, select cluster");
                }
            }
        });

        controlsContent.addComponent(stopClustersBtn);

        Button checkClustersBtn = new Button("Check cluster");
        checkClustersBtn.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                if (config != null) {
                    UUID trackID = HBaseUI.getHbaseManager().checkCluster(config.getClusterName());
                    ProgressWindow window = new ProgressWindow(HBaseUI.getExecutor(), HBaseUI.getTracker(), trackID, Config.PRODUCT_KEY);
                    window.getWindow().addCloseListener(new Window.CloseListener() {
                        @Override
                        public void windowClose(Window.CloseEvent closeEvent) {
                            refreshClustersInfo();
                        }
                    });
                } else {
                    show("Please, select cluster");
                }
            }
        });

        controlsContent.addComponent(checkClustersBtn);

        Button destroyClusterBtn = new Button("Destroy cluster");
        destroyClusterBtn.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (config != null) {
                    ConfirmationDialog alert = new ConfirmationDialog(String.format("Do you want to add node to the %s cluster?", config.getClusterName()),
                            "Yes", "No");
                    alert.getOk().addClickListener(new Button.ClickListener() {
                        @Override
                        public void buttonClick(Button.ClickEvent clickEvent) {
                            UUID trackID = HBaseUI.getHbaseManager().uninstallCluster(config.getClusterName());
                            ProgressWindow window = new ProgressWindow(HBaseUI.getExecutor(), HBaseUI.getTracker(), trackID, Config.PRODUCT_KEY);
                            window.getWindow().addCloseListener(new Window.CloseListener() {
                                @Override
                                public void windowClose(Window.CloseEvent closeEvent) {
                                    refreshClustersInfo();
                                }
                            });
                            contentRoot.getUI().addWindow(window.getWindow());
                        }
                    });

                    contentRoot.getUI().addWindow(alert.getAlert());
                } else {
                    show("Please, select cluster");
                }
            }

        });

        controlsContent.addComponent(destroyClusterBtn);
        content.addComponent(controlsContent);
        content.addComponent(masterTable);
        content.addComponent(regionTable);
        content.addComponent(quorumTable);
        content.addComponent(bmasterTable);

    }

    public static void checkNodesStatus(Table table) {
        for (Object o : table.getItemIds()) {
            int rowId = (Integer) o;
            Item row = table.getItem(rowId);
            Button checkBtn = (Button) (row.getItemProperty("Check").getValue());
            checkBtn.click();
        }
    }

    public Component getContent() {
        return contentRoot;
    }

    private void show(String notification) {
        Notification.show(notification);
    }

    private void populateMasterTable(final Table table, Set<UUID> agents, final HBaseType type) {

        table.removeAllItems();

        for (final UUID uuid : agents) {
            final Embedded progressIcon = new Embedded("", new ThemeResource("img/spinner.gif"));
            progressIcon.setVisible(false);

            final Object rowId = table.addItem(new Object[]{
                            uuid,
                            type,
                            progressIcon},
                    null
            );
        }
    }

    private void populateTable(final Table table, Set<UUID> agents, final HBaseType type) {

        table.removeAllItems();

        for (final UUID uuid : agents) {
            final Embedded progressIcon = new Embedded("", new ThemeResource("img/spinner.gif"));
            progressIcon.setVisible(false);

            final Object rowId = table.addItem(new Object[]{
                            uuid,
                            type,
                            progressIcon},
                    null
            );
        }
    }

    private void refreshUI() {
        if (config != null) {
            populateTable(quorumTable, config.getQuorum(), HBaseType.HQuorumPeer);
            populateTable(regionTable, config.getRegion(), HBaseType.HRegionServer);

            Set<UUID> masterSet = new HashSet<UUID>();
            masterSet.add(config.getMaster());
            populateMasterTable(masterTable, masterSet, HBaseType.HMaster);

            Set<UUID> bmasterSet = new HashSet<UUID>();
            bmasterSet.add(config.getBackupMasters());
            populateTable(bmasterTable, bmasterSet, HBaseType.BackupMaster);

        } else {
            regionTable.removeAllItems();
            quorumTable.removeAllItems();
            bmasterTable.removeAllItems();
            masterTable.removeAllItems();
        }
    }

    public void refreshClustersInfo() {
        List<HBaseConfig> clusters = HBaseUI.getHbaseManager().getClusters();
        HBaseConfig clusterInfo = (HBaseConfig) clusterCombo.getValue();
        clusterCombo.removeAllItems();
        if (clusters != null && clusters.size() > 0) {
            for (HBaseConfig info : clusters) {
                clusterCombo.addItem(info);
                clusterCombo.setItemCaption(info,
                        info.getClusterName());
            }
            if (clusterInfo != null) {
                for (HBaseConfig config : clusters) {
                    if (config.getClusterName().equals(clusterInfo)) {
                        clusterCombo.setValue(config);
                        return;
                    }
                }
            } else {
                clusterCombo.setValue(clusters.iterator().next());
            }
        }
    }

    private Table createTableTemplate(String caption, int size) {
        final Table table = new Table(caption);
        table.addContainerProperty("Host", String.class, null);
        table.addContainerProperty("Type", HBaseType.class, null);
        table.addContainerProperty("Status", Embedded.class, null);
        table.setSizeFull();
        table.setWidth(100, Sizeable.Unit.PERCENTAGE);
        table.setHeight(size, Sizeable.Unit.PERCENTAGE);
        table.setPageLength(10);
        table.setSelectable(false);
        table.setImmediate(true);

        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    String lxcHostname = (String) table.getItem(event.getItemId()).getItemProperty("Host").getValue();
                    Agent lxcAgent = HBaseUI.getAgentManager().getAgentByHostname(lxcHostname);
                    if (lxcAgent != null) {
                        TerminalWindow terminal = new TerminalWindow(Util.wrapAgentToSet(lxcAgent), HBaseUI.getExecutor(), HBaseUI.getCommandRunner(), HBaseUI.getAgentManager());
                        contentRoot.getUI().addWindow(terminal.getWindow());
                    } else {
                        show("Agent is not connected");
                    }
                }
            }
        });
        return table;
    }


}
