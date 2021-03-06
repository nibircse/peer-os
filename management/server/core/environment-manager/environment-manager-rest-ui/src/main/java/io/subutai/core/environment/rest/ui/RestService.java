package io.subutai.core.environment.rest.ui;


import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public interface RestService
{
    /** Templates **************************************************** */
    @GET
    @Path( "templates/verified/{templateName}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response getVerifiedTemplate( @PathParam( "templateName" ) String templateName );

    @POST
    @Path( "{environmentId}/containers/{containerId}/export/{name}/{version}/{private}" )
    @Produces( { MediaType.TEXT_PLAIN } )
    Response createTemplate( @PathParam( "environmentId" ) String environmentId,
                             @PathParam( "containerId" ) String containerId, @PathParam( "name" ) String templateName,
                             @PathParam( "version" ) String version, @PathParam( "private" ) boolean privateTemplate );

    /** Environments **************************************************** */

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    Response listEnvironments();

    @GET
    @Path( "tenants" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response listTenantEnvironments();

    @POST
    @Path( "build" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response build( @FormParam( "name" ) String name, @FormParam( "topology" ) String topologyJson );


    @POST
    @Path( "build/advanced" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response buildAdvanced( @FormParam( "name" ) String name, @FormParam( "topology" ) String topologyJson );


    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Path( "{environmentId}/modify" )
    Response modify( @PathParam( "environmentId" ) String environmentId, @FormParam( "topology" ) String topologyJson,
                     @FormParam( "removedContainers" ) String removedContainers,
                     @FormParam( "quotaContainers" ) String quotaContainers );

    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Path( "{environmentId}/modify/advanced" )
    Response modifyAdvanced( @PathParam( "environmentId" ) String environmentId,
                             @FormParam( "topology" ) String topologyJson,
                             @FormParam( "removedContainers" ) String removedContainers,
                             @FormParam( "quotaContainers" ) String quotaContainers );


    @DELETE
    @Path( "{environmentId}" )
    Response destroyEnvironment( @PathParam( "environmentId" ) String environmentId );


    /** Environments SSH keys **************************************************** */

    @GET
    @Path( "{environmentId}/keys" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response getSshKeys( @PathParam( "environmentId" ) String environmentId );


    @POST
    @Path( "{environmentId}/keys" )
    Response addSshKey( @PathParam( "environmentId" ) String environmentId, @FormParam( "key" ) String key );


    @DELETE
    @Path( "{environmentId}/keys" )
    Response removeSshKey( @PathParam( "environmentId" ) String environmentId, @QueryParam( "key" ) String key );


    @PUT
    @Path( "{environmentId}/containers/{containerId}/name" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response setContainerName( @PathParam( "environmentId" ) String environmentId,
                               @PathParam( "containerId" ) String containerId, @QueryParam( "name" ) String name );


    /** Containers **************************************************** */

    @DELETE
    @Path( "containers/{containerId}" )
    Response destroyContainer( @PathParam( "containerId" ) String containerId );

    @GET
    @Path( "containers/{containerId}/state" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response getContainerState( @PathParam( "containerId" ) String containerId );

    @PUT
    @Path( "containers/{containerId}/start" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response startContainer( @PathParam( "containerId" ) String containerId );

    @PUT
    @Path( "containers/{containerId}/stop" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response stopContainer( @PathParam( "containerId" ) String containerId );

    @GET
    @Path( "containers/{containerId}/snapshots" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response listContainerSnapshots( @PathParam( "containerId" ) String containerId );

    @DELETE
    @Path( "containers/{containerId}/snapshots/partition/{partition}/label/{label}" )
    Response removeContainerSnapshot( @PathParam( "containerId" ) String containerId,
                                      @PathParam( "partition" ) String partition, @PathParam( "label" ) String label );

    @PUT
    @Path( "containers/{containerId}/snapshots/partition/{partition}/label/{label}" )
    Response rollbackContainerSnapshot( @PathParam( "containerId" ) String containerId,
                                        @PathParam( "partition" ) String partition,
                                        @PathParam( "label" ) String label );

    @POST
    @Path( "containers/{containerId}/snapshots/partition/{partition}/label/{label}/{stop}" )
    Response addContainerSnapshot( @PathParam( "containerId" ) String containerId,
                                   @PathParam( "partition" ) String partition, @PathParam( "label" ) String label,
                                   @PathParam( "stop" ) boolean stopContainer );


    /** Container types **************************************************** */

    @GET
    @Path( "containers/types" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response listContainerTypes();


    @GET
    @Path( "containers/types/info" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response listContainerTypesInfo();


    /** Peers **************************************************** */

    @GET
    @Path( "peers" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response getPeers();

    /** Peers **************************************************** */

    @GET
    @Path( "resourcehosts" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response getResourceHosts();


    /** Tags **************************************************** */

    @POST
    @Path( "{environmentId}/containers/{containerId}/tags" )
    Response addTags( @PathParam( "environmentId" ) String environmentId,
                      @PathParam( "containerId" ) String containerId, @FormParam( "tags" ) String tags );

    @DELETE
    @Path( "{environmentId}/containers/{containerId}/tags/{tag}" )
    Response removeTag( @PathParam( "environmentId" ) String environmentId,
                        @PathParam( "containerId" ) String containerId, @PathParam( "tag" ) String tag );

    @GET
    @Path( "{environmentId}/containers/{containerId}/ssh" )
    @Produces( { MediaType.TEXT_PLAIN } )
    Response setupContainerSsh( @PathParam( "environmentId" ) String environmentId,
                                @PathParam( "containerId" ) String containerId );


    /** Share **************************************************** */

    @GET
    @Path( "shared/users/{objectId}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    Response getSharedUsers( @PathParam( "objectId" ) String objectId );


    @POST
    @Path( "{environmentId}/share" )
    Response share( @FormParam( "users" ) String users, @PathParam( "environmentId" ) String environmentId );


    @GET
    @Path( "{environmentId}/download" )
    Response getDownloadProgress( @PathParam( "environmentId" ) String environmentId );
}
