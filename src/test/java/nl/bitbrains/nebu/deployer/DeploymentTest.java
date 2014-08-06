package nl.bitbrains.nebu.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.containers.Deployment;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the {@link Deployment} class.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeploymentTest {

    @Mock
    private PhysicalTopology topology;

    private List<PhysicalHost> list;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.list = new ArrayList<PhysicalHost>();
    }

    private void setTopologyMockSuccess() {
        Mockito.when(this.topology.getCPUs()).thenReturn(this.list);
    }

    private int getTotalVMs(final Map<PhysicalHost, Integer> actual) {
        int sum = 0;
        for (final Integer it : actual.values()) {
            sum += it.intValue();
        }
        return sum;
    }

    // @Test
    // public void testSingleMachineSingleCPU() {
    // final PhysicalCPU cpu = new PhysicalCPU("cpu1");
    // this.list.add(cpu);
    // this.setTopologyMockSuccess();
    // final Map<PhysicalCPU, Integer> expected = new HashMap<PhysicalCPU,
    // Integer>();
    // expected.put(cpu, 1);
    //
    // final Map<PhysicalCPU, Integer> actual = Deployment
    // .suggestDeployment(this.topology, 1);
    // Assert.assertEquals(expected, actual);
    // }
    //
    // @Test
    // public void testMultipleMachineSingleCPU() {
    // final PhysicalCPU cpu = new PhysicalCPU("cpu1");
    // this.list.add(cpu);
    // this.setTopologyMockSuccess();
    // final Map<PhysicalCPU, Integer> expected = new HashMap<PhysicalCPU,
    // Integer>();
    // expected.put(cpu, 5);
    //
    // final Map<PhysicalCPU, Integer> actual = Deployment
    // .suggestDeployment(this.topology, 5);
    // Assert.assertEquals(expected, actual);
    // }
    //
    // @Test
    // public void testSingleMachineMultipleCPU() {
    // final PhysicalCPU cpu = new PhysicalCPU("cpu1");
    // final PhysicalCPU cpu2 = new PhysicalCPU("cpu2");
    // final int expected = 1;
    // this.list.add(cpu);
    // this.list.add(cpu2);
    // this.setTopologyMockSuccess();
    //
    // final Map<PhysicalCPU, Integer> actual = Deployment
    // .suggestDeployment(this.topology, expected);
    // final int actualInt = this.getTotalVMs(actual);
    // Assert.assertEquals(expected, actualInt);
    // }
    //
    // @Test
    // public void testMultipleMachineMultipleCPU() {
    // final PhysicalCPU cpu = new PhysicalCPU("cpu1");
    // final PhysicalCPU cpu2 = new PhysicalCPU("cpu2");
    // final PhysicalCPU cpu3 = new PhysicalCPU("cpu3");
    // final PhysicalCPU cpu4 = new PhysicalCPU("cpu4");
    // final int expected = 4;
    // this.list.add(cpu);
    // this.list.add(cpu2);
    // this.list.add(cpu3);
    // this.list.add(cpu4);
    // this.setTopologyMockSuccess();
    //
    // final Map<PhysicalCPU, Integer> actual = Deployment
    // .suggestDeployment(this.topology, expected);
    // final int actualInt = this.getTotalVMs(actual);
    // Assert.assertEquals(expected, actualInt);
    // }

}
