package nl.bitbrains.nebu.rest.client;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.VirtualMachineBuilder;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.config.ClientConfiguration;
import nl.bitbrains.nebu.common.config.Configuration;
import nl.bitbrains.nebu.common.config.InvalidConfigurationException;
import nl.bitbrains.nebu.common.factories.StringFactory;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
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
 *         Tests for the {@link RequestSender#getVirtualMachines()} method.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Configuration.class, ClientConfiguration.class })
@PowerMockIgnore("javax.management.*")
public class TestVMsRequestSender extends JerseyTest {
    @Mock
    private Configuration config;
    @Mock
    private ClientConfiguration clientConfig;

    @Path(RequestBuilder.URI_VIRTUAL_MACHINES)
    public static class VirtualMachinesResource {
        public static TestServerStatus status;
        public static List<String> inputVms = new ArrayList<String>();
        public static VirtualMachine inputVm = new VirtualMachineBuilder().withUuid("uuid")
                .build();

        @GET
        public Response getVirtualMachines() throws JDOMException {
            Response rep = null;
            if (VirtualMachinesResource.status == TestServerStatus.INTERNAL_SERVER_ERROR) {
                return Response.serverError().build();
            } else if (VirtualMachinesResource.status == TestServerStatus.WRONG_RESPONSE_TYPE) {
                rep = Response.ok("<hello/>", MediaType.APPLICATION_XML).build();
            } else {
                VirtualMachinesResource.inputVms = new ArrayList<String>();
                VirtualMachinesResource.inputVms.add("uuid");
                rep = Response
                        .ok(XMLConverter.convertJDOMElementW3CDocument(XMLConverter
                                    .convertCollectionToJDOMElement(VirtualMachinesResource.inputVms,
                                                                    new StringFactory())),
                            MediaType.APPLICATION_XML).build();
            }
            return rep;
        }

        @GET
        @Path("{uuid}")
        public Response getVirtualMachineInfo(@PathParam("uuid") final String uuid)
                throws JDOMException {
            Response rep = null;
            if (VirtualMachinesResource.status == TestServerStatus.INTERNAL_SERVER_ERROR) {
                return Response.serverError().build();
            } else if (VirtualMachinesResource.status == TestServerStatus.WRONG_RESPONSE_TYPE) {
                rep = Response.ok("<hello/>", MediaType.APPLICATION_XML).build();
            } else if (VirtualMachinesResource.status == TestServerStatus.PAGE_NOT_FOUND) {
                rep = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                VirtualMachinesResource.inputVm = new VirtualMachineBuilder().withUuid(uuid)
                        .withHost("host").build();
                final VirtualMachineFactory factory = new VirtualMachineFactory();
                rep = Response.ok(XMLConverter.convertJDOMElementW3CDocument(factory
                                          .toXML(VirtualMachinesResource.inputVm)),
                                  MediaType.APPLICATION_XML).build();
            }
            return rep;
        }
    }

    @Override
    protected Application configure() {
        final ResourceConfig rc = new ResourceConfig(VirtualMachinesResource.class);
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
    public void testBasicGetVMsRequest() throws CacheException {
        VirtualMachinesResource.status = TestServerStatus.OK;
        final List<String> list = RequestSender.get().getVirtualMachines();
        Assert.assertEquals(VirtualMachinesResource.inputVms, list);
    }

    @Test
    public void testWrongIPVMsRequest() throws InvalidConfigurationException, CacheException {
        this.setUpConfigMock("thisisawrongip", this.getPort());
        boolean caught = false;
        try {
            RequestSender.get().getVirtualMachines();
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testServerErrorVMsRequest() throws CacheException {
        VirtualMachinesResource.status = TestServerStatus.INTERNAL_SERVER_ERROR;
        boolean caught = false;
        try {
            RequestSender.get().getVirtualMachines();
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testWrongResponseVMsRequest() throws CacheException {
        VirtualMachinesResource.status = TestServerStatus.WRONG_RESPONSE_TYPE;
        boolean caught = false;
        try {
            RequestSender.get().getVirtualMachines();
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
    }

    @Test
    public void testBasicGetVMRequest() throws CacheException {
        VirtualMachinesResource.status = TestServerStatus.OK;
        final VirtualMachine vm = RequestSender.get().getVirtualMachine("uuid");
        Assert.assertEquals(VirtualMachinesResource.inputVm, vm);
    }

    @Test
    public void testWrongIPVMRequest() throws InvalidConfigurationException, CacheException {
        this.setUpConfigMock("thisisawrongip", this.getPort());
        boolean caught = false;
        try {
            RequestSender.get().getVirtualMachine("uuid");
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testServerErrorVMRequest() throws CacheException {
        VirtualMachinesResource.status = TestServerStatus.INTERNAL_SERVER_ERROR;
        boolean caught = false;
        try {
            RequestSender.get().getVirtualMachine("uuid");
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testWrongResponseVMRequest() throws CacheException {
        VirtualMachinesResource.status = TestServerStatus.WRONG_RESPONSE_TYPE;
        boolean caught = false;
        try {
            RequestSender.get().getVirtualMachine("uuid");
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testPageNotFoundVMRequest() throws CacheException {
        VirtualMachinesResource.status = TestServerStatus.PAGE_NOT_FOUND;
        boolean caught = false;
        try {
            RequestSender.get().getVirtualMachine("uuid");
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }
}
