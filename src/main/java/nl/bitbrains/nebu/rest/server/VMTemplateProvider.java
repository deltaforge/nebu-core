package nl.bitbrains.nebu.rest.server;

import java.text.ParseException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateFactory;
import nl.bitbrains.nebu.rest.RESTRequestException;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplateProvider {

    private static Logger logger = LogManager.getLogger();
    private final Application app;
    private final VMTemplate template;

    /**
     * Simple constructor.
     * 
     * @param app
     *            to set.
     * @param template
     *            to set.
     */
    public VMTemplateProvider(final Application app, final VMTemplate template) {
        this.app = app;
        this.template = template;
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
    public final Response getTemplate() throws JDOMException {
        VMTemplateProvider.logger.entry();
        Response rep = null;
        final Element elem = new VMTemplateFactory().toXML(this.template);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        rep = Response.ok(doc, MediaType.APPLICATION_XML).build();
        return VMTemplateProvider.logger.exit(rep);
    }

    /**
     * 
     * Handles the POST Request on this path/:uuid.
     * 
     * @param doc
     *            post body.
     * 
     * @return http status code 200 iff all went successful.
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public final Response postTemplate(final Document doc) {
        VMTemplateProvider.logger.entry();
        Response rep = null;
        try {
            final VMTemplate newTemplate = new VMTemplateFactory()
                    .fromXML(XMLConverter.convertW3CDocumentJDOMElement(doc))
                    .withUuid(this.template.getUniqueIdentifier()).build();
            RequestSender.get().putTemplate(this.template.getUniqueIdentifier(),
                                            XMLConverter.convertW3CDocumentJDOMElement(doc)
                                                    .getChild(VMTemplateFactory.TAG_VMMCONFIG));
            this.app.putVMTemplate(newTemplate);
            rep = Response.ok().build();
        } catch (final ParseException e) {
            VMTemplateProvider.logger.catching(Level.ERROR, e);
            rep = Response.status(Status.BAD_REQUEST).build();
        } catch (final RESTRequestException e) {
            VMTemplateProvider.logger.catching(Level.ERROR, e);
            rep = Response.status(e.getHttpCode()).build();
        }
        return VMTemplateProvider.logger.exit(rep);
    }
}
