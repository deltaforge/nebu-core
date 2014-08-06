package nl.bitbrains.nebu.rest.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import nl.bitbrains.nebu.common.config.ClientConfiguration;
import nl.bitbrains.nebu.common.config.Configuration;

import org.glassfish.jersey.client.ClientConfig;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 *         This singleton class allows one to build the requests that can be
 *         performed on the VMManager's REST API.
 */
public final class RequestBuilder {

    public static final String URI_TOPOLOGY = "topology";
    public static final String URI_VIRTUAL_MACHINES = "virt";
    public static final String URI_PHYSICAL_MACHINES = "phys";
    public static final String URI_CREATE_VM = "createVM";
    public static final String URI_STATUS = "status";
    public static final String URI_VMTEMPLATES = "vmtemplates";

    public static final String QUERY_PARAM_HOSTNAME = "hostname";
    public static final String QUERY_PARAM_TEMPLATE = "template";
    public static final String QUERY_PARAM_STORE = "store";
    private static final String NEBU_VMM = "nebu-vmm";

    private static RequestBuilder instance = null;

    /**
     * Private constructor to ensure it can not be instantiated outside of this
     * class.
     */
    private RequestBuilder() {
    }

    /**
     * @return the RequestBuilder singleton.
     */
    protected static RequestBuilder get() {
        if (RequestBuilder.instance == null) {
            RequestBuilder.instance = new RequestBuilder();
        }
        return RequestBuilder.instance;
    }

    /**
     * @return a builder that can call .get() to perform the GET on the topology
     *         URI.
     */
    public Invocation.Builder newGetTopologyClient() {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_TOPOLOGY);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @return a builder that can call .get() to perform the GET on the virtual
     *         machines URI.
     */
    public Invocation.Builder newGetVirtualMachinesClient() {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_VIRTUAL_MACHINES);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @param uuid
     *            identifier of the VM you are requesting.
     * @return a builder that can call .get() to perform the GET on the virtual
     *         machines URI with the specified uuid.
     */
    public Invocation.Builder newGetVirtualMachineClient(final String uuid) {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_VIRTUAL_MACHINES).path(uuid);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @param templateUUID
     *            uuid of the template to get the physical tree for.
     * @return a builder that can call .get() to perform the GET on a VMTemplate
     *         /phys to get the topology wherin the template can be deployed.
     */
    public Invocation.Builder newGetTopologyForVMTemplateClient(final String templateUUID) {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_VMTEMPLATES).path(templateUUID)
                .path(RequestBuilder.URI_PHYSICAL_MACHINES);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @param uuid
     *            identifier of the VM you are requesting.
     * @return a builder that can call .get() to perform the GET on the virtual
     *         machines URI with the specified uuid.
     */
    public Builder newGetVirtualMachineStatusClient(final String uuid) {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_STATUS).path(uuid);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @param uuid
     *            identifier of the template you are putting.
     * @return a builder that can call .put() to perform the PUT request, to
     *         store a template.
     */
    public Invocation.Builder newPutTemplateClient(final String uuid) {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_VMTEMPLATES).path(uuid);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @param uuid
     *            identifier of the template you are putting.
     * @param hostName
     *            prefix of the hostname of the new vm.
     * @param template
     *            VMTemplate uuid to use.
     * @return a builder that can call .post() to perform the POST request, to
     *         start a new VM.
     */
    public Invocation.Builder newPostCreateVMClient(final String uuid, final String hostName,
            final String template) {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_PHYSICAL_MACHINES).path(uuid)
                .path(RequestBuilder.URI_CREATE_VM)
                .queryParam(RequestBuilder.QUERY_PARAM_HOSTNAME, hostName)
                .queryParam(RequestBuilder.QUERY_PARAM_TEMPLATE, template);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @param uuid
     *            identifier of the template you are putting.
     * @param hostName
     *            prefix of the hostname of the new vm.
     * @param template
     *            VMTemplate uuid to use.
     * @param store
     *            to place the vm on.
     * @return a builder that can call .post() to perform the POST request, to
     *         start a new VM.
     */
    public Builder newPostCreateVMClient(final String uuid, final String hostName,
            final String template, final String store) {
        final WebTarget target = this.newNebuWebTarget();
        final WebTarget uriTarget = target.path(RequestBuilder.URI_PHYSICAL_MACHINES).path(uuid)
                .path(RequestBuilder.URI_CREATE_VM)
                .queryParam(RequestBuilder.QUERY_PARAM_HOSTNAME, hostName)
                .queryParam(RequestBuilder.QUERY_PARAM_TEMPLATE, template)
                .queryParam(RequestBuilder.QUERY_PARAM_STORE, store);
        return this.newXMLTypeInvocationBuilder(uriTarget);
    }

    /**
     * @param uriTarget
     *            target to specify the builder for.
     * @return the invocation.builder that can perform the request.
     */
    private Builder newXMLTypeInvocationBuilder(final WebTarget uriTarget) {
        final Builder builder = uriTarget.request(MediaType.APPLICATION_XML_TYPE);
        return builder;
    }

    /**
     * @return a Jersey ClientConfig that is suited for contact with Nebu
     *         provided APIs.
     */
    private ClientConfig newNebuClientConfig() {
        return new ClientConfig();
    }

    /**
     * @return a Client that is suited for contact with Nebu provided APIs.
     */
    private Client newNebuClient() {
        final ClientConfig config = this.newNebuClientConfig();
        return ClientBuilder.newClient(config);
    }

    /**
     * NB: Required a valid {@link Configuration}.
     * 
     * @return a WebTarget that is suited for contact with Nebu that contains
     *         the base URI.
     */
    private WebTarget newNebuWebTarget() {
        final Client client = this.newNebuClient();
        final ClientConfiguration config = Configuration.get()
                .getClientConfig(RequestBuilder.NEBU_VMM);
        return client.target("http://" + config.getIpAddress() + ":" + config.getPort());
    }

}
