package io.subutai.core.hubmanager.impl;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.subutai.common.dao.DaoManager;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.executor.api.CommandExecutor;
import io.subutai.core.hubmanager.api.HubManager;
import io.subutai.core.hubmanager.api.HubPluginException;
import io.subutai.core.hubmanager.api.StateLinkProcessor;
import io.subutai.core.hubmanager.api.dao.ConfigDataService;
import io.subutai.core.hubmanager.api.model.Config;
import io.subutai.core.hubmanager.impl.appscale.AppScaleManager;
import io.subutai.core.hubmanager.impl.appscale.AppScaleProcessor;
import io.subutai.core.hubmanager.impl.dao.ConfigDataServiceImpl;
import io.subutai.core.hubmanager.impl.processor.ContainerEventProcessor;
import io.subutai.core.hubmanager.impl.processor.EnvironmentUserHelper;
import io.subutai.core.hubmanager.impl.processor.HeartbeatProcessor;
import io.subutai.core.hubmanager.impl.processor.HubEnvironmentProcessor;
import io.subutai.core.hubmanager.impl.processor.HubLoggerProcessor;
import io.subutai.core.hubmanager.impl.processor.ProductProcessor;
import io.subutai.core.hubmanager.impl.processor.ResourceHostDataProcessor;
import io.subutai.core.hubmanager.impl.processor.ResourceHostMonitorProcessor;
import io.subutai.core.hubmanager.impl.processor.SystemConfProcessor;
import io.subutai.core.hubmanager.impl.processor.VehsProcessor;
import io.subutai.core.hubmanager.impl.tunnel.TunnelProcessor;
import io.subutai.core.identity.api.IdentityManager;
import io.subutai.core.metric.api.Monitor;
import io.subutai.core.network.api.NetworkManager;
import io.subutai.core.peer.api.PeerManager;
import io.subutai.core.security.api.SecurityManager;
import io.subutai.hub.share.dto.PeerDto;
import io.subutai.hub.share.dto.SystemConfDto;
import io.subutai.hub.share.dto.product.ProductsDto;
import io.subutai.hub.share.json.JsonUtil;


// TODO: Replace WebClient with HubRestClient.
public class HubManagerImpl implements HubManager
{
    private static final long TIME_15_MINUTES = 900;

    private static final Logger LOG = LoggerFactory.getLogger( HubManagerImpl.class );

    private SecurityManager securityManager;

    private EnvironmentManager environmentManager;

    private PeerManager peerManager;

    private ConfigManager configManager;

    private CommandExecutor commandExecutor;

    private ScheduledExecutorService hearbeatExecutorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledExecutorService resourceHostConfExecutorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledExecutorService resourceHostMonitorExecutorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledExecutorService hubLoggerExecutorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledExecutorService containerEventExecutor = Executors.newSingleThreadScheduledExecutor();

    private HeartbeatProcessor heartbeatProcessor;

    private ResourceHostDataProcessor resourceHostDataProcessor;

    private ResourceHostMonitorProcessor resourceHostMonitorProcessor;

    private HubLoggerProcessor hubLoggerProcessor;

    private DaoManager daoManager;

    private ConfigDataService configDataService;

    private Monitor monitor;

    private IdentityManager identityManager;

    private HubEnvironmentManager hubEnvironmentManager;

    private NetworkManager networkManager;

    private ContainerEventProcessor containerEventProcessor;

    private ProductProcessor productProccessor;

    private ScheduledExecutorService sumChecker = Executors.newSingleThreadScheduledExecutor();

    private final ExecutorService asyncHeartbeatExecutor = Executors.newFixedThreadPool( 10 );

    private String checksum = "";


    public HubManagerImpl( DaoManager daoManager )
    {
        this.daoManager = daoManager;
    }


    public void init()
    {
        try
        {
            configDataService = new ConfigDataServiceImpl( daoManager );

            configManager = new ConfigManager( securityManager, peerManager, configDataService, identityManager );

            hubEnvironmentManager =
                    new HubEnvironmentManager( environmentManager, configManager, peerManager, identityManager,
                            networkManager );

            heartbeatProcessor = new HeartbeatProcessor( this, configManager );

            resourceHostDataProcessor = new ResourceHostDataProcessor( this, peerManager, configManager, monitor );

            hubLoggerProcessor = new HubLoggerProcessor( configManager, this );

            resourceHostMonitorProcessor =
                    new ResourceHostMonitorProcessor( this, peerManager, configManager, monitor );

            productProccessor = new ProductProcessor( configManager );

            StateLinkProcessor systemConfProcessor = new SystemConfProcessor( configManager );

            EnvironmentUserHelper environmentUserHelper =
                    new EnvironmentUserHelper( configManager, identityManager, configDataService, environmentManager );

            StateLinkProcessor hubEnvironmentProccessor =
                    new HubEnvironmentProcessor( hubEnvironmentManager, configManager, peerManager, identityManager,
                            commandExecutor, environmentUserHelper );

            StateLinkProcessor vehsProccessor =
                    new VehsProcessor( hubEnvironmentManager, configManager, peerManager, commandExecutor,
                            environmentUserHelper );

            StateLinkProcessor tunnelProcessor = new TunnelProcessor( peerManager, configManager );

            heartbeatProcessor.addProcessor( vehsProccessor );
            heartbeatProcessor.addProcessor( hubEnvironmentProccessor );
            heartbeatProcessor.addProcessor( systemConfProcessor );
            heartbeatProcessor.addProcessor( productProccessor );

            AppScaleProcessor appScaleProcessor =
                    new AppScaleProcessor( configManager, new AppScaleManager( peerManager ) );

            heartbeatProcessor.addProcessor( appScaleProcessor );

            heartbeatProcessor.addProcessor( tunnelProcessor );

            hearbeatExecutorService.scheduleWithFixedDelay( heartbeatProcessor, 10, 60, TimeUnit.SECONDS );

            resourceHostConfExecutorService
                    .scheduleWithFixedDelay( resourceHostDataProcessor, 20, TIME_15_MINUTES, TimeUnit.SECONDS );

            resourceHostMonitorExecutorService
                    .scheduleWithFixedDelay( resourceHostMonitorProcessor, 30, 300, TimeUnit.SECONDS );

            containerEventProcessor = new ContainerEventProcessor( this, configManager, peerManager );

            containerEventExecutor
                    .scheduleWithFixedDelay( containerEventProcessor, 30, TIME_15_MINUTES, TimeUnit.SECONDS );

            hubLoggerExecutorService.scheduleWithFixedDelay( hubLoggerProcessor, 40, 3600, TimeUnit.SECONDS );

            this.sumChecker.scheduleWithFixedDelay( new Runnable()
            {
                @Override
                public void run()
                {
                    LOG.info( "Starting sumchecker" );
                    generateChecksum();
                }
            }, 1, 600000, TimeUnit.MILLISECONDS );
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage() );
        }
    }


    public void destroy()
    {
        hearbeatExecutorService.shutdown();
        resourceHostConfExecutorService.shutdown();
        resourceHostMonitorExecutorService.shutdown();
    }


    @Override
    public void sendHeartbeat() throws HubPluginException
    {
        resourceHostDataProcessor.process();
        heartbeatProcessor.sendHeartbeat();
        containerEventProcessor.process();
    }


    /**
     * Called by Hub to trigger heartbeat on peer
     */
    @Override
    public void triggerHeartbeat()
    {
        asyncHeartbeatExecutor.execute( new Runnable()
        {
            public void run()
            {
                try
                {
                    heartbeatProcessor.sendHeartbeat();
                }
                catch ( HubPluginException e )
                {
                    e.printStackTrace();
                }
            }
        } );
    }


    @Override
    public void sendResourceHostInfo() throws HubPluginException
    {
        resourceHostDataProcessor.process();
    }


    @Override
    public void registerPeer( String hupIp, String email, String password ) throws HubPluginException
    {
        configManager.addHubConfig( hupIp );

        RegistrationManager registrationManager = new RegistrationManager( this, configManager, hupIp );

        registrationManager.registerPeer( email, password );

        generateChecksum();
    }


    @Override
    public void unregisterPeer() throws HubPluginException
    {
        RegistrationManager registrationManager = new RegistrationManager( this, configManager, null );

        registrationManager.unregister();
    }


    @Override
    public String getHubDns() throws HubPluginException
    {
        Config config = getConfigDataService().getHubConfig( configManager.getPeerId() );

        if ( config != null )
        {
            return config.getHubIp();
        }
        else
        {
            return null;
        }
    }


    @Override
    public String getProducts() throws HubPluginException
    {
        try
        {
            WebClient client = configManager
                    .getTrustedWebClientWithAuth( "/rest/v1.2/marketplace/products/public", "hub.subut.ai" );

            Response r = client.get();

            if ( r.getStatus() == HttpStatus.SC_NO_CONTENT )
            {
                return null;
            }

            if ( r.getStatus() != HttpStatus.SC_OK )
            {
                LOG.error( r.readEntity( String.class ) );
                return null;
            }

            String result = r.readEntity( String.class );
            ProductsDto productsDto = new ProductsDto( result );
            return JsonUtil.toJson( productsDto );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new HubPluginException( "Could not retrieve product data", e );
        }
    }


    @Override
    public void installPlugin( String url, String name ) throws HubPluginException
    {
        try
        {
            File file =
                    new File( String.format( "%s/deploy", System.getProperty( "karaf.home" ) ) + "/" + name + ".kar" );
            URL website = new URL( url );
            FileUtils.copyURLToFile( website, file );
        }
        catch ( IOException e )
        {
            throw new HubPluginException( "Could not install plugin", e );
        }
        LOG.debug( "Product installed successfully..." );
    }


    @Override
    public void uninstallPlugin( final String name )
    {
        File file = new File( String.format( "%s/deploy", System.getProperty( "karaf.home" ) ) + "/" + name + ".kar" );
        LOG.info( String.format( "%s/deploy", System.getProperty( "karaf.home" ) ) + "/" + name + ".kar" );
        File repo = new File( "/opt/subutai-mng/system/io/subutai/" );
        File[] dirs = repo.listFiles( new FileFilter()
        {
            @Override
            public boolean accept( File pathname )
            {
                return pathname.getName().matches( ".*" + name.toLowerCase() + ".*" );
            }
        } );
        if ( dirs != null )
        {
            for ( File f : dirs )
            {
                LOG.info( f.getAbsolutePath() );
                try
                {
                    FileUtils.deleteDirectory( f );
                    LOG.debug( f.getName() + " is removed." );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
        if ( file.delete() )
        {
            LOG.debug( file.getName() + " is removed." );
        }
        LOG.debug( "Product uninstalled successfully..." );
    }


    @Override
    public boolean getRegistrationState()
    {
        return getConfigDataService().getHubConfig( configManager.getPeerId() ) != null;
    }


    @Override
    public Map<String, String> getPeerInfo() throws HubPluginException
    {
        Map<String, String> result = new HashMap<>();
        try
        {
            String path = "/rest/v1/peers/" + configManager.getPeerId();

            WebClient client = configManager.getTrustedWebClientWithAuth( path, configManager.getHubIp() );

            Response r = client.get();

            if ( r.getStatus() == HttpStatus.SC_OK )
            {
                byte[] encryptedContent = configManager.readContent( r );
                byte[] plainContent = configManager.getMessenger().consume( encryptedContent );
                PeerDto dto = JsonUtil.fromCbor( plainContent, PeerDto.class );
                result.put( "OwnerId", dto.getOwnerId() );

                LOG.debug( "PeerDto: " + result.toString() );
            }
        }
        catch ( Exception e )
        {
            throw new HubPluginException( "Could not retrieve Peer info", e );
        }
        return result;
    }


    public CommandExecutor getCommandExecutor()
    {
        return commandExecutor;
    }


    public void setCommandExecutor( final CommandExecutor commandExecutor )
    {
        this.commandExecutor = commandExecutor;
    }


    @Override
    public Config getHubConfiguration()
    {
        return configDataService.getHubConfig( configManager.getPeerId() );
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        this.environmentManager = environmentManager;
    }


    public void setSecurityManager( final SecurityManager securityManager )
    {
        this.securityManager = securityManager;
    }


    public void setPeerManager( final PeerManager peerManager )
    {
        this.peerManager = peerManager;
    }


    public ConfigDataService getConfigDataService()
    {
        return configDataService;
    }


    public void setConfigDataService( final ConfigDataService configDataService )
    {
        this.configDataService = configDataService;
    }


    public void setMonitor( Monitor monitor )
    {
        this.monitor = monitor;
    }


    private static ObjectMapper createMapper( JsonFactory factory )
    {
        ObjectMapper mapper = new ObjectMapper( factory );
        mapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        return mapper;
    }


    public void setIdentityManager( final IdentityManager identityManager )
    {
        this.identityManager = identityManager;
    }


    public void setNetworkManager( final NetworkManager networkManager )
    {
        this.networkManager = networkManager;
    }


    private void generateChecksum()
    {
        try
        {
            LOG.info( "Generating plugins list md5 checksum" );
            String productList = getProducts();
            MessageDigest md = MessageDigest.getInstance( "MD5" );
            byte[] bytes = md.digest( productList.getBytes( "UTF-8" ) );
            StringBuilder hexString = new StringBuilder();

            for ( int i = 0; i < bytes.length; i++ )
            {
                String hex = Integer.toHexString( 0xFF & bytes[i] );
                if ( hex.length() == 1 )
                {
                    hexString.append( '0' );
                }
                hexString.append( hex );
            }

            checksum = hexString.toString();
            LOG.info( "Checksum generated: " + checksum );
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage() );
            e.printStackTrace();
        }
    }


    @Override
    public String getChecksum()
    {
        return this.checksum;
    }


    @Override
    public void sendSystemConfiguration( final SystemConfDto dto )
    {
        if ( getRegistrationState() )
        {
            try
            {
                String path = "/rest/v1/system-changes";
                WebClient client = configManager.getTrustedWebClientWithAuth( path, configManager.getHubIp() );

                byte[] cborData = JsonUtil.toCbor( dto );

                byte[] encryptedData = configManager.getMessenger().produce( cborData );

                LOG.info( "Sending Configuration of SS to Hub..." );

                Response r = client.post( encryptedData );

                if ( r.getStatus() == HttpStatus.SC_NO_CONTENT )
                {
                    LOG.info( "SS configuration sent successfully." );
                }
                else
                {
                    LOG.error( "Could not send SS configuration to Hub: ", r.readEntity( String.class ) );
                }
            }
            catch ( Exception e )
            {
                LOG.error( "Could not send SS configuration to Hub", e );
            }
        }
    }
}