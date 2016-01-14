package io.subutai.core.peer.impl;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;

import io.subutai.common.peer.PeerException;
import io.subutai.common.peer.PeerInfo;
import io.subutai.common.peer.RegistrationData;
import io.subutai.common.util.RestUtil;
import io.subutai.core.peer.api.RegistrationClient;


/**
 * REST client implementation of registration process
 */
public class RegistrationClientImpl implements RegistrationClient
{
    protected RestUtil restUtil = new RestUtil();
    private static final String urlTemplate = "https://%s:8443/rest/v1/handshake/%s";
    private Object provider;


    public RegistrationClientImpl( final Object provider )
    {
        this.provider = provider;
    }


    @Override
    public PeerInfo getPeerInfo( final String destinationHost ) throws PeerException
    {

        WebClient client = restUtil.getTrustedWebClient( buildUrl( destinationHost, "/info" ), provider );

        client.type( MediaType.APPLICATION_JSON );
        client.accept( MediaType.APPLICATION_JSON );

        try
        {
            Response response = client.get();

            if ( response.getStatus() != Response.Status.OK.getStatusCode() )
            {
                throw new PeerException(
                        String.format( "Remote peer '%s' return %d error code. Please try again later.",
                                destinationHost, response.getStatus() ) );
            }
            else
            {
                return response.readEntity( PeerInfo.class );
            }
        }
        catch ( Exception e )
        {
            throw new PeerException(
                    String.format( "Error requesting remote peer '%s': %s .Please try again later.", destinationHost,
                            e.getMessage() ) );
        }
    }


    @Override
    public RegistrationData sendInitRequest( final String destinationHost, final RegistrationData registrationData )
            throws PeerException
    {
        WebClient client = restUtil.getTrustedWebClient( buildUrl( destinationHost, "/register" ), provider );

        client.type( MediaType.APPLICATION_JSON );
        client.accept( MediaType.APPLICATION_JSON );

        try
        {
            Response response = client.post( registrationData );
            if ( response.getStatus() != Response.Status.OK.getStatusCode() )
            {
                throw new PeerException(
                        String.format( "Remote peer '%s' return %d error code. Please try again later.",
                                destinationHost, response.getStatus() ) );
            }
            else
            {
                return response.readEntity( RegistrationData.class );
            }
        }
        catch ( Exception e )
        {
            throw new PeerException(
                    String.format( "Error requesting remote peer '%s': %s .Please try again later.", destinationHost,
                            e.getMessage() ) );
        }
    }


    @Override
    public void sendCancelRequest( String destinationHost, final RegistrationData registrationData )
            throws PeerException
    {
        WebClient client = restUtil.getTrustedWebClient( buildUrl( destinationHost, "/cancel" ), provider );

        client.type( MediaType.APPLICATION_JSON );
        client.accept( MediaType.APPLICATION_JSON );

        try
        {

            Response response = client.post( registrationData );
            if ( response.getStatus() != Response.Status.OK.getStatusCode() )
            {
                throw new PeerException(
                        "Error on sending cancel registration request: " + response.readEntity( String.class ) );
            }
        }
        catch ( Exception e )
        {
            throw new PeerException(
                    String.format( "Error requesting remote peer '%s': %s .Please try again later.", destinationHost,
                            e.getMessage() ) );
        }
    }


    @Override
    public void sendRejectRequest( final String destinationHost, final RegistrationData registrationData )
            throws PeerException
    {
        WebClient client = restUtil.getTrustedWebClient( buildUrl( destinationHost, "/reject" ), provider );

        client.type( MediaType.APPLICATION_JSON );
        client.accept( MediaType.APPLICATION_JSON );

        try
        {
            Response response = client.post( registrationData );
            if ( response.getStatus() != Response.Status.OK.getStatusCode() )
            {
                throw new PeerException( "Remote exception: " + response.readEntity( String.class ) );
            }
        }
        catch ( Exception e )
        {
            throw new PeerException(
                    String.format( "Error requesting remote peer '%s': %s .Please try again later.", destinationHost,
                            e.getMessage() ) );
        }
    }


    @Override
    public void sendUnregisterRequest( final String destinationHost, final RegistrationData registrationData )
            throws PeerException
    {
        WebClient client = restUtil.getTrustedWebClient( buildUrl( destinationHost, "/unregister" ), provider );

        client.type( MediaType.APPLICATION_JSON );
        client.accept( MediaType.APPLICATION_JSON );

        try
        {
            Response response = client.post( registrationData );
            if ( response.getStatus() != Response.Status.OK.getStatusCode() )
            {
                throw new PeerException( "Remote exception: " + response.readEntity( String.class ) );
            }
        }
        catch ( Exception e )
        {
            throw new PeerException(
                    String.format( "Error requesting remote peer '%s': %s .Please try again later.", destinationHost,
                            e.getMessage() ) );
        }
    }


    @Override
    public void sendApproveRequest( String destinationHost, final RegistrationData registrationData )
            throws PeerException
    {
        WebClient client = restUtil.getTrustedWebClient( buildUrl( destinationHost, "/approve" ), provider );

        client.type( MediaType.APPLICATION_JSON );
        client.accept( MediaType.APPLICATION_JSON );

        try
        {
            Response response = client.post( registrationData );
            if ( response.getStatus() != Response.Status.OK.getStatusCode() )
            {
                throw new PeerException( "Remote exception: " + response.readEntity( String.class ) );
            }
        }
        catch ( Exception e )
        {
            throw new PeerException(
                    String.format( "Error requesting remote peer '%s': %s .Please try again later.", destinationHost,
                            e.getMessage() ) );
        }
    }


    private String buildUrl( String destination, String action )
    {
        return String.format( urlTemplate, destination, action );
    }
}
