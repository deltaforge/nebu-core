package nl.bitbrains.nebu.rest.server;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateBuilder;
import nl.bitbrains.nebu.containers.VMTemplateFactory;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestSender;
import nl.bitbrains.nebu.rest.server.AppsProvider;
import nl.bitbrains.nebu.rest.server.VMTemplatesProvider;

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
public class TestVMTemplateProvider extends JerseyTest {
    @Mock
    private RequestSender reqSender;

    private Application app;

    private final String id = "id1";
    private final String name = "name";
    private final String templateID = "templateID";
    private final int cpu = 10;
    private final int mem = 20;
    private final int io = 30;
    private final int net = 40;

    private final String newName = "newName";
    private final int newCpu = 20;
    private final int newMem = 30;
    private final int newIo = 40;
    private final int newNet = 50;

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
        final Element root = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        final Attribute att = new Attribute(VMTemplateFactory.ATTRIBUTE_NAME, name);
        root.setAttribute(att);
        final Element loadProfile = this.makeLoadElement();
        root.addContent(loadProfile);
        final Element vmwareElem = new Element(VMTemplateFactory.TAG_VMMCONFIG);
        root.addContent(vmwareElem);
        return XMLConverter.convertJDOMElementW3CDocument(root);
    }

    private Element makeLoadElement() {
        final Element res = new Element(VMTemplateFactory.TAG_LOADPROFILE);
        final Element cpuElem = new Element(VMTemplateFactory.TAG_CPU).addContent(Integer
                .toString(this.newCpu));
        final Element memElem = new Element(VMTemplateFactory.TAG_MEM).addContent(Integer
                .toString(this.newMem));
        final Element ioElem = new Element(VMTemplateFactory.TAG_IO).addContent(Integer
                .toString(this.newIo));
        final Element netElem = new Element(VMTemplateFactory.TAG_NET).addContent(Integer
                .toString(this.newNet));
        res.addContent(cpuElem).addContent(memElem).addContent(ioElem).addContent(netElem);
        return res;
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

    private void setUpExistingVMTemplate() throws CacheException {
        final Map<String, Application> map = new HashMap<String, Application>();
        this.app = new ApplicationBuilder().withUuid(this.id).build();
        map.put(this.id, this.app);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
        this.app.putVMTemplate(new VMTemplateBuilder().withUuid(this.templateID)
                .withName(this.name).withCPU(this.cpu).withIO(this.io).withNet(this.net)
                .withMem(this.mem).build());
    }

    private VMTemplate performSuccessfullGETRequest() throws CacheException, ParseException {
        this.setUpExistingVMTemplate();
        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(VMTemplatesProvider.PATH).path(this.templateID).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());

        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(rep
                .readEntity(Document.class));
        return new VMTemplateFactory().fromXML(elem).build();
    }

    @Test
    public void testGetTemplateBasicInfo() throws CacheException, ParseException {
        final VMTemplate found = this.performSuccessfullGETRequest();
        Assert.assertEquals(this.templateID, found.getUniqueIdentifier());
        Assert.assertEquals(this.name, found.getName());
    }

    @Test
    public void testGetTemplatesProfileInfo() throws CacheException, ParseException {
        final VMTemplate found = this.performSuccessfullGETRequest();
        Assert.assertEquals(this.cpu, found.getCpu());
        Assert.assertEquals(this.mem, found.getMem());
        Assert.assertEquals(this.io, found.getIo());
        Assert.assertEquals(this.net, found.getNet());
    }

    @Test
    public void testPostVMTemplateResponse() throws CacheException, ParseException, JDOMException {
        this.setUpExistingVMTemplate();
        this.mockRequestSender();

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(this.newName),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(VMTemplatesProvider.PATH).path(this.templateID).request().post(entity);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
    }

    @Test
    public void testPostNewVMTemplateInternalInfo() throws CacheException, ParseException,
            JDOMException {
        this.setUpExistingVMTemplate();
        this.mockRequestSender();

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(this.newName),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(VMTemplatesProvider.PATH).path(this.templateID).request().post(entity);
        final VMTemplate template = this.app.getVMTemplate(this.templateID);
        Assert.assertEquals(this.newName, template.getName());
        Assert.assertEquals(this.newCpu, template.getCpu());
        Assert.assertEquals(this.newMem, template.getMem());
        Assert.assertEquals(this.newIo, template.getIo());
        Assert.assertEquals(this.newNet, template.getNet());
    }

    @Test
    public void testPostNewVMTemplateRestRequestException() throws CacheException, ParseException,
            JDOMException {
        this.setUpExistingVMTemplate();
        this.mockRequestSender(new RESTRequestException("", Status.BAD_GATEWAY.getStatusCode()));

        final Entity<Document> entity = Entity.entity(this.getPostRootBody(this.newName),
                                                      MediaType.APPLICATION_XML_TYPE);

        final Response rep = this.target(AppsProvider.PATH).path(this.id)
                .path(VMTemplatesProvider.PATH).path(this.templateID).request().post(entity);

        Assert.assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), rep.getStatus());
    }

}
