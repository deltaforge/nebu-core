package nl.bitbrains.nebu.deployer.policies;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.deployer.DeployerException;
import nl.bitbrains.nebu.deployer.policies.HDFSReplicationPolicy;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RequestSender.class, Application.class })
@PowerMockIgnore("javax.management.*")
public class TestHDFSReplicationPolicy extends PolicyTest {

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
        this.setPolicy(new HDFSReplicationPolicy());
    }

    @Test
    public void testWithNoLeftOvers() throws CacheException, DeployerException {
        this.generateDeployment(new String[] { "id" }, new int[] { 3 }, new int[] { 3 });
        Assert.assertEquals(this.getSpecs().get(0).getHost(), this.getSpecs().get(1).getHost());
        Assert.assertNotEquals(this.getSpecs().get(2).getHost(), this.getSpecs().get(1).getHost());
    }

    @Test
    public void testWithOnlyOneHost() throws CacheException, DeployerException {
        this.generateDeployment(new String[] { "id" }, new int[] { 3 }, new int[] { 3 });
        Assert.assertEquals(this.getSpecs().get(0).getHost(), this.getSpecs().get(1).getHost());
        Assert.assertNotEquals(this.getSpecs().get(2).getHost(), this.getSpecs().get(1).getHost());
    }

    @Test
    public void nonSimpleTopology() throws CacheException, DeployerException {
        this.generateDeployment(new String[] { "id" }, new int[] { 3 }, this
                .createTopology(this.numDcs,
                                this.numRacks,
                                this.numCpus,
                                this.numNetworkDisks,
                                this.numLocalDisks,
                                this.prefix));
        Assert.assertEquals(this.getSpecs().get(0).getHost(), this.getSpecs().get(1).getHost());
        Assert.assertNotEquals(this.getSpecs().get(2).getHost(), this.getSpecs().get(1).getHost());
    }
}
