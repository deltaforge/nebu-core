package nl.bitbrains.nebu.deployer.policies;

import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.deployer.policies.RandomPolicy;
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
public class TestRandomPolicy extends PolicyTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.setPolicy(new RandomPolicy());
    }

    @Test
    public void testConstructor() {
        final long seed = 802;
        final RandomPolicy policy = new RandomPolicy(seed);
        Assert.assertEquals(seed, policy.getSeed());
    }

    @Test
    public void testGetSet() {
        final long seed = 802;
        final RandomPolicy policy = new RandomPolicy();
        policy.setSeed(seed);
        Assert.assertEquals(seed, policy.getSeed());
    }

}
