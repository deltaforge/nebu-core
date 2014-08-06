package nl.bitbrains.nebu.rest.server;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.VirtualMachineBuilder;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.topology.PhysicalDataCenter;
import nl.bitbrains.nebu.common.topology.PhysicalDataCenterBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalHostBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalRack;
import nl.bitbrains.nebu.common.topology.PhysicalRackBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalRoot;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactories;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentBuilder;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;
import nl.bitbrains.nebu.containers.VMDeploymentSpecificationBuilder;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateBuilder;
import nl.bitbrains.nebu.rest.client.RequestSender;
import nl.bitbrains.nebu.rest.server.AppsProvider;
import nl.bitbrains.nebu.rest.server.PhysicalMachineProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
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
@PrepareForTest({ RequestSender.class, CacheManager.class })
@PowerMockIgnore("javax.management.*")
public class ITPhysProvider extends JerseyTest {

    private final String appID = "appid";
    private final String templateID1 = "templateId1";
    private final String deploymentID1 = "deploymentId1";
    private final String vmID = "vmID";
    private final String hostID = "hostID";
    private final int numVms1 = 1;

    private RequestSender reqSender;

    private Application app;
    private VMTemplate template1;
    private Deployment deployment1;
    private List<String> vmIds;

    public static boolean deepEquals(final PhysicalTopology one, final PhysicalTopology two) {
        return ITPhysProvider.deepEquals(one.getRoot(), two.getRoot());
    }

    private static boolean deepEquals(final PhysicalRoot root, final PhysicalRoot root2) {
        if (!root.equals(root2) || root.getDataCenters().size() != root2.getDataCenters().size()) {
            return false;
        }
        final List<PhysicalDataCenter> two = root2.getDataCenters();
        for (final PhysicalDataCenter d : root.getDataCenters()) {
            final int x = two.indexOf(d);
            if (x < 0 || !ITPhysProvider.deepEquals(d, two.get(x))) {
                return false;
            }
        }
        return true;
    }

    private static boolean deepEquals(final PhysicalDataCenter dc1, final PhysicalDataCenter dc2) {
        if (!dc1.equals(dc2) || dc1.getRacks().size() != dc2.getRacks().size()) {
            return false;
        }
        final List<PhysicalRack> two = dc2.getRacks();
        for (final PhysicalRack d : dc1.getRacks()) {
            final int x = two.indexOf(d);
            if (x < 0 || !ITPhysProvider.deepEquals(d, two.get(x))) {
                return false;
            }
        }
        return true;
    }

    private static boolean deepEquals(final PhysicalRack r1, final PhysicalRack r2) {
        if (!r1.equals(r2) || r1.getDisks().size() != r2.getDisks().size()
                || r1.getCPUs().size() != r2.getCPUs().size()) {
            return false;
        }
        final List<PhysicalHost> two = r2.getCPUs();
        for (final PhysicalHost d : r1.getCPUs()) {
            final int x = two.indexOf(d);
            if (x < 0 || !ITPhysProvider.deepEquals(d, two.get(x))) {
                return false;
            }
        }

        final List<PhysicalStore> disks2 = r2.getDisks();
        for (final PhysicalStore d : r1.getDisks()) {
            final int x = disks2.indexOf(d);
            if (x < 0 || !ITPhysProvider.deepEquals(d, disks2.get(x))) {
                return false;
            }
        }
        return true;
    }

    private static boolean deepEquals(final PhysicalHost h1, final PhysicalHost h2) {
        if (!h1.equals(h2) || h1.getDisks().size() != h2.getDisks().size()) {
            return false;
        }
        final List<PhysicalStore> disks = h2.getDisks();
        for (final PhysicalStore d : h1.getDisks()) {
            final int x = disks.indexOf(d);
            if (x < 0 || !ITPhysProvider.deepEquals(d, disks.get(x))) {
                return false;
            }
        }
        return true;
    }

    private static boolean deepEquals(final PhysicalStore s1, final PhysicalStore s2) {
        if (!s1.equals(s2)) {
            return false;
        }
        return s1.getCapacity() == s2.getCapacity();
    }

    @Override
    protected javax.ws.rs.core.Application configure() {
        return new ResourceConfig(AppsProvider.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.vmIds = new ArrayList<String>();
        this.setUpTopologyMock();
        this.setUpApp();
        PowerMockito.mockStatic(CacheManager.class);
    }

    private void setUpApp() throws CacheException {
        this.app = new ApplicationBuilder().withUuid(this.appID).build();
        this.template1 = new VMTemplateBuilder().withUuid(this.templateID1).build();
        this.app.putVMTemplate(this.template1);
        this.deployment1 = new DeploymentBuilder().withLaunched(true).withUuid(this.deploymentID1)
                .build();
        this.app.putDeployment(this.deployment1);
        this.setVMsForDeployment(this.deployment1, this.numVms1);
    }

    private void setVMsForDeployment(final Deployment dep, final int numVms) throws CacheException {
        final Map<VirtualMachine, VMDeploymentSpecification> vms = new HashMap<VirtualMachine, VMDeploymentSpecification>();
        for (int i = 0; i < numVms; i++) {
            final VirtualMachine x = new VirtualMachineBuilder().withUuid(this.vmID + i)
                    .withStatus(VirtualMachine.Status.ON).withHost(this.hostID + i).build();
            vms.put(x, new VMDeploymentSpecificationBuilder().withTemplate(this.template1)
                    .withHost(this.hostID + i).build());
            Mockito.when(this.reqSender.getVirtualMachine(this.vmID + i)).thenReturn(x);
            this.vmIds.add(this.vmID + i);
        }
        dep.setVirtualMachines(vms);
    }

    private void setUpTopologyMock() {
        PowerMockito.mockStatic(RequestSender.class);
        this.reqSender = PowerMockito.mock(RequestSender.class);
        PowerMockito.when(RequestSender.get()).thenReturn(this.reqSender);
    }

    private void setTopologyMockResponse(final String template, final PhysicalTopology topology)
            throws CacheException {
        Mockito.when(this.reqSender.getVMTemplateTopologyOptions(template)).thenReturn(topology);
    }

    private void setTopologyMockException(final Exception e) throws CacheException {
        Mockito.when(this.reqSender.getVMTemplateTopologyOptions(Matchers.anyString()))
                .thenThrow(e);
    }

    private void setVirtualMachineRequestSenderResponseMock(final List<String> vms)
            throws CacheException {
        Mockito.when(this.reqSender.getVirtualMachines()).thenReturn(vms);
    }

    private void setUpCacheManager(final String key, final Object value) throws CacheException {
        Mockito.when(CacheManager.get(key)).thenReturn(value);
    }

    private PhysicalTopology constructSimpleTopology() {
        final PhysicalTopology topology = new PhysicalTopology();
        final PhysicalDataCenter datacenter1 = new PhysicalDataCenterBuilder().withUuid("dc1")
                .build();
        final PhysicalRack rack1 = new PhysicalRackBuilder().withUuid("rack1").build();
        final PhysicalHost cpu1 = new PhysicalHostBuilder().withUuid(this.hostID + "0").build();
        topology.addDataCenter(datacenter1);
        topology.addRackToDataCenter(rack1, datacenter1);
        topology.addCPUToRack(cpu1, rack1);
        return topology;
    }

    private PhysicalTopology constructFilledTopology() {
        final PhysicalTopology topology = new PhysicalTopology();
        final PhysicalDataCenter datacenter1 = new PhysicalDataCenterBuilder().withUuid("dc1")
                .build();
        final PhysicalDataCenter datacenter2 = new PhysicalDataCenterBuilder().withUuid("dc2")
                .build();
        final PhysicalRack rack1 = new PhysicalRackBuilder().withUuid("rack1").build();
        final PhysicalRack rack2 = new PhysicalRackBuilder().withUuid("rack2").build();
        final PhysicalRack rack3 = new PhysicalRackBuilder().withUuid("rack3").build();
        final PhysicalHost cpu1 = new PhysicalHostBuilder().withUuid(this.hostID + "0").build();
        final PhysicalHost cpu2 = new PhysicalHostBuilder().withUuid(this.hostID + "1").build();
        final PhysicalHost cpu3 = new PhysicalHostBuilder().withUuid(this.hostID + "2").build();
        final PhysicalHost cpu4 = new PhysicalHostBuilder().withUuid(this.hostID + "3").build();
        final PhysicalHost cpu5 = new PhysicalHostBuilder().withUuid(this.hostID + "4").build();
        final PhysicalHost cpu6 = new PhysicalHostBuilder().withUuid(this.hostID + "5").build();
        topology.addDataCenter(datacenter1);
        topology.addDataCenter(datacenter2);
        topology.addRackToDataCenter(rack1, datacenter1);
        topology.addRackToDataCenter(rack2, datacenter2);
        topology.addRackToDataCenter(rack3, datacenter2);
        topology.addCPUToRack(cpu1, rack1);
        topology.addCPUToRack(cpu2, rack2);
        topology.addCPUToRack(cpu3, rack2);
        topology.addCPUToRack(cpu4, rack2);
        topology.addCPUToRack(cpu5, rack2);
        topology.addCPUToRack(cpu6, rack3);
        return topology;
    }

    @Test
    public void successfullResponseTopologySingleTemplateSingleVM() throws CacheException {
        final Map<String, Application> map = new HashMap<String, Application>();
        map.put(this.appID, this.app);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
        this.setTopologyMockResponse(this.templateID1, this.constructFilledTopology());
        this.setVirtualMachineRequestSenderResponseMock(this.vmIds);

        final Response rep = this.target(AppsProvider.PATH).path(this.appID)
                .path(PhysicalMachineProvider.PATH).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), rep.getStatus());
    }

    @Test
    public void successfullContentTopologySingleTemplateSingleVM() throws CacheException,
            ParseException {
        final Map<String, Application> map = new HashMap<String, Application>();
        map.put(this.appID, this.app);
        this.setUpCacheManager(AppsProvider.CACHE_KEY_APPS, map);
        this.setTopologyMockResponse(this.templateID1, this.constructFilledTopology());
        this.setVirtualMachineRequestSenderResponseMock(this.vmIds);

        final Response rep = this.target(AppsProvider.PATH).path(this.appID)
                .path(PhysicalMachineProvider.PATH).request().get();

        final TopologyFactories factories = TopologyFactories.createDefault();
        final PhysicalTopology actual = new PhysicalTopology(
                factories
                        .getPhysicalRootFactory()
                        .fromXML(XMLConverter.convertW3CDocumentJDOMElement(rep
                                .readEntity(Document.class))).build());

        Assert.assertTrue(ITPhysProvider.deepEquals(this.constructSimpleTopology(), actual));
    }
}
