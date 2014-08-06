package nl.bitbrains.nebu.deployer;

import java.util.HashMap;
import java.util.Map;

import nl.bitbrains.nebu.common.util.ErrorChecker;

/**
 * Manages the set of {@link DeployerPolicyFactory} classes available in the
 * system by mapping policy names to the corresponding factory.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeployerPolicyFactories {

    private final Map<String, DeployerPolicyFactory> factories;

    /**
     * Creates a new manager of {@link DeployerPolicyFactory} objects.
     */
    public DeployerPolicyFactories() {
        this.factories = new HashMap<String, DeployerPolicyFactory>();
    }

    /**
     * @param policyName
     *            the {@link DeployerPolicy} to find a factory for.
     * @return the corresponding {@link DeployerPolicyFactory}, or null if it
     *         does not exist.
     */
    public DeployerPolicyFactory getFactory(final String policyName) {
        return this.factories.get(policyName);
    }

    /**
     * @param policyName
     *            the name of the {@link DeployerPolicy} to search a factory
     *            for.
     * @return true iff this DeployerPolicyFactories has the corresponding
     *         {@link DeployerPolicyFactory}.
     */
    public boolean hasFactory(final String policyName) {
        return this.factories.containsKey(policyName);
    }

    /**
     * @param policyName
     *            the name of the {@link DeployerPolicy} to register a factory
     *            for.
     * @param policyFactory
     *            a {@link DeployerPolicyFactory} object that can create
     *            {@link DeployerPolicy} object of the given name.
     * @return this.
     */
    public DeployerPolicyFactories withFactory(final String policyName,
            final DeployerPolicyFactory policyFactory) {
        ErrorChecker.throwIfNullArgument(policyName, "policyName");
        ErrorChecker.throwIfNullArgument(policyFactory, "policyFactory");

        this.factories.put(policyName, policyFactory);

        return this;
    }

}
