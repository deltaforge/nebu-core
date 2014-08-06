package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.VirtualMachineBuilder;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentBuilder;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.deployer.DeployerException;
import nl.bitbrains.nebu.deployer.policies.AggregatedLocalityPolicy;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RequestSender.class, Application.class, VMDeploymentSpecification.class })
@PowerMockIgnore("javax.management.*")
public class TestAggregatedLocalityPolicy extends PolicyTest {

    private final int numDcs = 2;
    private final int numRacks = 3;
    private final int numCpus = 4;
    private final int numNetworkDisks = 2;
    private final int numLocalDisks = 3;
    private final String prefix = "prefix";

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.setPolicy(new AggregatedLocalityPolicy());
    }

    @Test
    public void getSetMax() {
        final AggregatedLocalityPolicy policy = (AggregatedLocalityPolicy) this.getPolicy();
        policy.setMaxVMsPerHost(1);
        Assert.assertEquals(1, policy.getMaxVMsPerHost());
    }

    @Test
    public void testAllOnOneHostSimpleTopology() throws CacheException, DeployerException {
        this.generateDeployment(new String[] { "id" }, new int[] { 2 }, new int[] { 8 });
        Assert.assertEquals(this.getSpecs().get(0).getHost(), this.getSpecs().get(1).getHost());
    }

    @Test
    public void testAllOnOneHostNonSimpleTopology() throws CacheException, DeployerException {
        final PhysicalTopology topology = this.createTopology(this.numDcs,
                                                              this.numRacks,
                                                              this.numCpus,
                                                              this.numNetworkDisks,
                                                              this.numLocalDisks,
                                                              this.prefix);
        this.generateDeployment(new String[] { "id" }, new int[] { 2 }, topology);
        Assert.assertEquals(this.getSpecs().get(0).getHost(), this.getSpecs().get(1).getHost());
    }

    @Test
    public void testTwoHostsNonSimpleTopology() throws CacheException, DeployerException {
        final AggregatedLocalityPolicy policy = (AggregatedLocalityPolicy) this.getPolicy();
        policy.setMaxVMsPerHost(1);
        final PhysicalTopology topology = this.createTopology(this.numDcs,
                                                              this.numRacks,
                                                              this.numCpus,
                                                              this.numNetworkDisks,
                                                              this.numLocalDisks,
                                                              this.prefix);
        this.generateDeployment(new String[] { "id" }, new int[] { 2 }, topology);
        Assert.assertNotEquals(this.getSpecs().get(0).getHost(), this.getSpecs().get(1).getHost());
    }

    @Test
    public void testThreeHostsNonSimpleTopology() throws CacheException, DeployerException {
        final AggregatedLocalityPolicy policy = (AggregatedLocalityPolicy) this.getPolicy();
        policy.setMaxVMsPerHost(1);
        final PhysicalTopology topology = this.createTopology(this.numDcs,
                                                              this.numRacks,
                                                              this.numCpus,
                                                              this.numNetworkDisks,
                                                              this.numLocalDisks,
                                                              this.prefix);
        this.generateDeployment(new String[] { "id" }, new int[] { 3 }, topology);
        final Set<String> set = new HashSet<String>();
        for (final VMDeploymentSpecification spec : this.getSpecs()) {
            set.add(spec.getHost());
        }
        Assert.assertEquals(3, set.size());
    }

    @Test
    public void testNotEnoughHostsSimpleTopology() throws CacheException, DeployerException {
        final AggregatedLocalityPolicy policy = (AggregatedLocalityPolicy) this.getPolicy();
        policy.setMaxVMsPerHost(1);
        boolean caught = false;
        try {
            this.generateDeployment(new String[] { "id" }, new int[] { 3 }, new int[] { 1 });
        } catch (final DeployerException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testExistingVMsMakeItNotFitSimpleTopology() throws CacheException,
            DeployerException {
        final AggregatedLocalityPolicy policy = (AggregatedLocalityPolicy) this.getPolicy();
        policy.setMaxVMsPerHost(1);
        final PhysicalTopology topology = this.createTopology(1, 1, 1, 1, 1, this.prefix);
        final String hostname = topology.getCPUs().get(0).getUniqueIdentifier();
        final Application app = new ApplicationBuilder().withUuid("uuid").build();
        final List<Deployment> deps = new ArrayList<Deployment>();
        final List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        final List<String> vmIds = new ArrayList<String>();
        final Deployment dep = new DeploymentBuilder().withUuid("id").build();
        final VirtualMachine vm = new VirtualMachineBuilder().withUuid("vmId").withHost(hostname)
                .build();
        final VMDeploymentSpecification spec = PowerMockito.mock(VMDeploymentSpecification.class);
        final VMTemplate template = this.mockTemplate("id", 1, topology);
        vmIds.add(vm.getUniqueIdentifier());
        Mockito.when(spec.getTemplate()).thenReturn(template);
        Mockito.when(this.reqSender.getVirtualMachines()).thenReturn(vmIds);
        Mockito.when(this.reqSender.getVirtualMachine(vm.getUniqueIdentifier())).thenReturn(vm);
        vms.add(vm);
        dep.addVirtualMachines(vm, spec);
        deps.add(dep);
        app.putDeployment(dep);

        boolean caught = false;
        try {
            this.generateDeployment(this.mockRequest(app));
        } catch (final DeployerException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }
}
