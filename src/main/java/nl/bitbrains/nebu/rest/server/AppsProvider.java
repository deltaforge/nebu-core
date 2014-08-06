package nl.bitbrains.nebu.rest.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

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
import nl.bitbrains.nebu.common.util.UUIDGenerator;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.ApplicationFactory;
import nl.bitbrains.nebu.deployer.Deployer;
import nl.bitbrains.nebu.deployer.MissingPolicyException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * Provides the /app path of the REST API.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@Path(AppsProvider.PATH)
public class AppsProvider {

    public static final String PATH = "/app";
    public static final String PATH_UUID = "{" + AppsProvider.UUID_NAME + "}";
    public static final String UUID_NAME = "appUUID";
    public static final String CACHE_KEY_APPS = "cacheApps";
    public static final String CONTENT_LOCATION_HEADER = "Location";
    private static Logger logger = LogManager.getLogger();

    /**
     * Default simple empty constructor.
     */
    public AppsProvider() {

    }

    /**
     * @param applicationID
     *            uuid of the app this query is routed to.
     * @return {@link AppProvider} to deal with requests for /app/uuid queries.
     */
    @Path(AppsProvider.PATH_UUID)
    public final Object appProvider(@PathParam(AppsProvider.UUID_NAME) final String applicationID) {
        Application app;
        try {
            app = AppsProvider.getApplication(applicationID);
        } catch (final NoSuchElementException e) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return new AppProvider(app);
    }

    /**
     * Gets the cache, also accessible for other classes.
     * 
     * @return the map of applications currently known to the system.
     */
    @SuppressWarnings("unchecked")
    protected static final Map<String, Application> getCache() {
        Map<String, Application> map;
        try {
            map = (Map<String, Application>) CacheManager.get(AppsProvider.CACHE_KEY_APPS);
        } catch (final CacheException | IllegalArgumentException e) {
            map = new HashMap<String, Application>();
            CacheManager.put(AppsProvider.CACHE_KEY_APPS, map);
        }
        return map;
    }

    /**
     * Gets an application based on the id, from the cache.
     * 
     * @param id
     *            of the app to fetch
     * @return the app if it exists, throws a {@link NoSuchElementException} if
     *         it doesn't.
     */
    private static Application getApplication(final String id) {
        final Map<String, Application> map = AppsProvider.getCache();
        if (!map.containsKey(id) || map.get(id) == null) {
            throw AppsProvider.logger.throwing(new NoSuchElementException("appID not found"));
        }
        return map.get(id);
    }

    /**
     * Handles the GET request on this path.
     * 
     * @return list of the currently known applications.
     * @throws JDOMException
     *             if xml conversion fails.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public final Response getApps() throws JDOMException {
        AppsProvider.logger.entry();
        final Map<String, Application> map = AppsProvider.getCache();
        final Collection<Application> apps = map.values();

        final Element elem = XMLConverter
                .convertCollectionToJDOMElement(apps,
                                                new ApplicationFactory(false),
                                                ApplicationFactory.LIST_TAG_ELEMENT_ROOT,
                                                ApplicationFactory.TAG_ELEMENT_ROOT);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        final Response rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        return AppsProvider.logger.exit(rep);
    }

    /**
     * Handles the POST request on this path.
     * 
     * @param doc
     *            body of the post request.
     * @return location header set iff successfull.
     * @throws URISyntaxException
     *             iff malformed uri is created
     * @throws CacheException
     *             if cache fails.
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public final Response postApps(final Document doc) throws URISyntaxException, CacheException {
        AppsProvider.logger.entry();
        final Map<String, Application> map = AppsProvider.getCache();
        final String key = UUIDGenerator.generate(Application.UUID_PREFIX);
        Response rep = null;
        try {
            final Element xml = XMLConverter.convertW3CDocumentJDOMElement(doc);
            final ApplicationBuilder builder = new ApplicationFactory().fromXML(xml);
            builder.withUuid(key);
            final Application app = builder.build();
            final Deployer deployer = (Deployer) CacheManager.get(Deployer.CACHE_KEY);
            deployer.setPolicy(app,
                               app.getDeploymentPolicy(),
                               new ApplicationFactory().getPolicyXML(xml));
            map.put(key, app);
            final URI location = new URI(key);
            rep = Response.created(location).build();
        } catch (final ParseException | MissingPolicyException e) {
            AppsProvider.logger.catching(Level.ERROR, e);
            rep = Response.status(Status.BAD_REQUEST).build();
        }

        return AppsProvider.logger.exit(rep);
    }
}
