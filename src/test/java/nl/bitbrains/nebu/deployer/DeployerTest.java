package nl.bitbrains.nebu.deployer;

import nl.bitbrains.nebu.deployer.Deployer;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeployerTest {

    @Test
    public void simpleConstructorTest() {
        Assert.assertNotNull(new Deployer());
    }
}
