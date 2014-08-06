package nl.bitbrains.nebu.rest.server;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
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
import nl.bitbrains.nebu.deployer.Deployer;
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
 * Tests for the {@link AppsProvider} class.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RequestSender.class, CacheManager.class, Deployer.class })
@PowerMockIgnore("javax.management.*")
public class TestAppsProvider extends JerseyTest {

    @Mock
    private RequestSender reqSender;
    private Deployer dep;

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
        this.dep = PowerMockito.mock(Deployer.class);
        this.setUpCacheManager(Deployer.CACHE_KEY, this.dep);
    }

    private void setUpCacheManager(final String key, final Object value) throws CacheException {
        Mockito.when(CacheManager.get(key)).thenReturn(value);
    }

    private void setUpCacheManager(final Throwable e) throws CacheException {
        Mockito.when(CacheManager.get(Matchers.anyString())).thenThrow(e);
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
    public void testCacheException() throws CacheException {
        this.setUpCacheManager(new CacheException("message"));
        AppsProvider.getCache();
        PowerMockito.verifyStatic();
        CacheManager.put(Matchers.eq(AppsProvider.CACHE_KEY_APPS), Matchers.any());
    }

    @Test
    public void testIllegalArgumentException() throws CacheException {
        this.setUpCacheManager(new IllegalArgumentException("message"));
        AppsProvider.getCache();
        PowerMockito.verifyStatic();
        CacheManager.put(Matchers.eq(AppsProvider.CACHE_KEY_APPS), Matchers.any());
    }

    @Test
    public void testGETRootNoApps() throws CacheException, ParseException {
        final Map<String, Application> map = new HashMap<String, Application>();
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Response rep = this.target(AppsProvider.PATH).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(rep
                .readEntity(Document.class));
        final List<Application> apps = XMLConverter
                .convertJDOMElementToList(elem, new ApplicationFactory());
        Assert.assertTrue(apps.isEmpty());
    }

    @Test
    public void testGETRootApps() throws CacheException, ParseException {
        final Map<String, Application> map = new HashMap<String, Application>();
        final String id1 = "id1";
        final String id2 = "id2";
        map.put(id1, new ApplicationBuilder().withUuid(id1).withName(id1).build());
        map.put(id2, new ApplicationBuilder().withUuid(id2).withName(id2).build());
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Response rep = this.target(AppsProvider.PATH).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(rep
                .readEntity(Document.class));
        final List<Application> apps = XMLConverter
                .convertJDOMElementToList(elem, new ApplicationFactory(false));
        Assert.assertEquals(2, apps.size());
    }

    @Test
    public void testPostNewApp() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody("name"),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).request().post(entity);

        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), rep.getStatus());
        Assert.assertNotNull(rep.getLocation());
    }

    @Test
    public void testPostNewAppCacheUpdated() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody("name"),
                                                      MediaType.APPLICATION_XML_TYPE);

        this.target(AppsProvider.PATH).request().post(entity);

        Assert.assertFalse(map.isEmpty());
    }

    @Test
    public void testPostNewAppMalformed() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Entity<String> entity = Entity.entity("malformedEntity",
                                                    MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).request().post(entity);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testGetNonExistingApp404() throws CacheException, ParseException, JDOMException {
        final Map<String, Application> map = new HashMap<String, Application>();
        final String id = "id1";
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);

        final Response rep = this.target(AppsProvider.PATH).path(id).request().get();

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rep.getStatus());
    }
}
