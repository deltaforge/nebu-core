package nl.bitbrains.nebu.containers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class that can be used by the deployer for specifying how a template should
 * be launched.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 */
public class VMDeploymentSpecification {
    private static Logger logger = LogManager.getLogger();

    private final VMTemplate template;
    private final String host;
    private final String store;

    /**
     * @param template
     *            to launch.
     * @param host
     *            to launch the VM on.
     * @param store
     *            to launch the VM on.
     */
    protected VMDeploymentSpecification(final VMTemplate template, final String host,
            final String store) {
        VMDeploymentSpecification.logger.entry();
        this.template = template;
        this.host = host;
        this.store = store;
        VMDeploymentSpecification.logger.exit();
    }

    /**
     * @return the template
     */
    public final VMTemplate getTemplate() {
        return this.template;
    }

    /**
     * @return the host
     */
    public final String getHost() {
        return this.host;
    }

    /**
     * @return the store
     */
    public final String getStore() {
        return this.store;
    }

}
