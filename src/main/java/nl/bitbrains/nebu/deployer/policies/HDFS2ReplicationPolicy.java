package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.Collections;
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
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class HDFS2ReplicationPolicy extends DeployerPolicy {

    /**
     * The logger for this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * The name of this policy.
     */
    public static final String POLICY_NAME = "hdfs2";

    /**
     * Simple default (empty) constructor.
     */
    public HDFS2ReplicationPolicy() {
    }

    @Override
    public final List<VMDeploymentSpecification> generateDeployment(final DeploymentRequest request)
            throws DeployerException {
        HDFS2ReplicationPolicy.logger.entry();
        // Default start
        ErrorChecker.throwIfNullArgument(request, "request");
        final Map<VMTemplate, Integer> requestedTemplates = request.getTemplateRequests();
        final List<VMDeploymentSpecification> result = new ArrayList<VMDeploymentSpecification>();
        // Algorithm attempts to distribute the VMs in a 2:1 fashion.
        // 2 are put on the 'fatCluster', the cluster which already has the most
        // VMs.
        // 1 is put on any random other cluster which is not equal to the
        // 'fatCluster'.

        // FOR every template t:
        //
        // fatCluster <- getMostBusyCluster();
        // numvms <- getTotalNumVMs();
        // numFatClusterVMs <- fatCluster.getNumVMs();
        //
        // FOR every vm in t:
        // IF numFatClusterVMs + 1 > (2/3) * numvms:
        // numFatClusterVMs++
        // place vm on other cluster
        // ELSE:
        // place vm on fat cluster
        // ENDIF
        // numvms++
        // ENDFOR
        // ENDFOR

        for (final Entry<VMTemplate, Integer> entry : requestedTemplates.entrySet()) {
            final VMTemplate template = entry.getKey();
            int numberOfTemplate = entry.getValue();
            final PhysicalTopology topology = super.retrieveTopology(template);
            this.initializeVmCounters(topology);
            this.updateVMCountersForApplicationAndTemplate(request.getApplication(),
                                                           template,
                                                           topology);

            final PhysicalRack fatCluster = this.getFatCluster();
            int numFatClusterVMs = this.getNumberOfVMs(fatCluster);
            int numvms = this.getTotalNumVMs();

            while (numberOfTemplate-- > 0) {
                PhysicalRack rack = null;
                if (3 * (numFatClusterVMs + 1) <= 2 * ++numvms) {
                    rack = fatCluster;
                    numFatClusterVMs++;
                } else {
                    rack = this.pickDifferentCluster(topology, fatCluster);
                }
                this.placeOnRack(template, rack, result);
            }
        }

        return HDFS2ReplicationPolicy.logger.exit(result);
    }

    /**
     * Assigns a VM to the given rack.
     * 
     * @param template
     *            The {@link VMTemplate} of the VM.
     * @param rack
     *            The {@link PhysicalRack} the VM gets assigned to.
     * @param result
     *            The {@link List} to which to add the new
     *            {@link VMDeploymentSpecification}.
     * @throws DeployerException
     *             If no suitable {@link PhysicalStore} can be found for
     *             deployment.
     */
    private void placeOnRack(final VMTemplate template, final PhysicalRack rack,
            final List<VMDeploymentSpecification> result) throws DeployerException {
        final PhysicalHost host = this.getLeastStressedHostFromLeastUsed(rack.getCPUs(), template);
        final PhysicalStore store = this.getSuitableStoreFromLeastUsed(rack.getDisks());
        final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                .withHost(host.getUniqueIdentifier()).withStore(store.getUniqueIdentifier())
                .withTemplate(template).build();
        this.updateVMCountersWithNewlyChosenHostAndStore(host, store);
        result.add(spec);
    }

    /**
     * Selects a {@link PhysicalRack} which is different from the given one.
     * 
     * @param topology
     *            The {@link PhysicalTopology} that contains all racks.
     * @param fatCluster
     *            The {@link PhysicalRack} to avoid.
     * @return A {@link PhysicalRack} from the {@link PhysicalTopology} unequal
     *         to the given {@link PhysicalRack}.
     * @throws DeployerException
     *             If no other {@link PhysicalRack} can be found.
     */
    private PhysicalRack pickDifferentCluster(final PhysicalTopology topology,
            final PhysicalRack fatCluster) throws DeployerException {
        final PhysicalRack otherRack = this.pick(topology.getRacks(), fatCluster);
        if (fatCluster.equals(otherRack)) {
            throw new DeployerException("No other rack to place VMs. This policy is useless.");
        }
        return otherRack;
    }

    /**
     * Calculates the total number of VMs.
     * 
     * @return The number of VMs.
     */
    private int getTotalNumVMs() {
        final Map<PhysicalRack, Integer> vmsPerRack = this.getVmsPerRack();
        int total = 0;
        if (vmsPerRack != null) {
            for (final Integer i : vmsPerRack.values()) {
                total += i;
            }
        }
        return total;
    }

    /**
     * Get the cluster that has the most number of VMs.
     * 
     * @return The {@link PhysicalRack} containing the most VMs in the cloud.
     * @throws DeployerException
     */
    private PhysicalRack getFatCluster() throws DeployerException {
        final Map<PhysicalRack, Integer> vmsPerRack = this.getVmsPerRack();
        int maxVms = -1;
        PhysicalRack rack = null;
        if (vmsPerRack != null) {
            for (final Entry<PhysicalRack, Integer> e : vmsPerRack.entrySet()) {
                if (e.getValue() > maxVms) {
                    maxVms = e.getValue();
                    rack = e.getKey();
                }
            }
        }
        return rack;
    }

    /**
     * Retrieves the number of VMs on the given {@link PhysicalRack}.
     * 
     * @param fatCluster
     *            The {@link PhysicalRack} whose number of VMs to calculate.
     * @return The number of VMs on the given {@link PhysicalRack}.
     */
    private int getNumberOfVMs(final PhysicalRack fatCluster) {
        final Map<PhysicalRack, Integer> vmsPerRack = super.getVmsPerRack();
        if (vmsPerRack != null) {
            return vmsPerRack.get(fatCluster);
        }
        return 0;
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
