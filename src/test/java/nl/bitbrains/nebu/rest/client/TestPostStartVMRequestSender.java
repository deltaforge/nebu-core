package nl.bitbrains.nebu.rest.client;

import java.net.URI;

import javax.ws.rs.POST;
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
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestBuilder;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.jdom2.Element;
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
public class TestPostStartVMRequestSender extends JerseyTest {

    private static final String uuid = "uuid";

    @Mock
    private Configuration config;
    @Mock
    private ClientConfiguration clientConfig;

    private Element xml;

    @Path(RequestBuilder.URI_PHYSICAL_MACHINES + "/{uuid}/" + RequestBuilder.URI_CREATE_VM)
    public static class StartResource {
        public static TestServerStatus status = TestServerStatus.OK;

        @POST
        public Response putTemplate(@PathParam("uuid") final String uuid) throws JDOMException {
            Response rep = null;
            if (StartResource.status == TestServerStatus.INTERNAL_SERVER_ERROR) {
                return Response.serverError().build();
            } else if (StartResource.status == TestServerStatus.WRONG_RESPONSE_TYPE) {
                rep = Response.ok("<hallo/>", MediaType.APPLICATION_XML).build();
            } else {
                rep = Response.created(URI.create(uuid)).build();
            }
            return rep;
        }
    }

    @Override
    protected Application configure() {
        final ResourceConfig rc = new ResourceConfig(StartResource.class);
        return rc;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        this.setUpConfigMock("localhost", this.getPort());

        CacheManager.resetCache();
        this.xml = new Element("root");
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
    public void testBasicPostStartRequest() throws CacheException {
        StartResource.status = TestServerStatus.OK;
        RequestSender.get().postCreateVM("uuid", "host", "temp");
        // Fails if an exception is thrown.
    }

    @Test
    public void testBasicPostStartWithDiskRequest() throws CacheException {
        StartResource.status = TestServerStatus.OK;
        RequestSender.get().postCreateVM("uuid", "host", "temp", "disk");
        // Fails if an exception is thrown.
    }

    @Test
    public void testWrongIPPostStartRequest() throws InvalidConfigurationException, CacheException {
        this.setUpConfigMock("thisisawrongip", this.getPort());
        boolean caught = false;
        try {
            RequestSender.get().postCreateVM("uuid", "host", "temp");
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testServerErrorPostStartRequest() throws CacheException {
        StartResource.status = TestServerStatus.INTERNAL_SERVER_ERROR;
        boolean caught = false;
        try {
            RequestSender.get().postCreateVM("uuid", "host", "temp");
        } catch (final RESTRequestException e) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                e.getHttpCode());
            caught = true;
        }
        Assert.assertTrue(caught);
    }

}
