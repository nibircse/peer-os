package org.safehaus.subutai.core.environment.ui.manage;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.safehaus.subutai.common.protocol.EnvironmentBlueprint;
import org.safehaus.subutai.core.environment.api.exception.EnvironmentManagerException;
import org.safehaus.subutai.core.environment.api.helper.EnvironmentBuildProcess;
import org.safehaus.subutai.core.environment.api.helper.ProcessStatusEnum;
import org.safehaus.subutai.core.environment.ui.EnvironmentManagerPortalModule;
import org.safehaus.subutai.core.environment.ui.executor.build.BuildCommandFactory;
import org.safehaus.subutai.core.environment.ui.executor.build.BuildProcessExecutionEvent;
import org.safehaus.subutai.core.environment.ui.executor.build.BuildProcessExecutionEventType;
import org.safehaus.subutai.core.environment.ui.executor.build.BuildProcessExecutionListener;
import org.safehaus.subutai.core.environment.ui.executor.build.BuildProcessExecutor;
import org.safehaus.subutai.core.environment.ui.executor.build.BuildProcessExecutorImpl;
import org.safehaus.subutai.core.environment.ui.text.EnvAnswer;
import org.safehaus.subutai.core.network.api.Tunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.FieldEvents;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


@SuppressWarnings( "serial" )
public class EnvironmentsBuildProcessForm implements BuildProcessExecutionListener
{

    private static final String OK_ICON_SOURCE = "img/ok.png";
    private static final String ERROR_ICON_SOURCE = "img/cancel.png";
    private static final String LOAD_ICON_SOURCE = "img/spinner.gif";
    private static final String STATUS = "Status";
    private static final String ACTION = "Action";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LoggerFactory.getLogger( EnvironmentsBuildProcessForm.class );

    private Map<UUID, ExecutorService> executorServiceMap = new HashMap<>();
    private VerticalLayout contentRoot;
    private Table environmentsTable;
    private EnvironmentManagerPortalModule module;
    private Button environmentsButton;


    public EnvironmentsBuildProcessForm( final EnvironmentManagerPortalModule module )
    {
        this.module = module;

        contentRoot = new VerticalLayout();
        contentRoot.setSpacing( true );
        contentRoot.setMargin( true );

        environmentsTable = createTable( "Environments Build Process", 300 );
        environmentsTable.setId( "environmentsTable" );

        environmentsButton = new Button( "View" );
        environmentsButton.setId( "environmentsButton" );
        environmentsButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                updateTableData();
            }
        } );
        contentRoot.addComponent( environmentsButton );
        contentRoot.addComponent( environmentsTable );
    }


    private Table createTable( String caption, int size )
    {
        Table table = new Table( caption );
        table.addContainerProperty( "Name", String.class, null );
        table.addContainerProperty( STATUS, Embedded.class, null );
        table.addContainerProperty( "Info", Button.class, null );
        table.addContainerProperty( "Networking", Button.class, null );
        table.addContainerProperty( ACTION, Button.class, null );
        table.addContainerProperty( "Destroy", Button.class, null );
        table.setPageLength( 10 );
        table.setSelectable( false );
        table.setEnabled( true );
        table.setImmediate( true );
        table.setSizeFull();
        return table;
    }


    private void updateTableData()
    {
        environmentsTable.removeAllItems();
        List<EnvironmentBuildProcess> processList = module.getEnvironmentManager().getBuildProcesses();
        if ( !processList.isEmpty() )
        {
            for ( final EnvironmentBuildProcess process : processList )
            {
                addProcessToTable( process );
            }
        }
        else
        {
            Notification.show( EnvAnswer.NO_BUILD_PROCESS.getAnswer() );
        }
        environmentsTable.refreshRowCache();
    }


    public void addProcessToTable( final EnvironmentBuildProcess process )
    {
        String status = null;
        Button viewButton = new Button( "Info" );
        viewButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                Window window = genProcessWindow( process );
                window.setVisible( true );
            }
        } );

        Button networkBtn = new Button( "Network settings" );
        networkBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                Window window = genNetworkConfigurationWindow( process );
                window.setVisible( true );
            }
        } );

        Button processButton = null;
        Embedded icon = null;

        switch ( process.getProcessStatusEnum() )
        {
            case NEW_PROCESS:
                processButton = new Button( "Build" );
                status = "ok";
                icon = new Embedded( "", new ThemeResource( OK_ICON_SOURCE ) );
                processButton.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent clickEvent )
                    {
                        startBuildProcess( process );
                    }
                } );

                break;
            case IN_PROGRESS:
                processButton = new Button( "Terminate" );
                status = "indicator";
                icon = new Embedded( "", new ThemeResource( LOAD_ICON_SOURCE ) );
                processButton.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( final Button.ClickEvent clickEvent )
                    {
                        terminateBuildProcess( process );
                    }
                } );
                break;
            case FAILED:
                status = "error";
                icon = new Embedded( "", new ThemeResource( ERROR_ICON_SOURCE ) );
                break;
            case TERMINATED:
                status = "error";
                icon = new Embedded( "", new ThemeResource( ERROR_ICON_SOURCE ) );
                break;
            case SUCCESSFUL:
                status = "ok";
                icon = new Embedded( "", new ThemeResource( OK_ICON_SOURCE ) );
                break;
            default:
                throw new AssertionError( process.getProcessStatusEnum().name() );
        }

        Button destroyButton = new Button( "Destroy" );
        destroyButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                destroyBuildProcess( process );
                environmentsButton.click();
            }
        } );

        EnvironmentBlueprint bp;
        try
        {
            bp = module.getEnvironmentManager().getEnvironmentBlueprint( process.getBlueprintId() );
        }
        catch ( EnvironmentManagerException e )
        {
            Notification.show( e.getMessage() );
            return;
        }
        if ( bp != null )
        {
            icon.setId( bp.getName() + "-" + status );
            viewButton.setId( bp.getName() + "-view" );
            if ( processButton != null )
            {
                processButton.setId( bp.getName() + "-process" );
            }
            destroyButton.setId( bp.getName() + "-destroy" );

            environmentsTable.addItem( new Object[]
            {
                bp.getName(), icon, viewButton, networkBtn, processButton, destroyButton
            }, process.getId() );
        }
        else
        {
            LOGGER.error( "Blueprint not found id=" + process.getBlueprintId() );
        }
    }


    private Window genProcessWindow( final EnvironmentBuildProcess process )
    {
        Window window = createWindow( "Environment details" );
        TextArea area = new TextArea();
        area.setSizeFull();
        area.setValue( GSON.toJson( process ) );
        window.setContent( area );
        contentRoot.getUI().addWindow( window );
        return window;
    }


    private Window genNetworkConfigurationWindow( final EnvironmentBuildProcess process )
    {
        Window window = createWindow( "Network settings" );
        Layout layout = new VerticalLayout();
        window.setContent( layout );

        final N2NConnectionImpl n2n = N2NConnectionImpl.newCopy( process.getN2nConnection() );
        final TunnelImpl tunnel = TunnelImpl.newCopy( process.getTunnel() );
        process.setN2nConnection( n2n );
        process.setTunnel( tunnel );

        TextField txtSuperNodeIp = new TextField( "Super node IP" );
        txtSuperNodeIp.setValue( n2n.superNodeIp );
        txtSuperNodeIp.addTextChangeListener( new FieldEvents.TextChangeListener()
        {
            @Override
            public void textChange( FieldEvents.TextChangeEvent event )
            {
                n2n.superNodeIp = event.getText();
            }
        } );
        layout.addComponent( txtSuperNodeIp );

        TextField txtSuperNodePort = new TextField( "Super node port" );
        txtSuperNodePort.setValue( Integer.toString( n2n.superNodePort ) );
        txtSuperNodePort.addTextChangeListener( new FieldEvents.TextChangeListener()
        {
            @Override
            public void textChange( FieldEvents.TextChangeEvent event )
            {
                try
                {
                    n2n.superNodePort = Integer.valueOf( event.getText() );
                }
                catch ( NumberFormatException ex )
                {
                    Notification.show( "Invalid super node port value" );
                }
            }
        } );
        layout.addComponent( txtSuperNodePort );

        TextField txtLocalIp = new TextField( "Local IP" );
        txtLocalIp.setValue( n2n.localIp );
        txtLocalIp.addTextChangeListener( new FieldEvents.TextChangeListener()
        {
            @Override
            public void textChange( FieldEvents.TextChangeEvent event )
            {
                n2n.localIp = event.getText();
            }
        } );
        layout.addComponent( txtLocalIp );

        TextField txtInterfaceName = new TextField( "Interface name" );
        txtInterfaceName.setValue( n2n.interfaceName );
        txtInterfaceName.addTextChangeListener( new FieldEvents.TextChangeListener()
        {
            @Override
            public void textChange( FieldEvents.TextChangeEvent event )
            {
                n2n.interfaceName = event.getText();
            }
        } );
        layout.addComponent( txtInterfaceName );

        TextField txtCommunityName = new TextField( "Community name" );
        txtCommunityName.setValue( n2n.communityName );
        txtCommunityName.addTextChangeListener( new FieldEvents.TextChangeListener()
        {
            @Override
            public void textChange( FieldEvents.TextChangeEvent event )
            {
                n2n.communityName = event.getText();
            }
        } );
        layout.addComponent( txtCommunityName );

        TextField txtKeyFilePath = new TextField( "Key file path" );
        txtKeyFilePath.setValue( process.getKeyFilePath() );
        txtKeyFilePath.addTextChangeListener( new FieldEvents.TextChangeListener()
        {
            @Override
            public void textChange( FieldEvents.TextChangeEvent event )
            {
                process.setKeyFilePath( event.getText() );
            }
        } );
        layout.addComponent( txtKeyFilePath );

        TextField txtTunnelName = new TextField( "Tunnel name" );
        txtTunnelName.setValue( process.getTunnel().getTunnelName() );
        txtTunnelName.addTextChangeListener( new FieldEvents.TextChangeListener()
        {
            @Override
            public void textChange( FieldEvents.TextChangeEvent event )
            {
                tunnel.tunnelName = event.getText();
            }
        } );
        layout.addComponent( txtTunnelName );

        contentRoot.getUI().addWindow( window );
        return window;
    }


    private Window createWindow( String caption )
    {
        Window window = new Window();
        window.setCaption( caption );
        window.setWidth( "800px" );
        window.setHeight( "500px" );
        window.setModal( true );
        window.setClosable( true );
        return window;
    }


    private void terminateBuildProcess( final EnvironmentBuildProcess environmentBuildProcess )
    {

        UUID uuid = UUID.fromString( environmentBuildProcess.getId().toString() );
        if ( executorServiceMap.containsKey( uuid ) )
        {
            ExecutorService executorService = executorServiceMap.get( uuid );
            if ( !executorService.isTerminated() )
            {
                executorService.shutdown();
                executorServiceMap.remove( uuid );
                environmentBuildProcess.setProcessStatusEnum( ProcessStatusEnum.TERMINATED );
                try
                {
                    module.getEnvironmentManager().saveBuildProcess( environmentBuildProcess );
                    Notification.show( "Saved" );
                }
                catch ( EnvironmentManagerException e )
                {
                    Notification.show( e.toString() );
                }
            }
            Notification.show( "Terminated" );
        }
        else
        {
            Notification.show( "No such process" );
            //TODO: check build process actual state in db and/or environments
        }
    }


    private void destroyBuildProcess( final EnvironmentBuildProcess environmentBuildProcess )
    {
        module.getEnvironmentManager().deleteBuildProcess( environmentBuildProcess );
    }


    public void startBuildProcess( final EnvironmentBuildProcess process )
    {
        N2NConnectionImpl n2n = ( N2NConnectionImpl ) process.getN2nConnection();
        Tunnel t = process.getTunnel();
        if ( n2n == null || !n2n.hasAllValues() || t == null || t.getTunnelName() == null )
        {
            Notification.show( "Network settings are not complete" );
            return;
        }

        BuildProcessExecutor buildProcessExecutor = new BuildProcessExecutorImpl( process );
        buildProcessExecutor.addListener( this );
        ExecutorService executor = Executors.newCachedThreadPool();

        buildProcessExecutor.execute( executor, new BuildCommandFactory( module.getEnvironmentManager(), process ) );
        executor.shutdown();
    }


    @Override
    public void onExecutionEvent( final BuildProcessExecutionEvent event )
    {
        updateEnvironmentsTableStatus( event );
    }


    private void updateEnvironmentsTableStatus( final BuildProcessExecutionEvent event )
    {
        Notification.show( event.getExceptionMessage() );
        contentRoot.getUI().access( new Runnable()
        {
            @Override
            public void run()
            {
                Item row = environmentsTable.getItem( event.getEnvironmentBuildProcess().getId() );
                if ( row != null )
                {
                    Property p = row.getItemProperty( STATUS );
                    Button actionBtn = ( Button ) row.getItemProperty( ACTION ).getValue();
                    if ( BuildProcessExecutionEventType.START.equals( event.getEventType() ) )
                    {
                        actionBtn.setEnabled( false );
                        Embedded indicator = new Embedded( "", new ThemeResource( LOAD_ICON_SOURCE ) );
                        indicator.setId( "indicator" );
                        p.setValue( indicator );
                    }
                    else if ( BuildProcessExecutionEventType.SUCCESS.equals( event.getEventType() ) )
                    {
                        p.setValue( new Embedded( "", new ThemeResource( OK_ICON_SOURCE ) ) );
                    }
                    else if ( BuildProcessExecutionEventType.FAIL.equals( event.getEventType() ) )
                    {
                        Embedded error = new Embedded( "", new ThemeResource( ERROR_ICON_SOURCE ) );
                        p.setValue( error );
                        error.setId( "error" );
                    }
                }
            }
        } );
    }


    public VerticalLayout getContentRoot()
    {
        return this.contentRoot;
    }
}

