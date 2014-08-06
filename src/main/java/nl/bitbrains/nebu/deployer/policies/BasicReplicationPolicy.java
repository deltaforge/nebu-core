package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.List;

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
 * Class representing the basic replication policy (spreads vms on rack-level).
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */

public class BasicReplicationPolicy extends DeployerPolicy {
    public static final String POLICY_NAME = "replicated";
    private static Logger logger = LogManager.getLogger();

    /**
     * Basic default constructor.
     */
    public BasicReplicationPolicy() {

    }

    @Override
    public final List<VMDeploymentSpecification> generateDeployment(final DeploymentRequest request)
            throws DeployerException {
        BasicReplicationPolicy.logger.entry();
        ErrorChecker.throwIfNullArgument(request, "request");
        // Basic algorithm: (rack-level equal spread)
        // Input: ts = templates, n(t) = vms/template
        // for t in ts
        // for i = 1..n(t)
        // r = min_{x in racks} vms(x)
        // h = min_{x in hosts(r)} vms(x)
        // place vm on h
        // end for
        // end for

        final List<VMDeploymentSpecification> result = new ArrayList<VMDeploymentSpecification>();
        for (final VMTemplate template : request.getTemplateRequests().keySet()) {
            final int nt = request.getTemplateRequests().get(template);

            final PhysicalTopology topology = super.retrieveTopology(template);
            this.initializeVmCounters(topology);
            this.updateVMCountersForApplicationAndTemplate(request.getApplication(),
                                                           template,
                                                           topology);

            for (int i = 0; i < nt; i++) {
                final PhysicalRack r = this.getLeastUsedRack();
                final PhysicalHost h = this
                        .getLeastStressedHostFromLeastUsed(r.getCPUs(), template);
                final PhysicalStore s = this.getSuitableStoreFromLeastUsedOrFull(r.getDisks());

                final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                        .withTemplate(template).withHost(h.getUniqueIdentifier())
                        .withStore(s.getUniqueIdentifier()).build();
                result.add(spec);

                this.updateVMCountersWithNewlyChosenHostAndStore(h, s);
            }
        }

        return BasicReplicationPolicy.logger.exit(result);
    }

}
