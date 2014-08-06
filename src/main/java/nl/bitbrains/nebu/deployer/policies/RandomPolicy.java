package nl.bitbrains.nebu.deployer.policies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nl.bitbrains.nebu.common.topology.PhysicalHost;
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
 * {@link DeployerPolicy} with fully randomized placement of virtual machines
 * over different hosts.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class RandomPolicy extends DeployerPolicy {

    public static final String POLICY_NAME = "random";
    private static Logger logger = LogManager.getLogger();

    private final Random rand;
    private long seed;

    /**
     * Creates a new instance of RandomPolicy. Creates a {@link Random} to
     * provide random number generation throughout the lifetime of the object.
     */
    public RandomPolicy() {
        this.rand = new Random();
        this.seed = 0;
    }

    /**
     * Creates a new instance of RandomPolicy. Creates a {@link Random} with the
     * seed.
     * 
     * @param seed
     *            to use.
     */
    public RandomPolicy(final long seed) {
        this.seed = seed;
        this.rand = new Random(seed);
    }

    /**
     * @param seed
     *            to set.
     */
    public final void setSeed(final long seed) {
        this.seed = seed;
        this.rand.setSeed(seed);
    }

    /**
     * @return the seed.
     */
    public final long getSeed() {
        return this.seed;
    }

    @Override
    public final List<VMDeploymentSpecification> generateDeployment(final DeploymentRequest request)
            throws DeployerException {
        RandomPolicy.logger.entry();
        ErrorChecker.throwIfNullArgument(request, "request");
        final List<VMDeploymentSpecification> result = new ArrayList<VMDeploymentSpecification>();

        for (final Map.Entry<VMTemplate, Integer> templateRequest : request.getTemplateRequests()
                .entrySet()) {
            final VMTemplate template = templateRequest.getKey();
            final int vmsNeeded = templateRequest.getValue();
            final List<VMDeploymentSpecification> allocation = this
                    .generateDeploymentForTemplate(template, vmsNeeded);
            result.addAll(allocation);
        }

        return RandomPolicy.logger.exit(result);
    }

    /**
     * @param template
     *            the {@link VMTemplate} to allocate.
     * @param vmsNeeded
     *            the amount of VMs that need to be launched for the given
     *            {@link VMTemplate}.
     * @return an allocation in the form of a list of {@link PhysicalCPU}s.
     * @throws DeployerException
     *             when the {@link PhysicalTopology} for the given
     *             {@link VMTemplate} can not be retrieved.
     */
    private List<VMDeploymentSpecification> generateDeploymentForTemplate(
            final VMTemplate template, final int vmsNeeded) throws DeployerException {
        final List<VMDeploymentSpecification> result = new ArrayList<VMDeploymentSpecification>();
        final PhysicalTopology topology = super.retrieveTopology(template);
        final List<PhysicalHost> hostsAvailable = topology.getCPUs();

        for (int vmNo = 0; vmNo < vmsNeeded; vmNo++) {
            final int selectedHost = this.rand.nextInt(hostsAvailable.size());
            final VMDeploymentSpecification spec = new VMDeploymentSpecificationBuilder()
                    .withHost(hostsAvailable.get(selectedHost).getUniqueIdentifier())
                    .withTemplate(template).build();
            result.add(spec);
        }

        return result;
    }

}
