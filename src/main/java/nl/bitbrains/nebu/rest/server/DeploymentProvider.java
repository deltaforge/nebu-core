package nl.bitbrains.nebu.rest.server;

import java.text.ParseException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.VirtualMachineBuilder;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
import nl.bitbrains.nebu.common.util.UUIDGenerator;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentFactory;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;
import nl.bitbrains.nebu.containers.VMDeploymentSpecificationFactory;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * Handles /deploy/uuid queries.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeploymentProvider {

    public static final String PATH_START = "start";
    private static Logger logger = LogManager.getLogger();
    private final Application app;
    private final Deployment dep;

    /**
     * Simple constructor.
     * 
     * @param app
     *            to use.
     * @param dep
     *            to use.
     */
    public DeploymentProvider(final Application app, final Deployment dep) {
        this.app = app;
        this.dep = dep;
    }

    /**
     * Handles a GET request for a specific deployment.
     * 
     * @return all information on that deployment.
     * @throws JDOMException
     *             iff conversion fails.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public final Response getDeployment() throws JDOMException {
        DeploymentProvider.logger.entry();
        Response rep = null;
        final Element elem = new DeploymentFactory().toXML(this.dep);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        return DeploymentProvider.logger.exit(rep);
    }

    /**
     * Handles a POST request on a specific deployment, changing its
     * configuration if it has not yet been launched.
     * 
     * @param doc
     *            post body
     * @return a 200 on successful change
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public final Response postDeployment(final Document doc) {
        DeploymentProvider.logger.entry();
        Response rep = null;
        if (this.dep.isLaunched()) {
            rep = Response.status(Status.FORBIDDEN).build();
        } else {
            try {
                final Deployment newDeployment = new DeploymentFactory()
                        .fromXML(XMLConverter.convertW3CDocumentJDOMElement(doc))
                        .withUuid(this.dep.getUniqueIdentifier()).build();
                this.app.putDeployment(newDeployment);
                rep = Response.ok().build();
            } catch (final ParseException e) {
                rep = Response.status(Status.BAD_REQUEST).build();
            }
        }
        return DeploymentProvider.logger.exit(rep);
    }

    /**
     * Adds a previously launched VM to the deployment.
     * 
     * @param doc
     *            describes the vm to add.
     * @param templateID
     *            id of the template that this vm was launched with.
     * @return 200 iff successfull, 404 if not found and 500 if errored.
     */
    @PUT
    @Produces(MediaType.APPLICATION_XML)
    public final Response addVMToDeployment(final Document doc,
            @QueryParam("template") final String templateID) {
        DeploymentProvider.logger.entry();
        Response rep = null;
        if (!this.dep.isLaunched()) {
            rep = Response.status(Status.FORBIDDEN).build();
        } else {
            try {
                final Element xml = XMLConverter.convertW3CDocumentJDOMElement(doc);
                final VirtualMachine vm = new VirtualMachineFactory(false).fromXML(xml).build();
                final VMDeploymentSpecification spec = new VMDeploymentSpecificationFactory()
                        .fromXML(xml.getChild(VMDeploymentSpecificationFactory.TAG_ELEMENT_ROOT))
                        .build();
                this.dep.addVirtualMachines(vm, spec);
                rep = Response.ok().build();
            } catch (final ParseException e) {
                rep = Response.status(Status.BAD_REQUEST).build();
            }
        }
        return DeploymentProvider.logger.exit(rep);
    }

    /**
     * Handles the POST on the /start URI, indicating the vms should be
     * launched.
     * 
     * @return 202 if succesfully propagated.
     */
    @Path(DeploymentProvider.PATH_START)
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public final Response postDeploymentStart() {
        DeploymentProvider.logger.entry();
        Response rep = null;
        if (this.dep.isLaunched()) {
            rep = Response.status(Status.FORBIDDEN).build();
        } else {
            final List<VMDeploymentSpecification> specs = this.dep.getSpecs();
            for (final VMDeploymentSpecification spec : specs) {
                try {
                    final VirtualMachine vm = this.launchSpecification(spec);
                    this.dep.addVirtualMachines(vm, spec);
                } catch (final RESTRequestException e) {
                    DeploymentProvider.logger.catching(e);
                    return DeploymentProvider.logger.exit(Response.status(e.getHttpCode()).build());
                }
            }
            this.dep.setLaunched(true);
            rep = Response.accepted().build();
        }
        return DeploymentProvider.logger.exit(rep);
    }

    /**
     * Launches a VM according to the specified specification.
     * 
     * @param spec
     *            to launch
     * @return the launched {@link VirtualMachine}
     * @throws RESTRequestException
     *             if launching fails.
     */
    private VirtualMachine launchSpecification(final VMDeploymentSpecification spec)
            throws RESTRequestException {

        VirtualMachine vm;
        if (spec.getStore() != null) {
            vm = this.launchWithDisk(spec);
        } else {
            vm = this.launchWithoutDisk(spec);
        }
        DeploymentProvider.logger
                .debug("Just requested a VM to be launched. Status can be found under hash: "
                        + vm.getUniqueIdentifier());
        vm.setStatus(VirtualMachine.Status.LAUNCHING);
        return vm;
    }

    /**
     * @param spec
     *            to launch
     * @return the {@link VirtualMachine} representing the VM that just
     *         launched.
     * @throws RESTRequestException
     *             if launching fails.
     */
    private VirtualMachine launchWithDisk(final VMDeploymentSpecification spec)
            throws RESTRequestException {
        final String hostName = UUIDGenerator.generate(spec.getTemplate().getName());
        final Response postRep = RequestSender.get().postCreateVM(spec.getHost(),
                                                                  hostName,
                                                                  spec.getTemplate()
                                                                          .getUniqueIdentifier(),
                                                                  spec.getStore());
        final VirtualMachine vm = new VirtualMachineBuilder()
                .withUuid(postRep.getLocation().getRawPath().split("/")[2])
                .withHost(spec.getHost()).withDisk(spec.getStore()).build();
        return vm;
    }

    /**
     * @param spec
     *            to launch
     * @return the {@link VirtualMachine} representing the VM that just
     *         launched.
     * @throws RESTRequestException
     *             if launching fails.
     */
    private VirtualMachine launchWithoutDisk(final VMDeploymentSpecification spec)
            throws RESTRequestException {
        final String hostName = UUIDGenerator.generate(spec.getTemplate().getName());
        final Response postRep = RequestSender.get().postCreateVM(spec.getHost(),
                                                                  hostName,
                                                                  spec.getTemplate()
                                                                          .getUniqueIdentifier());
        final VirtualMachine vm = new VirtualMachineBuilder()
                .withUuid(postRep.getLocation().getRawPath().split("/")[2])
                .withHost(spec.getHost()).build();
        return vm;
    }
}
