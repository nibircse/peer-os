package org.safehaus.subutai.core.channel.impl.token;


import org.safehaus.subutai.core.channel.api.token.ChannelTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by nisakov on 3/3/15.
 * Thread that controls channel-token validity period
 */
public class ChannelTokenController implements Runnable
{
    private final static Logger LOG = LoggerFactory.getLogger( ChannelTokenController.class );
    private ChannelTokenManager channelTokenManager = null;

    public ChannelTokenController(ChannelTokenManager channelTokenManager)
    {
        this.channelTokenManager = channelTokenManager;
    }

    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                Thread.sleep(60 * 60 * 1000 );

                LOG.info( "******** Channel Token controller invoked *********" );
                channelTokenManager.setTokenValidity();
            }
        }
        catch (InterruptedException e)
        {
            LOG.error( "Error in ChannelToken controller" , e );
        }
    }
}

