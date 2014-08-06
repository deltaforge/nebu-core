package nl.bitbrains.nebu.rest.server;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.ApplicationFactory;
import nl.bitbrains.nebu.rest.client.RequestSender;
import nl.bitbrains.nebu.rest.server.AppsProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RequestSender.class, CacheManager.class, XMLConverter.class })
@PowerMockIgnore("javax.management.*")
public class TestAppProvider extends JerseyTest {
    @Mock
    private RequestSender reqSender;

    @Mock
    private nl.bitbrains.nebu.containers.Application app;

    @Override
    protected javax.ws.rs.core.Application configure() {
        return new ResourceConfig(AppsProvider.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CacheManager.class);
    }

    private void setUpCacheManager(final String key, final Object value) throws CacheException {
        Mockito.when(CacheManager.get(key)).thenReturn(value);
    }

    private Document getPostRootBody(final String name) throws JDOMException {
        final Element root = new Element(ApplicationFactory.TAG_ELEMENT_ROOT);
        final Attribute att = new Attribute(ApplicationFactory.ATTRIBUTE_NAME, name);
        root.setAttribute(att);
        final Element policy = new Element(ApplicationFactory.TAG_DEPLOYMENT_POLICY);
        policy.setAttribute(ApplicationFactory.ATTRIBUTE_NAME, "random");
        root.addContent(policy);
        return XMLConverter.convertJDOMElementW3CDocument(root);
    }

    @Test
    public void testPostExistingApp() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        final String id = "id1";
        final String newName = "name";
        map.put(id, new ApplicationBuilder().withUuid(id).build());
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(newName),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(id).request().post(entity);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
        Assert.assertEquals(newName, map.get(id).getName());
    }

    @Test
    public void testPostExistingAppMalformed() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        final String id = "id1";
        map.put(id, new ApplicationBuilder().withUuid(id).build());
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Entity<String> entity = Entity.entity("malformedEntity",
                                                    MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(id).request().post(entity);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testPostExistingApp404() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        final String id = "id1";
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(id),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(id).request().post(entity);

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testGetExistingApp200() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        final String id = "id1";
        final Application expected = new ApplicationBuilder().withUuid(id).withName(id)
                .withDeploymentPolicy("random").build();
        map.put(id, expected);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Response rep = this.target(AppsProvider.PATH).path(id).request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());

        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(rep
                .readEntity(Document.class));
        final Application actual = new ApplicationFactory().fromXML(elem).build();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPostParseException() throws CacheException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        final String id = "id1";
        final Application expected = new ApplicationBuilder().withUuid(id).withName(id).build();
        map.put(id, expected);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        PowerMockito.mockStatic(XMLConverter.class);
        Mockito.when(XMLConverter.convertW3CDocumentJDOMElement(Matchers.any(Document.class)))
                .thenReturn(new Element("wrongElement"));

        final Entity<Document> entity = Entity.entity(this.getPostRootBody("newName"),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(id).request().post(entity);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rep.getStatus());

    }
}
