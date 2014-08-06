package nl.bitbrains.nebu.deployer.policies;

import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.deployer.policies.HDFS2ReplicationPolicy;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Very basic test for the {@link HDFS2ReplicationPolicy}.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RequestSender.class, Application.class })
@PowerMockIgnore("javax.management.*")
public class TestHDFS2ReplicationPolicy extends PolicyTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.setPolicy(new HDFS2ReplicationPolicy());
        this.setRequires2Racks(true);
    }

}
