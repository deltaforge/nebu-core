package nl.bitbrains.nebu.containers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bitbrains.nebu.common.interfaces.IBuilder;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.topology.PhysicalResource;
import nl.bitbrains.nebu.common.util.ErrorChecker;

/**
 * Builder class for the {@link Application}.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class ApplicationBuilder implements IBuilder<Application> {
    private String name;
    private String uuid;
    private String deploymentPolicy;
    private Map<String, VMTemplate> templates;
    private Map<String, Deployment> deployments;

    /**
     * Simple constructor.
     */
    public ApplicationBuilder() {
        this.reset();
    }

    /**
     * Resets the builder.
     */
    public final void reset() {
        this.uuid = null;
        this.name = null;
        this.deploymentPolicy = "random";
        this.templates = new HashMap<String, VMTemplate>();
        this.deployments = new HashMap<String, Deployment>();
    }

    /**
     * @param uuid
     *            to build with.
     * @return this for fluency
     */
    public final ApplicationBuilder withUuid(final String uuid) {
        ErrorChecker.throwIfNullArgument(uuid, Identifiable.UUID_NAME);
        this.uuid = uuid;
        return this;
    }

    /**
     * @param name
     *            to build with
     * @return this for fluency.
     */
    public final ApplicationBuilder withName(final String name) {
        ErrorChecker.throwIfNullArgument(name, "name");
        this.name = name;
        return this;
    }

    /**
     * @param policy
     *            to build with
     * @return this for fluency.
     */
    public final ApplicationBuilder withDeploymentPolicy(final String policy) {
        ErrorChecker.throwIfNullArgument(policy, "policy");
        this.deploymentPolicy = policy;
        return this;
    }

    /**
     * @param template
     *            to include.
     * @return this for fluency.
     */
    public final ApplicationBuilder withTemplate(final VMTemplate template) {
        ErrorChecker.throwIfNullArgument(template, "template");
        this.templates.put(template.getUniqueIdentifier(), template);
        return this;
    }

    /**
     * @param templates
     *            to include.
     * @return this for fluency.
     */
    public final ApplicationBuilder withTemplates(final List<VMTemplate> templates) {
        ErrorChecker.throwIfNullArgument(templates, "templates");
        for (final VMTemplate template : templates) {
            this.withTemplate(template);
        }
        return this;
    }

    /**
     * @param deployment
     *            to include.
     * @return this for fluency.
     */
    public final ApplicationBuilder withDeployment(final Deployment deployment) {
        ErrorChecker.throwIfNullArgument(deployment, "deployment");
        this.deployments.put(deployment.getUniqueIdentifier(), deployment);
        return this;
    }

    /**
     * @param deployments
     *            to include.
     * @return this for fluency.
     */
    public final ApplicationBuilder withDeployments(final List<Deployment> deployments) {
        ErrorChecker.throwIfNullArgument(deployments, "deployments");
        for (final Deployment deployment : deployments) {
            this.withDeployment(deployment);
        }
        return this;
    }

    /**
     * @return the build {@link Application} object.
     */
    public final Application build() {
        ErrorChecker.throwIfNotSet(this.uuid, PhysicalResource.UUID_NAME);
        ErrorChecker.throwIfNotSet(this.deploymentPolicy, "deploymentPolicy");
        final Application app = new Application(this.uuid, this.name, this.deploymentPolicy,
                this.templates, this.deployments);
        this.reset();
        return app;
    }

}
