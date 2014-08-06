package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.topology.PhysicalDataCenter;
import nl.bitbrains.nebu.common.topology.PhysicalDataCenterBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalHostBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalRack;
import nl.bitbrains.nebu.common.topology.PhysicalRackBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalStoreBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.DeploymentRequest;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateBuilder;
import nl.bitbrains.nebu.deployer.DeployerException;
import nl.bitbrains.nebu.deployer.DeployerPolicy;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@PrepareForTest(DeploymentRequest.class)
public abstract class PolicyTest {

    private DeployerPolicy policy;
    private Map<VMTemplate, Integer> map;
    private List<String> cpus;
    private List<VMDeploymentSpecification> specs;

    @Mock
    protected RequestSender reqSender;
    private boolean requires2Racks = false;

    protected void setRequires2Racks(final boolean requires2Racks) {
        this.requires2Racks = requires2Racks;
    }

    protected void setPolicy(final DeployerPolicy policy) {
        this.policy = policy;
    }

    protected DeployerPolicy getPolicy() {
        return this.policy;
    }

    /**
     * @return the specs
     */
    public final List<VMDeploymentSpecification> getSpecs() {
        return this.specs;
    }

    protected void setUp() {
        MockitoAnnotations.initMocks(this);
        this.map = new HashMap<VMTemplate, Integer>();
        this.cpus = new ArrayList<String>();
        PowerMockito.mockStatic(RequestSender.class);
        PowerMockito.when(RequestSender.get()).thenReturn(this.reqSender);
    }

    protected VMTemplate mockTemplate(final String id, final int numVms,
            final PhysicalTopology topology) throws CacheException {
        final VMTemplate template = new VMTemplateBuilder().withUuid(id).build();
        PowerMockito.when(this.reqSender.getVMTemplateTopologyOptions(id)).thenReturn(topology);
        this.map.put(template, numVms);
        return template;
    }

    protected DeploymentRequest mockRequest() {
        final DeploymentRequest depRequest = Mockito.mock(DeploymentRequest.class);
        Mockito.when(depRequest.getApplication()).thenReturn(this.mockApp());
        Mockito.when(depRequest.getTemplateRequests()).thenReturn(this.map);
        return depRequest;
    }

    protected DeploymentRequest mockRequest(final Application app) {
        final DeploymentRequest depRequest = Mockito.mock(DeploymentRequest.class);
        Mockito.when(depRequest.getApplication()).thenReturn(app);
        Mockito.when(depRequest.getTemplateRequests()).thenReturn(this.map);
        return depRequest;
    }

    /**
     * @return
     */
    protected Application mockApp() {
        final Application app = PowerMockito.mock(Application.class);
        return app;
    }

    protected final PhysicalTopology createTopology(final int numDcs, final int numRacks,
            final int numCpus, final int numNetworkDisks, final int numLocalDisks,
            final String prefix) {
        final PhysicalTopology result = new PhysicalTopology();
        for (int i = 0; i < numDcs; i++) {
            final PhysicalDataCenter dc = new PhysicalDataCenterBuilder().withUuid(prefix + "-dc"
                    + i).build();
            result.addDataCenter(dc);
            for (int j = 0; j < numRacks; j++) {
                final PhysicalRack rack = new PhysicalRackBuilder().withUuid(prefix + "-rack" + i
                        + "-" + j).build();
                result.addRackToDataCenter(rack, dc);
                for (int k = 0; k < numCpus; k++) {
                    final PhysicalHost host = new PhysicalHostBuilder().withUuid(prefix + "-host"
                            + i + "-" + j + "-" + k).build();
                    this.cpus.add(host.getUniqueIdentifier());
                    result.addCPUToRack(host, rack);
                    for (int l = 0; l < numLocalDisks; l++) {
                        final PhysicalStore disk = new PhysicalStoreBuilder().withCapacity(1)
                                .withUuid(prefix + "-disk" + i + "-" + j + "-" + k + "-" + l)
                                .build();
                        result.addDiskToHost(disk, host);
                    }
                }
                for (int k = 0; k < numNetworkDisks; k++) {
                    final PhysicalStore disk = new PhysicalStoreBuilder().withCapacity(1)
                            .withUuid(prefix + "-disk" + i + "-" + j + "-" + k).build();
                    result.addDiskToRack(disk, rack);
                }
            }
        }
        return result;
    }

    @Test
    public void testGenerateDeploymentEmptyRequest() throws DeployerException {
        final DeploymentRequest emptyRequest = PowerMockito.mock(DeploymentRequest.class);
        final List<VMDeploymentSpecification> list = this.getPolicy()
                .generateDeployment(emptyRequest);
        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void testGenerateDeploymentNullRequest() throws DeployerException {
        boolean caught = false;
        try {
            this.getPolicy().generateDeployment(null);
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    protected int generateDeployment(final String[] ids, final int[] numsVms, final int[] numsCPUs)
            throws CacheException, DeployerException {
        int expectedSum = 0;
        for (int i = 0; i < ids.length; i++) {
            this.mockTemplate(ids[i],
                              numsVms[i],
                              this.createTopology(1, 1, numsCPUs[i], 1, 1, ids[i]));
            expectedSum += numsVms[i];
        }
        this.specs = this.getPolicy().generateDeployment(this.mockRequest());
        return expectedSum;
    }

    protected int generateDeployment(final String[] ids, final int[] numsVms,
            final PhysicalTopology topology) throws CacheException, DeployerException {
        int expectedSum = 0;
        for (int i = 0; i < ids.length; i++) {
            this.mockTemplate(ids[i], numsVms[i], topology);
            expectedSum += numsVms[i];
        }
        this.specs = this.getPolicy().generateDeployment(this.mockRequest());
        return expectedSum;
    }

    protected void generateDeployment(final DeploymentRequest depRequest) throws CacheException,
            DeployerException {
        this.specs = this.getPolicy().generateDeployment(depRequest);
    }

    protected void testGenerateDeployment(final String[] ids, final int[] numsVms,
            final int[] numsCPUs) throws CacheException, DeployerException {
        if (this.requires2Racks) {
            boolean caught = false;
            try {
                this.generateDeployment(ids, numsVms, numsCPUs);
            } catch (final DeployerException e) {
                caught = true;
            }
            Assert.assertTrue(caught);
        } else {
            final int expectedSum = this.generateDeployment(ids, numsVms, numsCPUs);
            final List<String> gottenCPUs = new ArrayList<String>();
            for (final VMDeploymentSpecification spec : this.specs) {
                gottenCPUs.add(spec.getHost());
            }
            Assert.assertFalse(this.specs.isEmpty());
            Assert.assertEquals(expectedSum, this.specs.size());
            org.hamcrest.MatcherAssert.assertThat(gottenCPUs, org.hamcrest.Matchers
                    .everyItem(org.hamcrest.Matchers.isIn(this.cpus)));
        }
    }

    // It is impossible to combine JUnitParamsRunner with PowerMockito :(
    @Test
    public void testGenerateDeploymentOneOneOne() throws CacheException, DeployerException {
        this.testGenerateDeployment(new String[] { "id" }, new int[] { 1 }, new int[] { 1 });
    }

    @Test
    public void testGenerateDeploymentOneOneTwo() throws CacheException, DeployerException {
        this.testGenerateDeployment(new String[] { "id" }, new int[] { 1 }, new int[] { 2 });
    }

    @Test
    public void testGenerateDeploymentOneOneMultiple() throws CacheException, DeployerException {
        this.testGenerateDeployment(new String[] { "id" }, new int[] { 1 }, new int[] { 8 });
    }

    @Test
    public void testGenerateDeploymentTwoOneOne() throws CacheException, DeployerException {
        this.testGenerateDeployment(new String[] { "id1", "id2" }, new int[] { 1, 1 }, new int[] {
                1, 1 });
    }

    @Test
    public void testGenerateDeploymentTwoOneTwo() throws CacheException, DeployerException {
        this.testGenerateDeployment(new String[] { "id1", "id2" }, new int[] { 1, 1 }, new int[] {
                2, 2 });
    }

    @Test
    public void testGenerateDeploymentTwoOneMultiple() throws CacheException, DeployerException {
        this.testGenerateDeployment(new String[] { "id1", "id2" }, new int[] { 1, 1 }, new int[] {
                8, 4 });
    }

    @Test
    public void testGenerateDeploymentMultipleTwoOne() throws CacheException, DeployerException {
        this.testGenerateDeployment(new String[] { "id1", "id2" }, new int[] { 2, 2 }, new int[] {
                1, 1 });
    }

    @Test
    public void testGenerateDeploymentMultipleTwoMultiple() throws CacheException,
            DeployerException {
        this.testGenerateDeployment(new String[] { "id1", "id2" }, new int[] { 2, 2 }, new int[] {
                4, 8 });
    }

    @Test
    public void testGenerateDeploymentMultipleMultipleMultiple() throws CacheException,
            DeployerException {
        this.testGenerateDeployment(new String[] { "id1", "id2" }, new int[] { 8, 5 }, new int[] {
                4, 8 });
    }

    @Test
    public void testGenerateDeploymentOneOneZero() throws CacheException, DeployerException {
        boolean caught = false;
        try {
            this.generateDeployment(new String[] { "id1" }, new int[] { 1 }, new int[] { 0 });
        } catch (final DeployerException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }
}
