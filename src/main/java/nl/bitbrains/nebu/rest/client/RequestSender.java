package nl.bitbrains.nebu.rest.client;

import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.rest.RESTRequestException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 *         Use this class for sending requests to the webserver as configured in
 *         the configuration.
 */
public final class RequestSender {

    public static final String INVALID_RESPONSE = "Invalid response gotten :";
    public static final String CONNECTION_REFUSED = "Connection refused :";
    private static final String INVALID_BODY = "Invalid xml specified";
    private static Logger logger = LogManager.getLogger();
    private static RequestSender instance;

    /**
     * Private constructor to prevent instantiation outside of this class.
     */
    private RequestSender() {
    }

    /**
     * @return the RequestSender singleton.
     */
    public static RequestSender get() {
        if (RequestSender.instance == null) {
            RequestSender.instance = new RequestSender();
        }
        return RequestSender.instance;
    }

    /**
     * @param <T>
     *            Type that you want returned out.
     * @param builder
     *            invocation to perform.
     * @param classType
     *            type of result you expect.
     * @return the expected result of the request.
     * @throws RESTRequestException
     *             if an HTTP error code is returned.
     */
    protected static <T> T performGETRequestAndCheckResponse(final Invocation.Builder builder,
            final Class<T> classType) throws RESTRequestException {
        try {
            final Response rep = builder.get();

            if (rep.getStatus() != Response.Status.OK.getStatusCode()) {
                RequestSender.logger.error("Got StatusCode: " + rep.getStatus());
                throw new RESTRequestException(rep.getStatusInfo().toString(), rep.getStatus());
            }
            return rep.readEntity(classType);
        } catch (final WebApplicationException e) {
            throw RequestSender.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e));
        } catch (final ProcessingException e) {
            throw RequestSender.logger.throwing(new RESTRequestException(
                    RequestSender.CONNECTION_REFUSED + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e));
        }
    }

    /**
     * @return the Physical topology obtained from the server.
     * @throws CacheException
     *             if an HTTP error code is returned.
     */
    public PhysicalTopology getTopology() throws CacheException {
        return (PhysicalTopology) CacheManager.get(RequestBuilder.URI_TOPOLOGY,
                                                   new TopologyCacheLoader());
    }

    /**
     * @return the List of VirtualMachine instances obtained from the server.
     * @throws CacheException
     *             if an HTTP error code is returned.
     */
    @SuppressWarnings("unchecked")
    public List<String> getVirtualMachines() throws CacheException {
        return (List<String>) CacheManager.get(RequestBuilder.URI_VIRTUAL_MACHINES,
                                               new VirtListCacheLoader());
    }

    /**
     * @param uuid
     *            identifier of the VM you want information on.
     * @return the Virtualmachine instance obtained from the server.
     * @throws CacheException
     *             if an HTTP error code is returned.
     */
    public VirtualMachine getVirtualMachine(final String uuid) throws CacheException {
        return (VirtualMachine) CacheManager.get(RequestBuilder.URI_VIRTUAL_MACHINES + "/" + uuid,
                                                 new VirtItemCacheLoader(uuid));
    }

    /**
     * @param uuid
     *            identifier of the VM you want the status of.
     * @return the Virtualmachine instance obtained from the server.
     * @throws CacheException
     *             if an HTTP error code is returned.
     */
    public VirtualMachine getVirtualMachineStatus(final String uuid) throws CacheException {
        return (VirtualMachine) CacheManager.get(RequestBuilder.URI_VIRTUAL_MACHINES + "/" + uuid,
                                                 new VirtStatusCacheLoader(uuid),
                                                 VirtStatusCacheLoader.EXPIRATION_TIME);
    }

    /**
     * @param uuid
     *            of the vmTemplate to get the topology tree for.
     * @return the Physical topology obtained from the server.
     * @throws CacheException
     *             if an HTTP error code is returned.
     */
    public PhysicalTopology getVMTemplateTopologyOptions(final String uuid) throws CacheException {
        return (PhysicalTopology) CacheManager.get(RequestBuilder.URI_VMTEMPLATES + "/" + uuid
                + "/phys", new VMTemplateTopologyCacheLoader(uuid));
    }

    /**
     * @param uuid
     *            identifier of the VM you want information on.
     * @param xml
     *            PUT body.
     * @throws RESTRequestException
     *             if an HTTP error code is returned.
     */
    public void putTemplate(final String uuid, final Element xml) throws RESTRequestException {
        final Invocation.Builder builder = RequestBuilder.get().newPutTemplateClient(uuid);
        RequestSender.performPUTRequestAndCheckResponseCreated(builder, xml);
    }

    /**
     * @param uuid
     *            identifier of the template you are putting.
     * @param hostName
     *            prefix of the hostname of the new vm.
     * @param template
     *            VMTemplate uuid to use.
     * @return the response
     * @throws RESTRequestException
     *             if an HTTP error code is returned.
     */
    public Response postCreateVM(final String uuid, final String hostName, final String template)
            throws RESTRequestException {
        final Invocation.Builder builder = RequestBuilder.get().newPostCreateVMClient(uuid,
                                                                                      hostName,
                                                                                      template);
        return RequestSender.performPOSTRequestAndCheckResponseCreated(builder);
    }

    /**
     * @param uuid
     *            identifier of the template you are putting.
     * @param hostName
     *            prefix of the hostname of the new vm.
     * @param template
     *            VMTemplate uuid to use.
     * @param store
     *            store to use for this vm.
     * @return the response
     * @throws RESTRequestException
     *             if an HTTP error code is returned.
     */
    public Response postCreateVM(final String uuid, final String hostName, final String template,
            final String store) throws RESTRequestException {
        final Invocation.Builder builder = RequestBuilder.get().newPostCreateVMClient(uuid,
                                                                                      hostName,
                                                                                      template,
                                                                                      store);
        return RequestSender.performPOSTRequestAndCheckResponseCreated(builder);
    }

    /**
     * @param builder
     *            invocation to perform.
     * 
     * @return Response gotten.
     * @throws RESTRequestException
     *             iff http fails.
     */
    private static Response performPOSTRequestAndCheckResponseCreated(final Builder builder)
            throws RESTRequestException {
        try {
            final Response rep = builder.post(null);

            if (rep.getStatus() != Response.Status.CREATED.getStatusCode()) {
                RequestSender.logger.error(rep.getStatus());
                throw RequestSender.logger.throwing(new RESTRequestException(rep.getStatusInfo()
                        .toString(), rep.getStatus()));
            }
            return rep;
        } catch (final WebApplicationException e) {
            throw RequestSender.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e));
        } catch (final ProcessingException e) {
            throw RequestSender.logger.throwing(new RESTRequestException(
                    RequestSender.CONNECTION_REFUSED + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e));
        }
    }

    /**
     * @param builder
     *            invocation to perform.
     * @param xml
     *            body for the put request.
     * @return the Response object
     * @throws RESTRequestException
     *             if an HTTP error code is returned.
     */
    private static Response performPUTRequestAndCheckResponseCreated(final Builder builder,
            final Element xml) throws RESTRequestException {
        try {
            final Entity<Document> entity = Entity.entity(XMLConverter
                    .convertJDOMElementW3CDocument(xml), MediaType.APPLICATION_XML_TYPE);
            final Response rep = builder.put(entity);

            if (rep.getStatus() != Response.Status.CREATED.getStatusCode()) {
                RequestSender.logger.error(rep.getStatus());
                throw RequestSender.logger.throwing(new RESTRequestException(rep.getStatusInfo()
                        .toString(), rep.getStatus()));
            }
            return RequestSender.logger.exit(rep);
        } catch (final WebApplicationException e) {
            throw RequestSender.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e));
        } catch (final ProcessingException e) {
            throw RequestSender.logger.throwing(new RESTRequestException(
                    RequestSender.CONNECTION_REFUSED + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e));
        } catch (final JDOMException e) {
            throw RequestSender.logger.throwing(new RESTRequestException(RequestSender.INVALID_BODY
                    + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e));
        }
    }
}
