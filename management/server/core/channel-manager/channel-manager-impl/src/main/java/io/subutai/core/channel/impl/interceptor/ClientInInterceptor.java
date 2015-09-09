package io.subutai.core.channel.impl.interceptor;

import java.net.URL;
import javax.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import com.google.common.base.Strings;
import io.subutai.common.settings.ChannelSettings;
import io.subutai.common.settings.Common;
import io.subutai.core.channel.impl.ChannelManagerImpl;
import io.subutai.core.channel.impl.util.MessageContentUtil;


/**
 *
 */
public class ClientInInterceptor extends AbstractPhaseInterceptor<Message>
{

    private static final Logger LOG = LoggerFactory.getLogger( ClientInInterceptor.class );
    private ChannelManagerImpl channelManagerImpl = null;

    //******************************************************************
    public ClientInInterceptor( ChannelManagerImpl channelManagerImpl )
    {
        super( Phase.RECEIVE);
        this.channelManagerImpl = channelManagerImpl;
    }
    //******************************************************************


    @Override
    public void handleMessage( final Message message )
    {
        try
        {
            if ( InterceptorState.CLIENT_IN.isActive( message ) )
            {
                LOG.info( " ****** Client InInterceptor invoked ******** " );

                URL url = new URL( ( String ) message.getExchange().getOutMessage().get( Message.ENDPOINT_ADDRESS ) );

                if ( url.getPort() == Integer.parseInt( ChannelSettings.SECURE_PORT_X2 ) )
                {
                    HttpHeaders headers = new HttpHeadersImpl( message.getExchange().getOutMessage());

                    String spHeader = headers.getHeaderString( Common.HEADER_SPECIAL );

                    if(!Strings.isNullOrEmpty( spHeader ))
                    {
                        String envIdIn   = headers.getHeaderString( Common.HEADER_ENV_ID_SOURCE );
                        String peerIdIn  = headers.getHeaderString( Common.HEADER_PEER_ID_SOURCE );

                        if ( !Strings.isNullOrEmpty( envIdIn ) )
                        {
                            MessageContentUtil.decryptMessageContent( channelManagerImpl.getSecurityManager(), message,
                                    envIdIn );
                        }
                        else if ( !Strings.isNullOrEmpty( peerIdIn ) )
                        {
                            MessageContentUtil.decryptMessageContent( channelManagerImpl.getSecurityManager(), message,
                                    peerIdIn );
                        }
                    }

                }
            }
        }
        catch(Exception ex)
        {

        }
    }
}
