package nl.bitbrains.nebu.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.interfaces.IBuilder;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.topology.PhysicalResource;
import nl.bitbrains.nebu.common.util.ErrorChecker;

/**
 * Builder class for the {@link Deployment}.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeploymentBuilder implements IBuilder<Deployment> {
    private String uuid;
    private boolean launched;
    private List<VMDeploymentSpecification> specs;
    private Map<VirtualMachine, VMDeploymentSpecification> virtualMachines;

    /**
     * Simple constructor.
     */
    public DeploymentBuilder() {
        this.reset();
    }

    /**
     * Resets the builder.
     */
    public final void reset() {
        this.uuid = null;
        this.launched = false;
        this.specs = new ArrayList<VMDeploymentSpecification>();
        this.virtualMachines = new HashMap<VirtualMachine, VMDeploymentSpecification>();
    }

    /**
     * @param uuid
     *            to build with.
     * @return this for fluency
     */
    public final DeploymentBuilder withUuid(final String uuid) {
        ErrorChecker.throwIfNullArgument(uuid, Identifiable.UUID_NAME);
        this.uuid = uuid;
        return this;
    }

    /**
     * @param launched
     *            to build with
     * @return this for fluency.
     */
    public final DeploymentBuilder withLaunched(final boolean launched) {
        this.launched = launched;
        return this;
    }

    /**
     * @param spec
     *            to include.
     * @return this for fluency.
     */
    public final DeploymentBuilder withSpec(final VMDeploymentSpecification spec) {
        ErrorChecker.throwIfNullArgument(spec, "spec");
        this.specs.add(spec);
        return this;
    }

    /**
     * @param specs
     *            to include.
     * @return this for fluency.
     */
    public final DeploymentBuilder withSpecs(final List<VMDeploymentSpecification> specs) {
        ErrorChecker.throwIfNullArgument(specs, "specs");
        for (final VMDeploymentSpecification spec : specs) {
            this.withSpec(spec);
        }
        return this;
    }

    /**
     * @param virtualMachine
     *            to include.
     * @param spec
     *            this VM was launched with.
     * @return this for fluency.
     */
    public final DeploymentBuilder withVM(final VirtualMachine virtualMachine,
            final VMDeploymentSpecification spec) {
        ErrorChecker.throwIfNullArgument(virtualMachine, "virtualMachine");
        this.virtualMachines.put(virtualMachine, spec);
        return this;
    }

    /**
     * @return the build {@link Deployment} object.
     */
    public final Deployment build() {
        ErrorChecker.throwIfNotSet(this.uuid, PhysicalResource.UUID_NAME);
        final Deployment dep = new Deployment(this.uuid, this.launched, this.specs,
                this.virtualMachines);
        this.reset();
        return dep;
    }

}