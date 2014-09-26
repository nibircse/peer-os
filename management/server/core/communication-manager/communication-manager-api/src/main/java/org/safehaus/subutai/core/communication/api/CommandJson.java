/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.core.communication.api;


import org.safehaus.subutai.common.protocol.Request;
import org.safehaus.subutai.common.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * This is simple utility class for serializing/deserializing object to/from json.
 */
public class CommandJson
{

    private static final Logger LOG = LoggerFactory.getLogger( CommandJson.class.getName() );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().addDeserializationExclusionStrategy(
            new SkipNullsExclusionStrategy() ).disableHtmlEscaping().create();


    private CommandJson()
    {

    }


    /**
     * Returns deserialized request from json string
     *
     * @param json - request in json format
     *
     * @return request
     */
    public static Request getRequestFromCommandJson( String json )
    {
        try
        {
            Command cmd = getCommandFromJson( json );
            if ( cmd.getRequest() != null )
            {
                return cmd.getRequest();
            }
        }
        catch ( Exception ex )
        {
            LOG.error( "Error in getRequestFromCommandJson", ex );
        }

        return null;
    }


    /**
     * Returns deserialized request from json string
     *
     * @param json - request in json format
     *
     * @return request
     */
    public static Command getCommandFromJson( String json )
    {
        try
        {
            return GSON.fromJson( escape( json ), CommandImpl.class );
        }
        catch ( Exception ex )
        {
            LOG.error( "Error in getCommandFromJson", ex );
        }

        return null;
    }


    /**
     * Escapes symbols in json string
     *
     * @param s - string to escape
     *
     * @return escaped json string
     */
    private static String escape( String s )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < s.length(); i++ )
        {
            char ch = s.charAt( i );
            switch ( ch )
            {
                case '"':
                    sb.append( "\"" );
                    break;
                case '\\':
                    sb.append( "\\" );
                    break;
                case '\b':
                    sb.append( "\b" );
                    break;
                case '\f':
                    sb.append( "\f" );
                    break;
                case '\n':
                    sb.append( "\n" );
                    break;
                case '\r':
                    sb.append( "\r" );
                    break;
                case '\t':
                    sb.append( "\t" );
                    break;
                case '/':
                    sb.append( "\\/" );
                    break;
                default:
                    sb.append( processDefaultCase( ch ) );
            }
        }
        return sb.toString();
    }


    private static String processDefaultCase( char ch )
    {
        StringBuilder sb = new StringBuilder();
        if ( ( ch >= '\u0000' && ch <= '\u001F' ) || ( ch >= '\u007F' && ch <= '\u009F' ) || ( ch >= '\u2000'
                && ch <= '\u20FF' ) )
        {
            String ss = Integer.toHexString( ch );
            sb.append( "\\u" );
            for ( int k = 0; k < 4 - ss.length(); k++ )
            {
                sb.append( '0' );
            }
            sb.append( ss.toUpperCase() );
        }
        else
        {
            sb.append( ch );
        }
        return sb.toString();
    }


    /**
     * Returns deserialized response from json string
     *
     * @param json - response in json format
     *
     * @return response
     */
    public static Response getResponseFromCommandJson( String json )
    {
        try
        {
            Command cmd = getCommandFromJson( json );
            if ( cmd.getResponse() != null )
            {
                return cmd.getResponse();
            }
        }
        catch ( Exception ex )
        {
            LOG.error( "Error in getResponseCommandJson", ex );
        }

        return null;
    }


    /**
     * Returns serialized request from Request POJO
     *
     * @param request - request in pojo format
     *
     * @return request in json format
     */
    public static String getRequestCommandJson( Request request )
    {
        try
        {
            return GSON.toJson( new CommandImpl( request ) );
        }
        catch ( Exception ex )
        {
            LOG.error( "Error in getCommandJson", ex );
        }
        return null;
    }


    /**
     * Returns serialized response from Response POJO
     *
     * @param response - response in pojo format
     *
     * @return response in json format
     */
    public static String getResponseCommandJson( Response response )
    {
        try
        {
            return GSON.toJson( new CommandImpl( response ) );
        }
        catch ( Exception ex )
        {
            LOG.error( "Error in getResponseCommandJson", ex );
        }
        return null;
    }


    /**
     * Returns serialized request from Command POJO
     *
     * @param cmd - request in pojo format
     *
     * @return request in request format
     */
    public static String getCommandJson( Command cmd )
    {
        try
        {
            return GSON.toJson( cmd );
        }
        catch ( Exception ex )
        {
            LOG.error( "Error in getCommandJson", ex );
        }
        return null;
    }


    public static class CommandImpl implements Command
    {

        Request request;
        Response response;


        public CommandImpl( Object message )
        {
            if ( message instanceof Request )
            {
                this.request = ( Request ) message;
            }
            else if ( message instanceof Response )
            {
                this.response = ( Response ) message;
            }
        }


        @Override
        public Request getRequest()
        {
            return request;
        }


        @Override
        public Response getResponse()
        {
            return response;
        }


        @Override
        public boolean equals( final Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( !( o instanceof CommandImpl ) )
            {
                return false;
            }

            final CommandImpl command = ( CommandImpl ) o;

            if ( request != null ? !request.equals( command.request ) : command.request != null )
            {
                return false;
            }
            if ( response != null ? !response.equals( command.response ) : command.response != null )
            {
                return false;
            }

            return true;
        }


        @Override
        public int hashCode()
        {
            int result = request != null ? request.hashCode() : 0;
            result = 31 * result + ( response != null ? response.hashCode() : 0 );
            return result;
        }
    }
}
