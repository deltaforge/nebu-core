package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * Policy that tries to group vms as close as possible.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class AggregatedLocalityPolicy extends DeployerPolicy {

    public static final String POLICY_NAME = "agglocality";
    private static Logger logger = LogManager.getLogger();

    public static final int DEFAULT_MAX_VMS_PER_HOST = 4;
    private int maxVMsPerHost;

    /**
     * Simple default empty constructor.
     */
    public AggregatedLocalityPolicy() {
        this.maxVMsPerHost = AggregatedLocalityPolicy.DEFAULT_MAX_VMS_PER_HOST;
    }

    /**
     * @param maxVmsPerHost
     *            to set.
     */
    public final void setMaxVMsPerHost(final int maxVmsPerHost) {
        this.maxVMsPerHost = maxVmsPerHost;
    }

    /**
     * Simple getter.
     * 
     * @return the maxVMsPerHost
     */
    public final int getMaxVMsPerHost() {
        return this.maxVMsPerHost;
    }

    @Override
    public final List<VMDeploymentSpecification> generateDeployment(final DeploymentRequest request)
            throws DeployerException {
        AggregatedLocalityPolicy.logger.entry();
        ErrorChecker.throwIfNullArgument(request, "request");
        final Map<VMTemplate, Integer> requestedTemplates = request.getTemplateRequests();
        final List<VMDeploymentSpecification> result = new ArrayList<VMDeploymentSpecification>();
        if (requestedTemplates.isEmpty()) {
            return result;
        }
        final Map<VMTemplate, PhysicalTopology> topos = new HashMap<VMTemplate, PhysicalTopology>();
        PhysicalTopology superTopo = new PhysicalTopology();
        for (final VMTemplate template : requestedTemplates.keySet()) {
            final PhysicalTopology topology = super.retrieveTopology(template);
            topos.put(template, topology);
            superTopo = PhysicalTopology.mergeTree(topology, superTopo);
        }

        this.initializeVmCounters(superTopo);
        this.updateVMCountersForApplicationAndTemplate(request.getApplication(), null, superTopo);

        // Algorithm idea: Place maxMVsPerHost on the same host, before moving
        // to the next. Keeping in mind the vms already in place. You put as
        // many of them of possible in the same rack
        // too, before moving to the next rack.

        for (final Entry<VMTemplate, Integer> entry : requestedTemplates.entrySet()) {
            final VMTemplate template = entry.getKey();
            final PhysicalTopology topology = topos.get(template);
            int numberOfTemplate = entry.getValue();
            final List<PhysicalRack> racks = topology.getRacks();
            Collections.shuffle(racks);
            Collections.sort(racks, new PhysicalRackComparator(this.getVmsPerRack()));
            for (final PhysicalRack r : racks) {
                final List<PhysicalHost> hosts = r.getCPUs();
                Collections.shuffle(hosts);
                Collections.sort(hosts, new PhysicalHostComparator(this.getVmsPerHost()));
                for (final PhysicalHost h : hosts) {
                    while (this.getVmsPerHost().get(h.getUniqueIdentifier()) < this.maxVMsPerHost
                            && numberOfTemplate > 0) {
                        final PhysicalStore store = this.getSuitableStoreFromLeastUsedOrFull(r
                                .getDisks());
                        final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                                .withHost(h.getUniqueIdentifier())
                                .withStore(store.getUniqueIdentifier()).withTemplate(template)
                                .build();
                        result.add(spec);
                        this.updateVMCountersWithNewlyChosenHostAndStore(h, store);
                        numberOfTemplate--;
                    }
                    if (numberOfTemplate == 0) {
                        break;
                    }
                }
                if (numberOfTemplate == 0) {
                    break;
                }
            }
            if (numberOfTemplate > 0) {
                throw new DeployerException("Can not fit policy on the given hosts");
            }

        }

        return AggregatedLocalityPolicy.logger.exit(result);
    }

    /**
     * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
     * 
     */
    private static class PhysicalRackComparator implements Comparator<PhysicalRack> {

        private final Map<PhysicalRack, Integer> map;

        /**
         * @param inputMap
         *            to set.
         */
        public PhysicalRackComparator(final Map<PhysicalRack, Integer> inputMap) {
            this.map = inputMap;
        }

        @Override
        public int compare(final PhysicalRack o1, final PhysicalRack o2) {
            return this.map.get(o2) - this.map.get(o1);
        }

    }

    /**
     * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
     * 
     */
    public static class PhysicalHostComparator implements Comparator<PhysicalHost> {

        private final Map<String, Integer> map;

        /**
         * @param vmsPerHost
         *            to set.
         */
        public PhysicalHostComparator(final Map<String, Integer> vmsPerHost) {
            this.map = vmsPerHost;
        }

        @Override
        public int compare(final PhysicalHost o1, final PhysicalHost o2) {
            return this.map.get(o2.getUniqueIdentifier()) - this.map.get(o1.getUniqueIdentifier());
        }

    }
}
