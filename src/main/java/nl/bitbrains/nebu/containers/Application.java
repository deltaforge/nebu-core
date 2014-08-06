package nl.bitbrains.nebu.containers;

import java.util.Collection;
import java.util.Map;

import nl.bitbrains.nebu.common.interfaces.Identifiable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds information related to an Application users can deploy vms for.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class Application implements Identifiable, VMTemplateCollection {

    public static final String UUID_PREFIX = "app";
    private static Logger logger = LogManager.getLogger();

    private String name;
    private final String uuid;
    private String deploymentPolicy;
    private final Map<String, VMTemplate> templates;
    private final Map<String, Deployment> deployments;

    /**
     * Simple constructor that sets the name.
     * 
     * @param name
     *            to use.
     * @param uuid
     *            identifier.
     * @param deploymentPolicy
     *            to use.
     * @param deployments
     *            to use.
     * @param templates
     *            to use.
     */
    protected Application(final String uuid, final String name, final String deploymentPolicy,
            final Map<String, VMTemplate> templates, final Map<String, Deployment> deployments) {
        Application.logger.entry();
        this.uuid = uuid;
        this.name = name;
        this.deploymentPolicy = deploymentPolicy;
        this.templates = templates;
        this.deployments = deployments;
        Application.logger.exit();
    }

    /**
     * @return the name of the application.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * @param newName
     *            sets the name
     */
    public final void setName(final String newName) {
        this.name = newName;
    }

    /**
     * @return the deploymentPolicy
     */
    public final String getDeploymentPolicy() {
        return this.deploymentPolicy;
    }

    /**
     * @param newPolicy
     *            to set.
     */
    public final void setDeploymentPolicy(final String newPolicy) {
        this.deploymentPolicy = newPolicy;
    }

    @Override
    public final String getUniqueIdentifier() {
        return this.uuid;
    }

    /**
     * Puts a template in the app.
     * 
     * @param template
     *            template to add.
     */
    public final void putVMTemplate(final VMTemplate template) {
        this.templates.put(template.getUniqueIdentifier(), template);
    }

    /**
     * Gets all templates of the app.
     * 
     * @return Collection of templates.
     */
    public final Collection<VMTemplate> getVMTemplates() {
        return this.templates.values();
    }

    @Override
    public final int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (!(obj instanceof Application)) {
            return false;
        }
        final Application other = (Application) obj;
        return this.uuid.equals(other.uuid);
    }

    /**
     * @param templateUUID
     *            id to look for.
     * @return the template if found, null otherwise.
     */
    public final VMTemplate getVMTemplate(final String templateUUID) {
        return this.templates.get(templateUUID);
    }

    /**
     * @param deployment
     *            to add.
     */
    public final void putDeployment(final Deployment deployment) {
        this.deployments.put(deployment.getUniqueIdentifier(), deployment);
    }

    /**
     * @param depuuid
     *            id to look for.
     * @return the deployment if found, null otherwise.
     */
    public final Deployment getDeployment(final String depuuid) {
        return this.deployments.get(depuuid);
    }

    /**
     * @return the deployments.
     */
    public final Collection<Deployment> getDeployments() {
        return this.deployments.values();
    }

}
