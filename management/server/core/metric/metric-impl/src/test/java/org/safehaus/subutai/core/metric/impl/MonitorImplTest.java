package org.safehaus.subutai.core.metric.impl;


import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.safehaus.subutai.common.exception.CommandException;
import org.safehaus.subutai.common.exception.DaoException;
import org.safehaus.subutai.common.protocol.CommandResult;
import org.safehaus.subutai.common.protocol.RequestBuilder;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.messenger.api.Message;
import org.safehaus.subutai.core.messenger.api.MessageException;
import org.safehaus.subutai.core.messenger.api.Messenger;
import org.safehaus.subutai.core.metric.api.ContainerHostMetric;
import org.safehaus.subutai.core.metric.api.MetricListener;
import org.safehaus.subutai.core.metric.api.MonitorException;
import org.safehaus.subutai.core.metric.api.ResourceHostMetric;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.core.peer.api.LocalPeer;
import org.safehaus.subutai.core.peer.api.Peer;
import org.safehaus.subutai.core.peer.api.PeerException;
import org.safehaus.subutai.core.peer.api.PeerManager;
import org.safehaus.subutai.core.peer.api.RemotePeer;
import org.safehaus.subutai.core.peer.api.ResourceHost;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test for MonitorImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class MonitorImplTest
{
    private static final String SUBSCRIBER_ID = "subscriber";
    private static final String RESOURCE_HOST = "resource";
    private static final UUID ENVIRONMENT_ID = UUID.randomUUID();
    private static final UUID LOCAL_PEER_ID = UUID.randomUUID();
    private static final UUID REMOTE_PEER_ID = UUID.randomUUID();
    private static final String HOST = "test";
    private static final double METRIC_VALUE = 123;
    private static final String METRIC_JSON = " {\"host\":\"test\", \"totalRam\":\"123\"," +
            "\"availableRam\":\"123\", \"usedRam\":\"123\", \"cpuLoad5\":\"123\","
            + "  \"availableDisk\" : \"123\", \"usedDisk\" : \"123\", \"totalDisk\" : \"123\"}";
    @Mock
    DataSource dataSource;
    @Mock
    PeerManager peerManager;
    @Mock
    MonitorDao monitorDao;
    @Mock
    ContainerHostMetric containerHostMetric;
    @Mock
    MetricListener metricListener;
    Set<MetricListener> metricListeners;
    MonitorImplExt monitor;
    @Mock
    ExecutorService notificationService;
    @Mock
    Environment environment;
    @Mock
    LocalPeer localPeer;
    @Mock
    RemotePeer remotePeer;
    @Mock
    Messenger messenger;
    @Mock
    ContainerHost containerHost;
    @Mock
    ResourceHost resourceHost;


    static class MonitorImplExt extends MonitorImpl
    {
        public MonitorImplExt( final DataSource dataSource, final PeerManager peerManager, final Messenger messenger )
                throws DaoException
        {
            super( dataSource, peerManager, messenger );
        }


        public void setMonitorDao( MonitorDao monitorDao ) {this.monitorDao = monitorDao;}


        public void setNotificationExecutor( ExecutorService executor ) {this.notificationExecutor = executor;}


        public void setMetricListeners( Set<MetricListener> metricListeners ) {this.metricListeners = metricListeners;}
    }


    @Before
    public void setUp() throws Exception
    {
        Connection connection = mock( Connection.class );
        PreparedStatement preparedStatement = mock( PreparedStatement.class );
        when( connection.prepareStatement( anyString() ) ).thenReturn( preparedStatement );
        when( dataSource.getConnection() ).thenReturn( connection );
        monitor = new MonitorImplExt( dataSource, peerManager, messenger );
        monitor.setMonitorDao( monitorDao );

        containerHostMetric = mock( ContainerHostMetric.class );
        when( containerHostMetric.getEnvironmentId() ).thenReturn( ENVIRONMENT_ID );
        when( metricListener.getSubscriberId() ).thenReturn( SUBSCRIBER_ID );
        metricListeners = Sets.newHashSet( metricListener );
        monitor.setMetricListeners( metricListeners );
        monitor.setNotificationExecutor( notificationService );
        when( monitorDao.getEnvironmentSubscribersIds( ENVIRONMENT_ID ) )
                .thenReturn( Sets.newHashSet( SUBSCRIBER_ID ) );
        when( environment.getId() ).thenReturn( ENVIRONMENT_ID );
        when( localPeer.getId() ).thenReturn( LOCAL_PEER_ID );
        when( localPeer.isLocal() ).thenReturn( true );
        when( remotePeer.isLocal() ).thenReturn( false );
        when( peerManager.getLocalPeer() ).thenReturn( localPeer );
        when( environment.getContainers() ).thenReturn( Sets.newHashSet( containerHost ) );
        when( containerHost.getEnvironmentId() ).thenReturn( ENVIRONMENT_ID );
        when( localPeer.getResourceHosts() ).thenReturn( Sets.newHashSet( resourceHost ) );
    }


    @Test(expected = NullPointerException.class)
    public void testConstructorShouldFailOnNullDataSource() throws Exception
    {
        new MonitorImpl( null, peerManager, messenger );
    }


    @Test(expected = NullPointerException.class)
    public void testConstructorShouldFailOnNullPeerManager() throws Exception
    {
        new MonitorImpl( dataSource, null, messenger );
    }


    @Test(expected = NullPointerException.class)
    public void testConstructorShouldFailOnNullMessenger() throws Exception
    {
        new MonitorImpl( dataSource, peerManager, null );
    }


    @Test
    public void testDestroy() throws Exception
    {

        monitor.destroy();

        verify( notificationService ).shutdown();
    }


    @Test
    public void testAddMetricListener() throws Exception
    {
        Set<MetricListener> metricListeners = Sets.newHashSet();
        monitor.setMetricListeners( metricListeners );

        monitor.addMetricListener( metricListener );

        assertTrue( metricListeners.contains( metricListener ) );
    }


    @Test
    public void testRemoveMetricListener() throws Exception
    {
        monitor.removeMetricListener( metricListener );

        assertFalse( metricListeners.contains( metricListener ) );
    }


    @Test
    public void testNotify() throws Exception
    {

        ArgumentCaptor<AlertNotifier> alertNotifierArgumentCaptor = ArgumentCaptor.forClass( AlertNotifier.class );

        monitor.notifyListener( containerHostMetric, SUBSCRIBER_ID );

        verify( notificationService ).execute( alertNotifierArgumentCaptor.capture() );
        assertEquals( metricListener, alertNotifierArgumentCaptor.getValue().listener );
        assertEquals( containerHostMetric, alertNotifierArgumentCaptor.getValue().metric );
    }


    @Test
    public void testAlertThresholdExcess() throws Exception
    {

        monitor.alertThresholdExcess( containerHostMetric );

        verify( containerHostMetric ).getEnvironmentId();
    }


    @Test(expected = MonitorException.class)
    public void testAlertThresholdExcessException() throws Exception
    {
        doThrow( new DaoException( null ) ).when( monitorDao ).getEnvironmentSubscribersIds( ENVIRONMENT_ID );

        monitor.alertThresholdExcess( containerHostMetric );
    }


    @Test
    public void testAlertThresholdExcessLocalPeer() throws Exception
    {
        //set owner id as local peer
        when( containerHost.getCreatorPeerId() ).thenReturn( LOCAL_PEER_ID );
        when( localPeer.getContainerHostByName( HOST ) ).thenReturn( containerHost );
        Peer ownerPeer = mock( Peer.class );
        when( peerManager.getPeer( LOCAL_PEER_ID ) ).thenReturn( ownerPeer );
        when( ownerPeer.isLocal() ).thenReturn( true );


        monitor.alertThresholdExcess( METRIC_JSON );

        verify( monitorDao ).getEnvironmentSubscribersIds( ENVIRONMENT_ID );
    }


    @Test
    public void testAlertThresholdExcessRemotePeer() throws Exception
    {
        //set owner id as local peer
        when( containerHost.getCreatorPeerId() ).thenReturn( REMOTE_PEER_ID );
        when( localPeer.getContainerHostByName( HOST ) ).thenReturn( containerHost );
        Peer ownerPeer = mock( Peer.class );
        when( peerManager.getPeer( REMOTE_PEER_ID ) ).thenReturn( ownerPeer );
        when( ownerPeer.isLocal() ).thenReturn( false );
        Message message = mock( Message.class );
        when( messenger.createMessage( any() ) ).thenReturn( message );


        monitor.alertThresholdExcess( METRIC_JSON );


        verify( messenger ).createMessage( isA( ContainerHostMetric.class ) );
        verify( messenger ).sendMessage( eq( ownerPeer ), isA( Message.class ), anyString(), anyInt() );
    }


    @Test(expected = MonitorException.class)
    public void testAlertThresholdExcessException2() throws Exception
    {

        when( localPeer.getContainerHostByName( HOST ) ).thenThrow( new PeerException( "" ) );

        monitor.alertThresholdExcess( METRIC_JSON );
    }


    @Test
    public void testStartMonitoring() throws Exception
    {

        String longSubscriberId = StringUtils.repeat( "s", 101 );
        String subscriberId = StringUtils.repeat( "s", 100 );
        when( metricListener.getSubscriberId() ).thenReturn( longSubscriberId );

        monitor.startMonitoring( metricListener, environment );

        verify( monitorDao ).addSubscription( ENVIRONMENT_ID, subscriberId );
    }


    @Test(expected = MonitorException.class)
    public void testStartMonitoringException() throws Exception
    {
        doThrow( new DaoException( null ) ).when( monitorDao ).addSubscription( ENVIRONMENT_ID, SUBSCRIBER_ID );

        monitor.startMonitoring( metricListener, environment );
    }


    @Test
    public void testStopMonitoring() throws Exception
    {

        String longSubscriberId = StringUtils.repeat( "s", 101 );
        String subscriberId = StringUtils.repeat( "s", 100 );
        when( metricListener.getSubscriberId() ).thenReturn( longSubscriberId );

        monitor.stopMonitoring( metricListener, environment );

        verify( monitorDao ).removeSubscription( ENVIRONMENT_ID, subscriberId );
    }


    @Test(expected = MonitorException.class)
    public void testStopMonitoringException() throws Exception
    {
        doThrow( new DaoException( null ) ).when( monitorDao ).removeSubscription( ENVIRONMENT_ID, SUBSCRIBER_ID );

        monitor.stopMonitoring( metricListener, environment );
    }


    @Test
    public void testGetResourceHostMetrics() throws Exception
    {
        CommandResult commandResult = mock( CommandResult.class );
        when( resourceHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( commandResult.getStdOut() ).thenReturn( METRIC_JSON );

        Set<ResourceHostMetric> metrics = monitor.getResourceHostMetrics();

        ResourceHostMetric metric = metrics.iterator().next();
        assertEquals( LOCAL_PEER_ID, metric.getPeerId() );
        assertEquals( HOST, metric.getHost() );
        assertEquals( METRIC_VALUE, metric.getTotalRam() );
    }


    @Test(expected = MonitorException.class)
    public void testGetResourceHostMetricsWithMonitorException() throws Exception
    {
        CommandResult commandResult = mock( CommandResult.class );
        when( resourceHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );
        when( commandResult.hasSucceeded() ).thenReturn( false );

        monitor.getResourceHostMetrics();
    }


    @Test(expected = MonitorException.class)
    public void testGetResourceHostMetricsWithMonitorException2() throws Exception
    {
        when( localPeer.getResourceHosts() ).thenThrow( new PeerException( "" ) );

        monitor.getResourceHostMetrics();
    }


    @Test
    public void testGetContainerHostMetrics() throws Exception
    {

        CommandResult commandResult = mock( CommandResult.class );
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( commandResult.getStdOut() ).thenReturn( METRIC_JSON );
        when( containerHost.getPeer() ).thenReturn( localPeer );
        when( containerHost.getParentHostname() ).thenReturn( RESOURCE_HOST );
        when( localPeer.getContainerHostsByEnvironmentId( ENVIRONMENT_ID ) )
                .thenReturn( Sets.newHashSet( containerHost ) );
        when( resourceHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );
        when( localPeer.getResourceHostByName( RESOURCE_HOST ) ).thenReturn( resourceHost );

        Set<ContainerHostMetric> metrics = monitor.getContainerMetrics( environment );

        ContainerHostMetric metric = metrics.iterator().next();
        assertEquals( ENVIRONMENT_ID, metric.getEnvironmentId() );
        assertEquals( HOST, metric.getHost() );
        assertEquals( METRIC_VALUE, metric.getTotalRam() );
    }


    @Test
    public void testGetContainerHostMetricsRemote() throws Exception
    {

        ContainerHostMetricResponseListener listener = mock( ContainerHostMetricResponseListener.class );
        monitor.containerHostMetricResponseListener = listener;
        when( containerHost.getPeer() ).thenReturn( remotePeer );
        ContainerHostMetricImpl expected = mock( ContainerHostMetricImpl.class );
        when( listener.waitMetrics( any( UUID.class ) ) ).thenReturn(
                Sets.<ContainerHostMetricImpl>newHashSet( expected ) );


        Set<ContainerHostMetric> metrics = monitor.getContainerMetrics( environment );

        ContainerHostMetric metric = metrics.iterator().next();
        assertEquals( expected, metric );
    }


    @Test(expected = MonitorException.class)
    public void testGetContainerHostMetricsWithException() throws Exception
    {
        when( containerHost.getPeer() ).thenReturn( remotePeer );
        doThrow( new MessageException( "" ) ).when( messenger )
                                             .sendMessage( any( Peer.class ), any( Message.class ), anyString(),
                                                     anyInt() );


        monitor.getContainerMetrics( environment );
    }


    @Test
    public void testGetLocalContainerHostMetrics() throws Exception
    {
        when( localPeer.getContainerHostsByEnvironmentId( ENVIRONMENT_ID ) )
                .thenReturn( Sets.newHashSet( containerHost ) );
        when( containerHost.getParentHostname() ).thenReturn( RESOURCE_HOST );


        monitor.getLocalContainerHostMetrics( ENVIRONMENT_ID );


        verify( containerHost, times( 2 ) ).getParentHostname();
    }


    @Test
    public void testGetLocalContainerHostMetricsWithException() throws Exception
    {
        PeerException exception = mock( PeerException.class );
        doThrow( exception ).when( localPeer ).getContainerHostsByEnvironmentId( ENVIRONMENT_ID );


        monitor.getLocalContainerHostMetrics( ENVIRONMENT_ID );


        verify( exception ).printStackTrace( any( PrintStream.class ) );
    }


    @Test
    public void testGetContainerMetrics() throws Exception
    {
        CommandResult commandResult = mock( CommandResult.class );
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( commandResult.getStdOut() ).thenReturn( METRIC_JSON );
        when( resourceHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );

        Set<ContainerHostMetricImpl> metrics = Sets.newHashSet();
        monitor.getContainerMetrics( ENVIRONMENT_ID, metrics, resourceHost, containerHost );

        ContainerHostMetric metric = metrics.iterator().next();
        assertEquals( ENVIRONMENT_ID, metric.getEnvironmentId() );
        assertEquals( HOST, metric.getHost() );
        assertEquals( METRIC_VALUE, metric.getTotalRam() );
    }


    @Test
    public void testGetContainerMetricsWithException() throws Exception
    {
        CommandResult commandResult = mock( CommandResult.class );
        when( commandResult.hasSucceeded() ).thenReturn( false );
        when( resourceHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );


        monitor.getContainerMetrics( ENVIRONMENT_ID, null, resourceHost, containerHost );

        verify( commandResult ).getStdErr();
    }


    @Test
    public void testGetContainerMetricsWithException2() throws Exception
    {
        CommandException exception = mock( CommandException.class );
        doThrow( exception ).when( resourceHost ).execute( any( RequestBuilder.class ) );


        monitor.getContainerMetrics( ENVIRONMENT_ID, null, resourceHost, containerHost );

        verify( exception ).printStackTrace( any( PrintStream.class ) );
    }
}
