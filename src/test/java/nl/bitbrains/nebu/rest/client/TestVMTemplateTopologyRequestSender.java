package nl.bitbrains.nebu.rest.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.config.ClientConfiguration;
import nl.bitbrains.nebu.common.config.Configuration;
import nl.bitbrains.nebu.common.config.InvalidConfigurationException;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactories;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestBuilder;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Before;
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

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 *         Tests for the {@link RequestSender#getTopology()} method.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Configuration.class, ClientConfiguration.class })
@PowerMockIgnore("javax.management.*")
public class TestVMTemplateTopologyRequestSender extends JerseyTest {

    private static final String tempID = "id";

    @Mock
    private Configuration config;
    @Mock
    private ClientConfiguration clientConfig;

    @Path(RequestBuilder.URI_VMTEMPLATES + "/{id}/" + RequestBuilder.URI_PHYSICAL_MACHINES)
    public static class TopologyResource {
        public static TestServerStatus status = TestServerStatus.OK;
        public static PhysicalTopology inputTopology;

        @GET
        public Response getTopology(@PathParam("id") final String id) throws JDOMException {
            Response rep = null;
            if (TopologyResource.status == TestServerStatus.INTERNAL_SERVER_ERROR) {
                return Response.serverError().build();
            } else if (TopologyResource.status == TestServerStatus.WRONG_RESPONSE_TYPE) {
                rep = Response.ok("<hallo/>", MediaType.APPLICATION_XML).build();
            } else {
                TopologyResource.inputTopology = new PhysicalTopology();
                final TopologyFactories factories = TopologyFactories.createDefault();
                rep = Response.ok(XMLConverter.convertJDOMElementW3CDocument(factories
                                          .getPhysicalRootFactory()
                                          .toXML(TopologyResource.inputTopology.getRoot())),
                                  MediaType.APPLICATION_XML).build();
            }
            return rep;
        }
    }

    @Override
    protected Application configure() {
        final ResourceConfig rc = new ResourceConfig(TopologyResource.class);
        return rc;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        this.setUpConfigMock("localhost", this.getPort());

        CacheManager.resetCache();
    }

    private void setUpConfigMock(final String ipAddress, final int port) {
        PowerMockito.mockStatic(Configuration.class);
        Mockito.when(Configuration.get()).thenReturn(this.config);
        Mockito.when(this.config.getClientConfig(Matchers.anyString()))
                .thenReturn(this.clientConfig);
        Mockito.when(this.clientConfig.getIpAddress()).thenReturn(ipAddress);
        Mockito.when(this.clientConfig.getPort()).thenReturn(port);
    }

    @Test
    public void testBasicGetTopologyRequest() throws CacheException {
        TopologyResource.status = TestServerStatus.OK;
        final PhysicalTopology topology = RequestSender.get()
                .getVMTemplateTopologyOptions(TestVMTemplateTopologyRequestSender.tempID);
        Assert.assertEquals(TopologyResource.inputTopology, topology);
    }

    @Test
    public void testWrongIPTopologyRequest() throws InvalidConfigurationException, CacheException {
        this.setUpConfigMock("thisisawrongip", this.getPort());
        boolean caught = false;
        try {
            RequestSender.get()
                    .getVMTemplateTopologyOptions(TestVMTemplateTopologyRequestSender.tempID);
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testServerErrorTopologyRequest() throws CacheException {
        TopologyResource.status = TestServerStatus.INTERNAL_SERVER_ERROR;
        boolean caught = false;
        try {
            RequestSender.get()
                    .getVMTemplateTopologyOptions(TestVMTemplateTopologyRequestSender.tempID);
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testWrongResponseTopologyRequest() throws CacheException {
        TopologyResource.status = TestServerStatus.WRONG_RESPONSE_TYPE;
        boolean caught = false;
        try {
            RequestSender.get()
                    .getVMTemplateTopologyOptions(TestVMTemplateTopologyRequestSender.tempID);
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }
}
