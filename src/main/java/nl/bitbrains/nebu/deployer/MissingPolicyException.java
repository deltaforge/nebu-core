package nl.bitbrains.nebu.deployer;

/**
 * Thrown if a policy cannot be found.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class MissingPolicyException extends DeployerException {

    private static final long serialVersionUID = -3074126495508937041L;

    /**
     * @param ex
     *            the cause of this MissingPolicyException.
     */
    public MissingPolicyException(final Throwable ex) {
        super(ex);
    }

    /**
     * @param message
     *            a detailed description of this MissingPolicyException.
     */
    public MissingPolicyException(final String message) {
        super(message);
    }

    /**
     * @param message
     *            a detailed description of this MissingPolicyException.
     * @param ex
     *            the cause of this MissingPolicyException.
     */
    public MissingPolicyException(final String message, final Throwable ex) {
        super(message, ex);
    }

}
