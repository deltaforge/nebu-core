package nl.bitbrains.nebu.deployer;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.common.util.UUIDGenerator;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentBuilder;
import nl.bitbrains.nebu.containers.DeploymentRequest;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * Responsible for generating {@link Deployment}s for a given
 * {@link DeploymentRequest}. The Deployer supports having a different
 * {@link DeployerPolicy} for each {@link Application}. These policies are used
 * in the decision making process when generating a deployment suggestion.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class Deployer {

    public static final String CACHE_KEY = "deployer";
    private static Logger logger = LogManager.getLogger();

    private DeployerPolicyFactories factories;
    private final Map<Application, DeployerPolicy> policies;

    /**
     * Creates a new Deployer.
     */
    public Deployer() {
        Deployer.logger.entry();
        this.policies = new HashMap<Application, DeployerPolicy>();
        this.factories = null;
        Deployer.logger.exit();
    }

    /**
     * Adds policy factories.
     * 
     * @param factories
     *            to set.
     * @return this for fluency.
     */
    public final Deployer withPolicyFactories(final DeployerPolicyFactories factories) {
        this.factories = factories;
        return this;
    }

    /**
     * @param application
     *            the {@link Application} to retrieve a {@link DeployerPolicy}
     *            for.
     * @return the associated {@link DeployerPolicy}.
     */
    public final DeployerPolicy getPolicy(final Application application) {
        return this.policies.get(application);
    }

    /**
     * @param application
     *            the {@link Application} to set a new {@link DeployerPolicy}
     *            for.
     * @param policy
     *            the {@link DeployerPolicy} to be used.
     */
    public final void setPolicy(final Application application, final DeployerPolicy policy) {
        ErrorChecker.throwIfNullArgument(application, "application");
        ErrorChecker.throwIfNullArgument(policy, "policy");

        this.policies.put(application, policy);
    }

    /**
     * @param application
     *            the {@link Application} to set a new {@link DeployerPolicy}
     *            for.
     * @param policyName
     *            the name of the {@link DeployerPolicy} to be used.
     * @throws MissingPolicyException
     *             when no {@link DeployerPolicyFactory} for the requested
     *             policy name could be found.
     */
    public final void setPolicy(final Application application, final String policyName)
            throws MissingPolicyException {
        if (this.factories != null && this.factories.hasFactory(policyName)) {
            final DeployerPolicyFactory factory = this.factories.getFactory(policyName);
            this.setPolicy(application, factory.newInstance());
        } else {
            throw new MissingPolicyException("Could not retrieve a factory for policy type '"
                    + policyName + "'");
        }
    }

    /**
     * @param application
     *            the {@link Application} to set a new {@link DeployerPolicy}
     *            for.
     * @param policyName
     *            the name of the {@link DeployerPolicy} to be used.
     * @param configuration
     *            additional options for the {@link DeployerPolicy} in the form
     *            of XML.
     * @throws MissingPolicyException
     *             when no {@link DeployerPolicyFactory} for the requested
     *             policy name could be found.
     * @throws ParseException
     *             when the XML is malformed.
     */
    public final void setPolicy(final Application application, final String policyName,
            final Element configuration) throws MissingPolicyException, ParseException {
        if (this.factories != null && this.factories.hasFactory(policyName)) {
            final DeployerPolicyFactory factory = this.factories.getFactory(policyName);
            this.setPolicy(application, factory.fromXML(configuration));
        } else {
            throw new MissingPolicyException("Could not retrieve a factory for policy type '"
                    + policyName + "'");
        }
    }

    /**
     * Generates a new {@link Deployment} for the given
     * {@link DeploymentRequest}. The Deployer uses the {@link DeployerPolicy}
     * that is specified for the {@link Application} to which the request
     * belongs.
     * 
     * @param request
     *            the {@link DeploymentRequest} to generate from.
     * @return the generated {@link Deployment}.
     * @throws DeployerException
     *             when the {@link DeployerPolicy} is missing information or
     *             cannot compute a valid {@link Deployment}.
     */
    public final Deployment generateDeployment(final DeploymentRequest request)
            throws DeployerException {
        Deployer.logger.entry();
        final Application application = request.getApplication();
        if (this.policies.containsKey(application)) {
            final String uuid = UUIDGenerator.generate(Deployment.UUID_PREFIX);
            final DeploymentBuilder builder = new DeploymentBuilder().withUuid(uuid);

            final List<VMDeploymentSpecification> specs = this.policies.get(application)
                    .generateDeployment(request);

            builder.withSpecs(specs);

            return Deployer.logger.exit(builder.build());
        } else {
            throw Deployer.logger.exit(new MissingPolicyException(
                    "Cannot find policy for application '" + application.getUniqueIdentifier()
                            + "'"));
        }
    }
}
