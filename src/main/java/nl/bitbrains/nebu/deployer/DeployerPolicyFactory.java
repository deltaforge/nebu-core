package nl.bitbrains.nebu.deployer;

import nl.bitbrains.nebu.common.util.xml.XMLReader;

/**
 * Mandates the ability the create a {@link DeployerPolicy} with default
 * settings. Concrete implementations of the DeployerPolicyFactory should also
 * be able to parse an XML representation of the {@link DeployerPolicy} for
 * user-specified parameters.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public interface DeployerPolicyFactory extends XMLReader<DeployerPolicy> {

    /**
     * @return a new instance of a concrete {@link DeployerPolicy} with default
     *         configuration.
     */
    DeployerPolicy newInstance();

}
