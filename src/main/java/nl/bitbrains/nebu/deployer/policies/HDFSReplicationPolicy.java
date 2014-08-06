package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bitbrains.nebu.common.topology.PhysicalDataCenter;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalRack;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.containers.DeploymentRequest;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;
import nl.bitbrains.nebu.containers.VMDeploymentSpecificationBuilder;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.deployer.DeployerException;
import nl.bitbrains.nebu.deployer.DeployerPolicy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class HDFSReplicationPolicy extends DeployerPolicy {

    public static final String POLICY_NAME = "hdfs";
    private static Logger logger = LogManager.getLogger();

    private static final int DEFAULT_GROUPING_SIZE = 3;
    private final int groupingSize;

    /**
     * Simple default (empty) constructor.
     */
    public HDFSReplicationPolicy() {
        this.groupingSize = HDFSReplicationPolicy.DEFAULT_GROUPING_SIZE;
    }

    @Override
    public final List<VMDeploymentSpecification> generateDeployment(final DeploymentRequest request)
            throws DeployerException {
        HDFSReplicationPolicy.logger.entry();
        // Default start
        ErrorChecker.throwIfNullArgument(request, "request");
        final Map<VMTemplate, Integer> requestedTemplates = request.getTemplateRequests();
        final List<VMDeploymentSpecification> result = new ArrayList<VMDeploymentSpecification>();
        // Algorithm attempts to create groups of three, with two machines on
        // the same physicalhost and one on a seperate one.

        // FOR every template t:
        // Pick a random host h in cluster c, datacenter d
        // Place 2 vms on h.
        // Pick a random host h2 in clutser c2, datacenter d2, such that d != d2
        // (or if impossible, fall back to c != c2)
        // Place 1 vm on h2.
        // END FOR.

        for (final Entry<VMTemplate, Integer> entry : requestedTemplates.entrySet()) {
            final VMTemplate template = entry.getKey();
            int numberOfTemplate = entry.getValue();
            final PhysicalTopology topology = super.retrieveTopology(template);
            final List<PhysicalHost> allHosts = topology.getCPUs();
            this.initializeVmCounters(topology);
            this.updateVMCountersForApplicationAndTemplate(request.getApplication(),
                                                           template,
                                                           topology);

            while (numberOfTemplate >= this.groupingSize) {
                final PhysicalHost host1 = this.getLeastStressedHostFromLeastUsed(allHosts,
                                                                                  template);
                final PhysicalHost host2 = this.pickDifferentHost(host1, topology);
                final List<PhysicalStore> stores = host1.getParent().getDisks();
                for (int i = 1; i < this.groupingSize; i++) {
                    final PhysicalStore store = this.getSuitableStoreFromLeastUsedOrFull(stores);
                    final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                            .withHost(host1.getUniqueIdentifier()).withTemplate(template)
                            .withStore(store.getUniqueIdentifier()).build();
                    result.add(spec);
                }
                final List<PhysicalStore> stores2 = host2.getParent().getDisks();
                Collections.shuffle(stores2);
                final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                        .withHost(host2.getUniqueIdentifier()).withTemplate(template)
                        .withStore(stores2.get(0).getUniqueIdentifier()).build();
                result.add(spec);
                numberOfTemplate -= this.groupingSize;
            }

            if (numberOfTemplate > 0) {
                final PhysicalHost host = this
                        .getLeastStressedHostFromLeastUsed(allHosts, template);
                final List<PhysicalStore> stores = host.getParent().getDisks();
                Collections.shuffle(stores);
                for (int i = 0; i < numberOfTemplate; i++) {
                    final PhysicalStore store = this.getSuitableStoreFromLeastUsedOrFull(stores);
                    final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                            .withHost(host.getUniqueIdentifier()).withTemplate(template)
                            .withStore(store.getUniqueIdentifier()).build();
                    result.add(spec);
                }
            }
        }

        return HDFSReplicationPolicy.logger.exit(result);
    }

    /**
     * Finds a host so that the datacenter is different, or if this is
     * impossible the cluster is different than the given host.
     * 
     * @param host1
     *            host to avoid.
     * @param topology
     *            to search in.
     * @return the found host
     */
    private PhysicalHost pickDifferentHost(final PhysicalHost host1, final PhysicalTopology topology) {
        final PhysicalRack rack1 = host1.getParent();
        final PhysicalDataCenter dc1 = rack1.getParent();

        final List<PhysicalDataCenter> dcs = topology.getDataCenters();
        final PhysicalDataCenter dc2 = this.pick(dcs, dc1);
        final List<PhysicalRack> racks = dc2.getRacks();
        final PhysicalRack r2 = this.pick(racks, rack1);
        final List<PhysicalHost> hosts = r2.getCPUs();
        return this.pick(hosts, host1);
    }

    /**
     * Picks a type avoiding the specified object is possible.
     * 
     * @param <T>
     *            type to avoid.
     * @param list
     *            to pick from.
     * @param toAvoid
     *            object to avoid.
     * @return a different object if possible.
     */
    private <T> T pick(final List<T> list, final T toAvoid) {
        Collections.shuffle(list);
        T dc2 = toAvoid;
        for (final T d : list) {
            if (!d.equals(toAvoid)) {
                dc2 = d;
                break;
            }
        }
        return dc2;
    }
}
