package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bitbrains.nebu.common.topology.PhysicalDataCenter;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalRack;
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
public class LocalityPolicy extends DeployerPolicy {

    public static final String POLICY_NAME = "locality";
    private static Logger logger = LogManager.getLogger();

    public static final int DEFAULT_MAX_VMS_PER_HOST = 4;
    private int maxVMsPerHost;

    /**
     * Simple default empty constructor.
     */
    public LocalityPolicy() {
        this.maxVMsPerHost = LocalityPolicy.DEFAULT_MAX_VMS_PER_HOST;
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
        LocalityPolicy.logger.entry();
        ErrorChecker.throwIfNullArgument(request, "request");
        final Map<VMTemplate, Integer> requestedTemplates = request.getTemplateRequests();
        final Map<VMTemplate, PhysicalTopology> topos = new HashMap<VMTemplate, PhysicalTopology>();
        final List<VMDeploymentSpecification> result = new ArrayList<VMDeploymentSpecification>();
        for (final VMTemplate template : requestedTemplates.keySet()) {
            topos.put(template, DeployerPolicy.retrieveTopology(template));
        }

        // Algorithm idea: Place maxMVsPerHost on the same host, before moving
        // to the next. You put as many of them of possible in the same rack
        // too, before moving to the next rack.

        for (final Entry<VMTemplate, Integer> entry : requestedTemplates.entrySet()) {
            final VMTemplate template = entry.getKey();
            int numberOfTemplate = entry.getValue();
            final PhysicalTopology topology = topos.get(template);
            final Map<String, Integer> vmsPerHost = this.newVmsPerHost(topology.getCPUs());
            this.updateVmsPerHostForExistingVMs(vmsPerHost, request.getApplication(), template);

            final List<PhysicalDataCenter> dcs = topology.getDataCenters();
            Collections.shuffle(dcs);
            for (int i = 0; i < dcs.size() && numberOfTemplate > 0; i++) {
                final List<PhysicalRack> racks = dcs.get(i).getRacks();
                Collections.shuffle(racks);
                for (int j = 0; j < racks.size() && numberOfTemplate > 0; j++) {
                    final List<PhysicalHost> hosts = racks.get(j).getCPUs();
                    Collections.shuffle(hosts);
                    for (int k = 0; k < hosts.size() && numberOfTemplate > 0; k++) {
                        final String hostId = hosts.get(k).getUniqueIdentifier();
                        while (vmsPerHost.get(hostId) < this.maxVMsPerHost && numberOfTemplate > 0) {
                            final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                                    .withHost(hosts.get(k).getUniqueIdentifier())
                                    .withTemplate(template).build();
                            result.add(spec);
                            vmsPerHost.put(hostId, vmsPerHost.get(hostId) + 1);
                            numberOfTemplate--;
                        }
                    }
                }
            }
            // Could not fit it all in.
            if (numberOfTemplate > 0) {
                throw LocalityPolicy.logger.throwing(new DeployerException("Does not fit"));
            }
        }

        return LocalityPolicy.logger.exit(result);
    }
}
