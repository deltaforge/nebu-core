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
import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentBuilder;
import nl.bitbrains.nebu.containers.DeploymentFactory;
import nl.bitbrains.nebu.containers.DeploymentRequest;
import nl.bitbrains.nebu.containers.DeploymentRequestFactory;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateBuilder;
import nl.bitbrains.nebu.deployer.Deployer;
import nl.bitbrains.nebu.deployer.DeployerException;
import nl.bitbrains.nebu.deployer.MissingPolicyException;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestSender;
import nl.bitbrains.nebu.rest.server.AppsProvider;
import nl.bitbrains.nebu.rest.server.DeploymentsProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
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
@PrepareForTest({ RequestSender.class, CacheManager.class, XMLConverter.class, Application.class,
        VMTemplate.class, Deployment.class, Deployer.class })
@PowerMockIgnore("javax.management.*")
public class TestDeploymentsProvider extends JerseyTest {
    @Mock
    private RequestSender reqSender;

    private Deployer deployer;

    // IS currently not mocked! FIXME TODO: Check if this is possible without
    // stackoverflow from powermock.
    private Deployment mockedDeployment;

    private nl.bitbrains.nebu.containers.Application app;

    private VMTemplate template1;
    private VMTemplate template2;

    private final String id = "id1";
    private final String templateID1 = "templateID";
    private final int num1 = 10;
    private final String templateID2 = "templateID2";
    private final int num2 = 5;

    private final String depID = "depid";

    @Override
    protected javax.ws.rs.core.Application configure() {
        return new ResourceConfig(AppsProvider.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(CacheManager.class);
        this.deployer = PowerMockito.mock(Deployer.class);

        this.app = new ApplicationBuilder().withUuid(this.id).build();
        this.template1 = new VMTemplateBuilder().withUuid(this.templateID1).build();
        this.template2 = new VMTemplateBuilder().withUuid(this.templateID2).build();
        this.mockedDeployment = new DeploymentBuilder().withUuid(this.depID).build();

        // this.app = PowerMockito.mock(Application.class);
        // this.template1 = PowerMockito.mock(VMTemplate.class);
        // this.template2 = PowerMockito.mock(VMTemplate.class);
        // this.mockedDeployment = PowerMockito.mock(Deployment.class);
    }

    private void setUpCacheManager(final String key, final Object value) throws CacheException {
        Mockito.when(CacheManager.get(key)).thenReturn(value);
    }

    private void setUpCacheManagerException(final String key, final Throwable error)
            throws CacheException {
        Mockito.when(CacheManager.get(key)).thenThrow(error);
    }

    private Document getPostRootBody(final Map<String, Integer> templates) throws JDOMException {
        final Element root = new Element(DeploymentRequestFactory.TAG_ELEMENT_ROOT);
        for (final String key : templates.keySet()) {
            final Element template = this.makeTemplateElem(key, templates.get(key));
            root.addContent(template);
        }
        return XMLConverter.convertJDOMElementW3CDocument(root);
    }

    private Element makeTemplateElem(final String id, final int number) {
        final Element result = new Element(DeploymentRequestFactory.TAG_ELEMENT_TEMPLATE);
        result.setAttribute(IdentifiableFactory.TAG_ID, id);
        result.addContent(new Element(DeploymentRequestFactory.TAG_ELEMENT_NUMBER).setText(Integer
                .toString(number)));
        return result;
    }

    private void mockRequestSender() {
        PowerMockito.mockStatic(RequestSender.class);
        Mockito.when(RequestSender.get()).thenReturn(this.reqSender);
    }

    private void mockRequestSender(final Throwable e) throws RESTRequestException {
        PowerMockito.mockStatic(RequestSender.class);
        Mockito.when(RequestSender.get()).thenReturn(this.reqSender);
        Mockito.doThrow(e).when(this.reqSender)
                .putTemplate(Matchers.anyString(), Matchers.any(Element.class));
    }

    // private void createAppWithTemplate() throws CacheException {
    // final Map<String, Application> map = new HashMap<String, Application>();
    // map.put(this.id, this.app);
    // Mockito.when(this.app.getUniqueIdentifier()).thenReturn(this.id);
    // Mockito.when(this.app.getVMTemplate(this.templateID1)).thenReturn(this.template1);
    // Mockito.when(this.app.getVMTemplate(this.templateID2)).thenReturn(this.template2);
    // Mockito.when(this.template1.getUniqueIdentifier()).thenReturn(this.templateID1);
    // Mockito.when(this.template2.getUniqueIdentifier()).thenReturn(this.templateID2);
    // this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
    // this.setUpCacheManager(Deployer.CACHE_KEY, this.deployer);
    // Mockito.when(this.deployer.generateDeployment(Matchers.any(DeploymentRequest.class)))
    // .thenReturn(this.mockedDeployment);
    // Mockito.when(this.mockedDeployment.getUniqueIdentifier()).thenReturn(this.depID);
    // }

    private void createAppWithTemplates() throws CacheException, MissingPolicyException,
            DeployerException {
        final Map<String, Application> map = new HashMap<String, Application>();
        map.put(this.id, this.app);
        this.app.putVMTemplate(this.template1);
        this.app.putVMTemplate(this.template2);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
        this.setUpCacheManager(Deployer.CACHE_KEY, this.deployer);
        Mockito.when(this.deployer.generateDeployment(Matchers.any(DeploymentRequest.class)))
                .thenReturn(this.mockedDeployment);

    }

    private void createAppWithTemplatesCacheException() throws CacheException,
            MissingPolicyException, DeployerException {
        final Map<String, Application> map = new HashMap<String, Application>();
        map.put(this.id, this.app);
        this.app.putVMTemplate(this.template1);
        this.app.putVMTemplate(this.template2);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
        this.setUpCacheManagerException(Deployer.CACHE_KEY, new CacheException(""));
    }

    private void createAppWithTemplatesDeployerException() throws CacheException,
            MissingPolicyException, DeployerException {
        final Map<String, Application> map = new HashMap<String, Application>();
        map.put(this.id, this.app);
        this.app.putVMTemplate(this.template1);
        this.app.putVMTemplate(this.template2);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
        this.setUpCacheManager(Deployer.CACHE_KEY, this.deployer);
        Mockito.when(this.deployer.generateDeployment(Matchers.any(DeploymentRequest.class)))
                .thenThrow(new DeployerException(""));
    }

    @Test
    public void testGetAllDeploymentsNone() throws CacheException, ParseException,
            MissingPolicyException, DeployerException {
        this.createAppWithTemplates();

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());

        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(rep
                .readEntity(Document.class));
        final List<Deployment> list = XMLConverter
                .convertJDOMElementToList(elem, new DeploymentFactory(false));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void testGetAllTemplatesOne() throws CacheException, ParseException,
            MissingPolicyException, DeployerException {
        this.createAppWithTemplates();
        this.app.putDeployment(new DeploymentBuilder().withUuid(this.depID).build());

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());

        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(rep
                .readEntity(Document.class));
        final List<Deployment> list = XMLConverter
                .convertJDOMElementToList(elem, new DeploymentFactory(false));
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(this.depID, list.get(0).getUniqueIdentifier());
    }

    @Test
    public void testGetAllTemplatesMultiple() throws CacheException, ParseException,
            MissingPolicyException, DeployerException {
        this.createAppWithTemplates();
        final int numDeps = 8;
        for (int i = 0; i < numDeps; i++) {
            this.app.putDeployment(new DeploymentBuilder().withUuid(this.depID + i).build());
        }

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());

        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(rep
                .readEntity(Document.class));
        final List<Deployment> list = XMLConverter
                .convertJDOMElementToList(elem, new DeploymentFactory(false));
        Assert.assertEquals(numDeps, list.size());
    }

    @Test
    public void testPostDeploymentResponse() throws CacheException, ParseException, JDOMException,
            MissingPolicyException, DeployerException {
        this.createAppWithTemplates();
        final Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(this.templateID1, this.num1);
        map.put(this.templateID2, this.num2);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(map),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().post(entity);

        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), rep.getStatus());
        Assert.assertNotNull(rep.getHeaderString(AppsProvider.CONTENT_LOCATION_HEADER));
    }

    @Test
    public void testPostDeploymentInternalInfo() throws CacheException, ParseException,
            JDOMException, MissingPolicyException, DeployerException {
        this.createAppWithTemplates();
        final Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(this.templateID1, this.num1);
        map.put(this.templateID2, this.num2);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(map),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().post(entity);
        Assert.assertEquals(this.mockedDeployment, this.app.getDeployment(this.depID));
    }

    @Test
    public void testPostDeploymentCacheException() throws CacheException, ParseException,
            MissingPolicyException, DeployerException, JDOMException {
        this.createAppWithTemplatesCacheException();
        final Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(this.templateID1, this.num1);
        map.put(this.templateID2, this.num2);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(map),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().post(entity);

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testPostDeploymentParseException() throws CacheException, ParseException,
            MissingPolicyException, DeployerException, JDOMException {
        this.createAppWithTemplates();

        final Entity<Document> entity = Entity
                .entity(XMLConverter.convertJDOMElementW3CDocument(new Element("hallo")),
                        MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().post(entity);

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testPostDeploymentDeployerException() throws CacheException, ParseException,
            MissingPolicyException, DeployerException, JDOMException {
        this.createAppWithTemplatesDeployerException();
        final Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(this.templateID1, this.num1);
        map.put(this.templateID2, this.num2);

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(map),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).request().post(entity);

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testGetNotExistingDeployment() throws CacheException, ParseException,
            MissingPolicyException, DeployerException {
        this.createAppWithTemplates();

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(DeploymentsProvider.PATH).path("THISIDDOESNOTEXIST").request().get();

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rep.getStatus());
    }

}
