package nl.bitbrains.nebu.rest.server;

import java.net.URI;
import java.text.ParseException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentFactory;
import nl.bitbrains.nebu.containers.DeploymentRequest;
import nl.bitbrains.nebu.containers.DeploymentRequestFactory;
import nl.bitbrains.nebu.deployer.Deployer;
import nl.bitbrains.nebu.deployer.DeployerException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * Provides deployment functionality to the application/user via the Nebu REST
 * API.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeploymentsProvider {

    public static final String PATH = "deploy";
    public static final String PATH_UUID = "{" + DeploymentsProvider.UUID_NAME + "}";

    public static final String UUID_NAME = "depUUID";
    public static final String QUERY_PARAM_NUMVMS = "numVms";
    private static Logger logger = LogManager.getLogger();

    private final Application app;

    /**
     * Public constructor.
     * 
     * @param app
     *            to use
     */
    public DeploymentsProvider(final Application app) {
        this.app = app;
    }

    /**
     * @param depuuid
     *            id of the deployment to get.
     * @return DeploymentProvider to handle the /deploy/uuid queries.
     */
    @Path(DeploymentsProvider.PATH_UUID)
    public final Object deploymentProvider(
            @PathParam(DeploymentsProvider.UUID_NAME) final String depuuid) {
        if (this.app.getDeployment(depuuid) == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return new DeploymentProvider(this.app, this.app.getDeployment(depuuid));
    }

    /**
     * Handles a GET request on the base URI.
     * 
     * @return a list of deployments for the specified app.
     * @throws JDOMException
     *             iff conversion fails
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public final Response getDeployments() throws JDOMException {
        DeploymentsProvider.logger.entry();
        Response rep = null;
        final Element elem = XMLConverter
                .convertCollectionToJDOMElement(this.app.getDeployments(),
                                                new DeploymentFactory(false),
                                                DeploymentFactory.LIST_TAG_ELEMENT_ROOT,
                                                DeploymentFactory.TAG_ELEMENT_ROOT);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        return DeploymentsProvider.logger.exit(rep);
    }

    /**
     * Handles a POST request on the base URI, creating a new deployment.
     * Creates a deployment suggestion based on the required number of VMs.
     * 
     * @param doc
     *            post body.
     * @return a 201 on successful creation
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public final Response postDeployments(final Document doc) {
        DeploymentsProvider.logger.entry();
        Response rep = null;
        final DeploymentRequest depRequest;
        final Deployer deployer;
        final Deployment deployment;

        try {
            final Element depRequestElem = XMLConverter.convertW3CDocumentJDOMElement(doc);
            depRequest = new DeploymentRequestFactory(this.app).fromXML(depRequestElem);
        } catch (final ParseException ex) {
            DeploymentsProvider.logger.catching(Level.WARN, ex);
            return DeploymentsProvider.logger.exit(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build());
        }

        try {
            deployer = (Deployer) CacheManager.get(Deployer.CACHE_KEY);
        } catch (final CacheException ex) {
            DeploymentsProvider.logger.catching(Level.WARN, ex);
            return DeploymentsProvider.logger.exit(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build());
        }

        try {
            deployment = deployer.generateDeployment(depRequest);
        } catch (final DeployerException ex) {
            DeploymentsProvider.logger.catching(Level.WARN, ex);
            return DeploymentsProvider.logger.exit(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build());
        }

        this.app.putDeployment(deployment);
        final URI location = URI.create(deployment.getUniqueIdentifier());
        rep = Response.created(location).build();
        return DeploymentsProvider.logger.exit(rep);
    }

}
