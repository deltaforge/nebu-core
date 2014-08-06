package nl.bitbrains.nebu.rest.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * Provides virtual machine information to clients.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public final class VirtualMachineProvider {

    public static final String PATH = "virt";
    private static Logger logger = LogManager.getLogger();
    private final Application app;
    private final boolean isReal;

    /**
     * Public constructor.
     * 
     * @param app
     *            to use
     * @param isReal
     *            true iff real vm information should be sent, rather than
     *            mocked.
     */
    public VirtualMachineProvider(final Application app, final boolean isReal) {
        this.app = app;
        this.isReal = isReal;
    }

    /**
     * Returns a list of all virtual machine uuids known to the virtual machine
     * manager.
     * 
     * @return A list of virtual machine uuids.
     * @throws JDOMException
     *             if it can not convert xml
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getVirtualResources() throws JDOMException {
        VirtualMachineProvider.logger.entry();
        Response rep = null;
        final Collection<Deployment> deployments = this.app.getDeployments();
        final List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        for (final Deployment dep : deployments) {
            dep.refreshVMInformation();
            for (final VirtualMachine vm : dep.getVirtualMachines()) {
                if (vm.isOn()) {
                    vms.add(vm);
                }
            }
        }
        final Element elem = XMLConverter
                .convertCollectionToJDOMElement(vms,
                                                new VirtualMachineFactory(false),
                                                VirtualMachineFactory.TAG_LIST_ELEMENT_ROOT,
                                                VirtualMachineFactory.TAG_ELEMENT_ROOT);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        return VirtualMachineProvider.logger.exit(rep);
    }

    /**
     * Returns information about a virtual machine based on the given uuid.
     * 
     * @param uuid
     *            The uuid of the virtual machine whose information needs to be
     *            retrieved.
     * @return Information about a given virtual machine.
     * @throws JDOMException
     *             if invalid xml conversion.
     */
    @GET
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_XML)
    public Response getVirtualMachineInfo(@PathParam("uuid") final String uuid)
            throws JDOMException {
        VirtualMachineProvider.logger.entry();
        Response rep = null;
        final VirtualMachine requestedVM = this.translateUUID2VM(uuid);
        if (requestedVM == null) {
            return VirtualMachineProvider.logger.exit(Response.status(Status.NOT_FOUND).build());
        }
        try {
            final VirtualMachine newVM = RequestSender.get().getVirtualMachine(uuid);
            requestedVM.adoptFromOther(newVM);
            if (!this.isReal) {
                newVM.setHost("host-" + newVM.getUniqueIdentifier());
                newVM.removeStores();
                newVM.addStore("store-" + newVM.getUniqueIdentifier());
            }
            final Element elem = new VirtualMachineFactory().toXML(newVM);

            final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
            rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        } catch (final RESTRequestException e) {
            if (e.getHttpCode() == Status.ACCEPTED.getStatusCode()) {
                VirtualMachineProvider.logger.debug("VM is not yet fully launched");
            } else {
                VirtualMachineProvider.logger.catching(Level.WARN, e);
            }
            return VirtualMachineProvider.logger.exit(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build());
        } catch (final CacheException e) {
            VirtualMachineProvider.logger.catching(Level.ERROR, e);
            return VirtualMachineProvider.logger.exit(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build());
        }

        return VirtualMachineProvider.logger.exit(rep);
    }

    /**
     * Removes a vm from the app.
     * 
     * @param uuid
     *            of the vm to remove.
     * @return 200 iff success.
     */
    @DELETE
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteVirtualMachine(@PathParam("uuid") final String uuid) {
        VirtualMachineProvider.logger.entry();
        Response rep = null;
        final VirtualMachine requestedVM = this.translateUUID2VM(uuid);
        if (requestedVM == null) {
            return VirtualMachineProvider.logger.exit(Response.status(Status.NOT_FOUND).build());
        }
        final Deployment dep = this.getDeploymentForVM(requestedVM);

        VirtualMachineProvider.logger.debug(dep);
        VirtualMachineProvider.logger.debug(dep.getUniqueIdentifier());
        VirtualMachineProvider.logger.debug(dep.getVirtualMachines());
        dep.removeVirtualMachine(requestedVM);
        VirtualMachineProvider.logger.debug(dep.getVirtualMachines());

        rep = Response.ok().build();
        return VirtualMachineProvider.logger.exit(rep);
    }

    /**
     * Tries to find the {@link VirtualMachine} with the matching uuid.
     * 
     * @param uuid
     *            to find the VM for.
     * @return the found VM or null if none found.
     */
    private VirtualMachine translateUUID2VM(final String uuid) {
        final Collection<Deployment> deployments = this.app.getDeployments();
        VirtualMachine requestedVM = null;
        for (final Deployment dep : deployments) {
            final List<VirtualMachine> vms = dep.getVirtualMachines();
            for (final VirtualMachine vm : vms) {
                if (vm.getUniqueIdentifier().equals(uuid)) {
                    requestedVM = vm;
                    break;
                }
            }
            if (requestedVM != null) {
                break;
            }
        }
        return requestedVM;
    }

    /**
     * Gets a deployment for a vm.
     * 
     * @param vm
     *            to find the deployment for.
     * @return the deployment that the vm belongs to.
     */
    private Deployment getDeploymentForVM(final VirtualMachine vm) {
        final Collection<Deployment> deployments = this.app.getDeployments();
        for (final Deployment dep : deployments) {
            final List<VirtualMachine> vms = dep.getVirtualMachines();
            if (vms.contains(vm)) {
                return dep;
            }
        }
        return null;
    }
}
