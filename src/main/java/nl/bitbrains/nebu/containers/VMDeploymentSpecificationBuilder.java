package nl.bitbrains.nebu.containers;

import nl.bitbrains.nebu.common.interfaces.IBuilder;
import nl.bitbrains.nebu.common.util.ErrorChecker;

/**
 * Builder class for {@link VMDeploymentSpecification}.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 */
public class VMDeploymentSpecificationBuilder implements IBuilder<VMDeploymentSpecification> {
    private VMTemplate template;
    private String host;
    private String store;

    /**
     * Simple default constructor.
     */
    public VMDeploymentSpecificationBuilder() {
        this.reset();
    }

    @Override
    public final void reset() {
        this.template = null;
        this.host = null;
        this.store = null;
    }

    /**
     * @param template
     *            to build with.
     * @return this for fluency.
     */
    public final VMDeploymentSpecificationBuilder withTemplate(final VMTemplate template) {
        this.template = template;
        return this;
    }

    /**
     * @param host
     *            to build with.
     * @return this for fluency.
     */
    public final VMDeploymentSpecificationBuilder withHost(final String host) {
        this.host = host;
        return this;
    }

    /**
     * @param store
     *            to build with.
     * @return this for fluency.
     */
    public final VMDeploymentSpecificationBuilder withStore(final String store) {
        this.store = store;
        return this;
    }

    @Override
    public final VMDeploymentSpecification build() {
        ErrorChecker.throwIfNotSet(this.template, "template");
        ErrorChecker.throwIfNotSet(this.host, "host");
        final VMDeploymentSpecification spec = new VMDeploymentSpecification(this.template,
                this.host, this.store);
        this.reset();
        return spec;
    }

}
