package nl.bitbrains.nebu.rest.server;

import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.topology.factory.PhysicalHostFactory;
import nl.bitbrains.nebu.common.topology.factory.PhysicalStoreFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentBuilder;
import nl.bitbrains.nebu.containers.DeploymentFactory;
import nl.bitbrains.nebu.containers.DeploymentRequest;
import nl.bitbrains.nebu.containers.DeploymentRequestFactory;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;
import nl.bitbrains.nebu.containers.VMDeploymentSpecificationBuilder;
import nl.bitbrains.nebu.containers.VMDeploymentSpecificationFactory;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateBuilder;
import nl.bitbrains.nebu.containers.VMTemplateFactory;
import nl.bitbrains.nebu.deployer.Deployer;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestSender;
import nl.bitbrains.nebu.rest.server.AppsProvider;
import nl.bitbrains.nebu.rest.server.DeploymentProvider;
import nl.bitbrains.nebu.rest.server.DeploymentsProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
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
@PrepareForTest({ RequestSender.class, CacheManager.class, Deployer.class, DeploymentRequest.class })
@PowerMockIgnore("javax.management.*")
public class TestDeploymentProvider extends JerseyTest {

    @Mock
    private RequestSender reqSender;

    @Mock
    private PhysicalTopology topology;

    private nl.bitbrains.nebu.containers.Application app;
    private DeploymentRequestFactory depFactory;
    private Deployment dep;
    private Deployment otherDep;

    @Mock
    private DeploymentRequest depReq;
    private Deployer deployer;

    private final Element elem = new Element("deployment");
    private final Element reqElem = new Element("deploymentRequest");

    private Document doc;
    private Document reqDoc;

    private final String appUUID = "appuuid";
    private final String depID = "depID";
    private final String templateID = "templateID";
    private final String hostID = "hostID";

    @Override
    protected javax.ws.rs.core.Application configure() {
        return new ResourceConfig(AppsProvider.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        PowerMockito.mockStatic(CacheManager.class);
        this.setUpReqSenderMock();
        final DOMOutputter converter = new DOMOutputter();
        final org.jdom2.Document jdomDoc = new org.jdom2.Document(this.elem);
        this.doc = converter.output(jdomDoc);
        this.reqDoc = converter.output(new org.jdom2.Document(this.reqElem));
        this.deployer = PowerMockito.mock(Deployer.class);
    }

    private void setUpReqSenderMock() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(RequestSender.class);
        PowerMockito.when(RequestSender.get()).thenReturn(this.reqSender);
    }

    private void setUpCacheManager(final String key, final Object value) throws CacheException {
        Mockito.when(CacheManager.get(key)).thenReturn(value);
    }

    private void setCacheMock() throws CacheException {
        this.setUpCacheManager(Deployer.CACHE_KEY, this.deployer);
    }

    private void setApplicationReal() {
        this.app = new ApplicationBuilder().withUuid(this.appUUID).withDeployment(this.dep)
                .build();
    }

    private void setDeploymentReal() {
        this.dep = new DeploymentBuilder().withUuid(this.depID).build();
    }

    private void setNonEmptyApplicationMap() throws CacheException {
        final Map<String, Application> map = new HashMap<String, Application>();
        map.put(this.appUUID, this.app);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
    }

    private void setDeployerMock() throws Exception {
        Mockito.when(this.deployer.generateDeployment(Matchers.any(DeploymentRequest.class)))
                .thenReturn(this.dep);
    }

    private void setLuanchReqSenderMock() throws RESTRequestException {
        Mockito.when(this.reqSender.postCreateVM(Matchers.anyString(),
                                                 Matchers.anyString(),
                                                 Matchers.anyString())).thenReturn(Response
                .created(URI.create("/a/a/a/a/")).build());
        Mockito.when(this.reqSender.postCreateVM(Matchers.anyString(),
                                                 Matchers.anyString(),
                                                 Matchers.anyString(),
                                                 Matchers.anyString())).thenReturn(Response
                .created(URI.create("/b/b/b/b/")).build());
    }

    private void setLaunchReqSenderMock(final Throwable e) throws Exception {
        Mockito.when(this.reqSender.postCreateVM(Matchers.anyString(),
                                                 Matchers.anyString(),
                                                 Matchers.anyString())).thenThrow(e);
    }

    private void addSpecToDep(final Deployment dep, final String templateId, final String host) {
        final VMTemplate template = new VMTemplateBuilder().withUuid(templateId)
                .withName(templateId).build();
        final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                .withTemplate(template).withHost(host).build();
        dep.addSpec(spec);
    }

    private void addSpecToDep(final Deployment dep, final String templateId, final String host,
            final String store) {
        final VMTemplate template = new VMTemplateBuilder().withUuid(templateId)
                .withName(templateId).build();
        final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                .withTemplate(template).withHost(host).withStore(store).build();
        dep.addSpec(spec);
    }

    private Document getPostRootBodyChangeDep() throws JDOMException {
        this.otherDep = new DeploymentBuilder()
                .withLaunched(false)
                .withUuid(this.depID)
                .withSpec(new VMDeploymentSpecificationBuilder()
                        .withHost("host")
                        .withTemplate(new VMTemplateBuilder().withUuid("templateID")
                                .withName("name").build()).build()).build();
        return XMLConverter.convertJDOMElementW3CDocument(new DeploymentFactory()
                .toXML(this.otherDep));
    }

    @Test
    public void testGet404Exception() throws CacheException {
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request().get();
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testGetSuccessResponse() throws CacheException {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testGetSuccessContent() throws CacheException, ParseException {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request().get();
        final Document gottenDoc = rep.readEntity(Document.class);
        final Deployment gottenDep = new DeploymentFactory()
                .fromXML(XMLConverter.convertW3CDocumentJDOMElement(gottenDoc)).build();
        Assert.assertEquals(this.depID, gottenDep.getUniqueIdentifier());
    }

    @Test
    public void testPost404Exception() throws CacheException {
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Entity<Document> entity = Entity.entity(this.doc, MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request().post(entity);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testPostChange403Exception() throws CacheException {
        this.setDeploymentReal();
        this.dep.setLaunched(true);
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Entity<Document> entity = Entity.entity(this.doc, MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request().post(entity);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testPostSuccesResponseAndInternals() throws CacheException, JDOMException {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Entity<Document> entity = Entity.entity(this.getPostRootBodyChangeDep(),
                                                      MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request().post(entity);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
        Assert.assertFalse(this.app.getDeployment(this.depID).getSpecs().isEmpty());
    }

    @Test
    public void testPostStart403Exception() throws CacheException {
        this.setDeploymentReal();
        this.dep.setLaunched(true);
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Entity<Document> entity = Entity.entity(this.doc, MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID)
                .path(DeploymentProvider.PATH_START).request().post(entity);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testPostStartNoSpecs() throws CacheException {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Entity<Document> entity = Entity.entity(this.doc, MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID)
                .path(DeploymentProvider.PATH_START).request().post(entity);
        Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), rep.getStatus());
        Assert.assertTrue(this.dep.isLaunched());
    }

    @Test
    public void testPostStartOneSpecNoStore() throws Exception {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        this.setDeployerMock();
        this.setLuanchReqSenderMock();
        this.addSpecToDep(this.dep, this.templateID, this.hostID);
        final Entity<Document> entity = Entity.entity(this.doc, MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID)
                .path(DeploymentProvider.PATH_START).request().post(entity);
        Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), rep.getStatus());
        Assert.assertEquals(1, this.dep.getVirtualMachines().size());
        Mockito.verify(this.reqSender).postCreateVM(Matchers.eq(this.hostID),
                                                    Matchers.startsWith(this.templateID),
                                                    Matchers.eq(this.templateID));
    }

    @Test
    public void testPostStartOneSpecRestReqException() throws Exception {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        this.setDeployerMock();
        final int errorCode = 123;
        this.setLaunchReqSenderMock(new RESTRequestException("", errorCode));
        this.addSpecToDep(this.dep, this.templateID, this.hostID);
        final Entity<Document> entity = Entity.entity(this.doc, MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID)
                .path(DeploymentProvider.PATH_START).request().post(entity);
        Assert.assertEquals(errorCode, rep.getStatus());
    }

    @Test
    public void testPostStartTwoSpecs() throws Exception {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        this.setDeployerMock();
        this.setLuanchReqSenderMock();
        this.addSpecToDep(this.dep, this.templateID, this.hostID);
        this.addSpecToDep(this.dep, this.templateID, this.hostID, this.hostID);
        final Entity<Document> entity = Entity.entity(this.doc, MediaType.APPLICATION_XML_TYPE);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID)
                .path(DeploymentProvider.PATH_START).request().post(entity);
        Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), rep.getStatus());
        Assert.assertEquals(2, this.dep.getVirtualMachines().size());
        Mockito.verify(this.reqSender).postCreateVM(Matchers.eq(this.hostID),
                                                    Matchers.startsWith(this.templateID),
                                                    Matchers.eq(this.templateID),
                                                    Matchers.eq(this.hostID));
    }

    @Test
    public void addVMToDeployment404() throws CacheException, JDOMException {
        this.setNonEmptyApplicationMap();
        this.setCacheMock();
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request()
                .put(this.createPUTEntity());
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rep.getStatus());
    }

    @Test
    public void addVMToDeployment403() throws CacheException, JDOMException {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request()
                .put(this.createPUTEntity());
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), rep.getStatus());
    }

    @Test
    public void addVMToDeployment400() throws CacheException, JDOMException {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.dep.setLaunched(true);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request()
                .put(this.createMalformedPUTEntity());
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rep.getStatus());
    }

    @Test
    public void addVMToDeployment200() throws CacheException, JDOMException {
        this.setDeploymentReal();
        this.setApplicationReal();
        this.setNonEmptyApplicationMap();
        this.dep.setLaunched(true);
        final Response rep = this.target(AppsProvider.PATH).path(this.appUUID)
                .path(DeploymentsProvider.PATH).path(this.depID).request()
                .put(this.createPUTEntity());
        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
    }

    private Entity<Document> createMalformedPUTEntity() throws JDOMException {
        final Element elem = new Element(VirtualMachineFactory.TAG_ELEMENT_ROOT);
        elem.setAttribute(IdentifiableFactory.TAG_ID, "id");
        final Element specElem = new Element(VMDeploymentSpecificationFactory.TAG_ELEMENT_ROOT);
        elem.addContent(specElem);
        final Element templateElem = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        templateElem.setAttribute(IdentifiableFactory.TAG_ID, "id");
        specElem.addContent(templateElem);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        return Entity.entity(doc, MediaType.APPLICATION_XML_TYPE);
    }

    private Entity<Document> createPUTEntity() throws JDOMException {
        final Element elem = new Element(VirtualMachineFactory.TAG_ELEMENT_ROOT);
        elem.setAttribute(IdentifiableFactory.TAG_ID, "id");
        final Element specElem = new Element(VMDeploymentSpecificationFactory.TAG_ELEMENT_ROOT);
        elem.addContent(specElem);
        final Element templateElem = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        final Element hostElem = new Element(PhysicalHostFactory.TAG_ELEMENT_ROOT);
        final Element storeElem = new Element(PhysicalStoreFactory.TAG_ELEMENT_ROOT);
        templateElem.setAttribute(IdentifiableFactory.TAG_ID, "id");
        templateElem.setAttribute(VMTemplateFactory.ATTRIBUTE_NAME, "name");
        hostElem.setAttribute(IdentifiableFactory.TAG_ID, "id");
        storeElem.setAttribute(IdentifiableFactory.TAG_ID, "id");
        specElem.addContent(templateElem).addContent(hostElem).addContent(storeElem);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        return Entity.entity(doc, MediaType.APPLICATION_XML_TYPE);
    }

}
