package nl.bitbrains.nebu.rest.server;

import java.text.ParseException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.ApplicationFactory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * Deals with all requests on /app/uuid.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class AppProvider {

    private static Logger logger = LogManager.getLogger();
    private final Application app;
    private final boolean isReal = true;

    /**
     * Default simple empty constructor.
     * 
     * @param app
     *            to set.
     */
    public AppProvider(final Application app) {
        this.app = app;
    }

    /**
     * @return the handler for the ./deploy path
     */
    @Path(DeploymentsProvider.PATH)
    public final DeploymentsProvider deployment() {
        return new DeploymentsProvider(this.app);
    }

    /**
     * @return the handler for the ./templates path
     */
    @Path(VMTemplatesProvider.PATH)
    public final VMTemplatesProvider templates() {
        return new VMTemplatesProvider(this.app);
    }

    /**
     * @return the handler for the ./virt path
     */
    @Path(VirtualMachineProvider.PATH)
    public final VirtualMachineProvider virt() {
        return new VirtualMachineProvider(this.app, this.isReal);
    }

    /**
     * @return the handler for the ./phys path
     */
    @Path(PhysicalMachineProvider.PATH)
    public final PhysicalMachineProvider phys() {
        return new PhysicalMachineProvider(this.app, this.isReal);
    }

    /**
     * Handles the GET Request on this path/:uuid.
     * 
     * @return the app information
     * @throws JDOMException
     *             iff conversion fails.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public final Response getApp() throws JDOMException {
        Response rep = null;
        final Element elem = new ApplicationFactory().toXML(this.app);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        return rep;
    }

    /**
     * Handles the POST Request on this path/:uuid.
     * 
     * @param doc
     *            Document supplied to post request
     * @return http status code 200 iff all went successful.
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public final Response postApp(final Document doc) {
        AppProvider.logger.entry();
        Response rep = null;
        try {
            final ApplicationBuilder builder = new ApplicationFactory().fromXML(XMLConverter
                    .convertW3CDocumentJDOMElement(doc));
            builder.withUuid(this.app.getUniqueIdentifier());
            final Application receivedApp = builder.build();
            this.app.setName(receivedApp.getName());
            this.app.setDeploymentPolicy(receivedApp.getDeploymentPolicy());
            rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        } catch (final ParseException e) {
            AppProvider.logger.catching(Level.WARN, e);
            rep = Response.status(Status.BAD_REQUEST).build();
        }
        return AppProvider.logger.exit(rep);
    }

}
