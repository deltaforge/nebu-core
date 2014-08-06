package nl.bitbrains.nebu.deployer;

import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.containers.Deployment;

/**
 * Thrown when the {@link Deployer} encounters an issue while calculating a
 * suggested {@link Deployment}, e.g. when vital data about the
 * {@link PhysicalTopology} is missing.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeployerException extends Exception {

    private static final long serialVersionUID = 6349374783311393580L;

    /**
     * @param ex
     *            the cause of this DeployerException.
     */
    public DeployerException(final Throwable ex) {
        super(ex);
    }

    /**
     * @param message
     *            a detailed description of this DeployerException.
     */
    public DeployerException(final String message) {
        super(message);
    }

    /**
     * @param message
     *            a detailed description of this DeployerException.
     * @param ex
     *            the cause of this DeployerException.
     */
    public DeployerException(final String message, final Throwable ex) {
        super(message, ex);
    }

}
