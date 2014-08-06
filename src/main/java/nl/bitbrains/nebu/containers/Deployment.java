package nl.bitbrains.nebu.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides helper functions for the deployment of VMs on the available
 * clusters.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class Deployment implements Identifiable {

    public static final String UUID_PREFIX = "deployment";
    private static Logger logger = LogManager.getLogger();

    private final String uuid;
    private boolean launched;
    private final List<VMDeploymentSpecification> specs;
    private Map<VirtualMachine, VMDeploymentSpecification> virtualMachines;

    /**
     * Default constructor.
     * 
     * @param virtualMachines
     *            to set
     * @param specs
     *            to set
     * @param launched
     *            to set
     * @param uuid
     *            to set
     */
    protected Deployment(final String uuid, final boolean launched,
            final List<VMDeploymentSpecification> specs,
            final Map<VirtualMachine, VMDeploymentSpecification> virtualMachines) {
        Deployment.logger.entry();
        this.uuid = uuid;
        this.launched = launched;
        this.specs = specs;
        this.virtualMachines = virtualMachines;
        Deployment.logger.exit();
    }

    @Override
    public final String getUniqueIdentifier() {
        return this.uuid;
    }

    /**
     * @return the templateMapping.
     */
    public final List<VMDeploymentSpecification> getSpecs() {
        return new ArrayList<VMDeploymentSpecification>(this.specs);
    }

    /**
     * @param spec
     *            the {@link VMDeploymentSpecification} to add.
     */
    public final void addSpec(final VMDeploymentSpecification spec) {
        ErrorChecker.throwIfNullArgument(spec, "spec");
        this.specs.add(spec);
    }

    @Override
    public final int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (!(obj instanceof Deployment)) {
            return false;
        }
        final Deployment other = (Deployment) obj;
        return this.uuid.equals(other.uuid);
    }

    /**
     * 
     * @return the launched
     */
    public final boolean isLaunched() {
        return this.launched;
    }

    /**
     * @param launched
     *            to set.
     */
    public final void setLaunched(final boolean launched) {
        this.launched = launched;
    }

    /**
     * @return the vmMapping
     */
    public final List<VirtualMachine> getVirtualMachines() {
        return new ArrayList<VirtualMachine>(this.virtualMachines.keySet());
    }

    /**
     * @return the vmMapping
     */
    public final Map<VirtualMachine, VMDeploymentSpecification> getVirtualMachinesWithSpecs() {
        return new HashMap<VirtualMachine, VMDeploymentSpecification>(this.virtualMachines);
    }

    /**
     * Gets the {@link VMDeploymentSpecification} for a {@link VirtualMachine}.
     * 
     * @param vm
     *            to get the specs for.
     * @return the specs or null if not found.
     */
    public final VMDeploymentSpecification getSpecForVM(final VirtualMachine vm) {
        return this.virtualMachines.get(vm);
    }

    /**
     * @param launched
     *            filter by status.
     * @return the vmMapping
     */
    public final List<VirtualMachine> getVirtualMachines(final boolean launched) {
        final List<VirtualMachine> result = new ArrayList<VirtualMachine>();
        for (final VirtualMachine vm : this.getVirtualMachines()) {
            if (launched == vm.isOn()) {
                result.add(vm);
            }
        }
        return result;
    }

    /**
     * @param launched
     *            filter by status.
     * @return the vmMapping
     */
    public final Map<VirtualMachine, VMDeploymentSpecification> getVirtualMachinesWithSpecs(
            final boolean launched) {
        final Map<VirtualMachine, VMDeploymentSpecification> result = new HashMap<VirtualMachine, VMDeploymentSpecification>();
        for (final VirtualMachine vm : this.getVirtualMachines(launched)) {
            result.put(vm, this.getSpecForVM(vm));
        }
        return result;
    }

    /**
     * @param virtualMachines
     *            to set.
     */
    public final void setVirtualMachines(
            final Map<VirtualMachine, VMDeploymentSpecification> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    /**
     * @param vm
     *            to add.
     * @param spec
     *            of the vm to add.
     */
    public final void addVirtualMachines(final VirtualMachine vm,
            final VMDeploymentSpecification spec) {
        this.virtualMachines.put(vm, spec);
    }

    /**
     * Refreshes the vms by getting the latest information.
     */
    public final void refreshVMInformation() {
        Deployment.logger.entry();
        List<String> allVms;
        try {
            allVms = RequestSender.get().getVirtualMachines();
        } catch (final CacheException e1) {
            Deployment.logger.catching(Level.ERROR, e1);
            return;
        }
        for (final VirtualMachine vm : this.getVirtualMachines()) {
            if (!vm.isLaunching()) {
                // VMs that no longer exist should be removed.
                if (!allVms.contains(vm.getUniqueIdentifier())) {
                    this.virtualMachines.remove(vm);
                } else {
                    try {
                        final VirtualMachine updatedVM = RequestSender.get()
                                .getVirtualMachine(vm.getUniqueIdentifier());

                        vm.adoptFromOther(updatedVM);
                    } catch (final CacheException e) {
                        Deployment.logger.catching(Level.ERROR, e);
                    }
                }
            } else {
                VirtualMachine updatedVMStatus;
                try {
                    updatedVMStatus = RequestSender.get()
                            .getVirtualMachineStatus(vm.getUniqueIdentifier());
                    final VMDeploymentSpecification spec = this.removeVirtualMachine(vm);
                    vm.setUuid(updatedVMStatus.getUniqueIdentifier());
                    VirtualMachine updatedVM;
                    updatedVM = RequestSender.get().getVirtualMachine(vm.getUniqueIdentifier());
                    vm.adoptFromOther(updatedVM);
                    this.addVirtualMachines(vm, spec);
                } catch (final RESTRequestException e) {
                    if (e.getHttpCode() == Status.ACCEPTED.getStatusCode()) {
                        Deployment.logger.debug("VM is not yet fully launched");
                    } else {
                        Deployment.logger.catching(Level.WARN, e);
                    }
                } catch (final CacheException e) {
                    Deployment.logger.catching(Level.ERROR, e);
                }
            }
        }
        Deployment.logger.exit();
    }

    /**
     * @param vm
     *            to remove.
     * @return the removed specification.
     */
    public VMDeploymentSpecification removeVirtualMachine(final VirtualMachine vm) {
        Deployment.logger.entry();
        return Deployment.logger.exit(this.virtualMachines.remove(vm));
    }
}
