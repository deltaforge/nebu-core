package nl.bitbrains.nebu.rest.server;

import java.net.URI;
import java.text.ParseException;
import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.util.UUIDGenerator;
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
 * Handles the requests on the /app/:uuid/vmtemplates uri.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplatesProvider {
    public static final String PATH = "vmtemplates";
    public static final String PATH_UUID = "{" + VMTemplatesProvider.UUID_NAME + "}";
    public static final String UUID_NAME = "templateUUID";
    private static Logger logger = LogManager.getLogger();

    private final Application app;

    /**
     * Simple constructor.
     * 
     * @param app
     *            to use
     */
    public VMTemplatesProvider(final Application app) {
        this.app = app;
    }

    /**
     * @param templateID
     *            id of the template to route the queries to.
     * @return the {@link VMTemplateProvider} that can handle the queries.
     */
    @Path(VMTemplatesProvider.PATH_UUID)
    public final Object getVMTemplateProvider(
            @PathParam(VMTemplatesProvider.UUID_NAME) final String templateID) {
        if (this.app.getVMTemplate(templateID) == null) {
            return VMTemplatesProvider.logger.exit(Response.status(Response.Status.NOT_FOUND)
                    .build());
        }
        return new VMTemplateProvider(this.app, this.app.getVMTemplate(templateID));
    }

    /**
     * Handles the GET request on this path.
     * 
     * @return list of the currently known templates
     * @throws JDOMException
     *             iff conversion fails
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public final Response getTemplates() throws JDOMException {
        VMTemplatesProvider.logger.entry();
        Response rep = null;
        final Collection<VMTemplate> templates = this.app.getVMTemplates();
        final Element elem = XMLConverter
                .convertCollectionToJDOMElement(templates,
                                                new VMTemplateFactory(false),
                                                VMTemplateFactory.LIST_TAG_ELEMENT_ROOT,
                                                VMTemplateFactory.TAG_ELEMENT_ROOT);
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(elem);
        rep = Response.ok(doc, MediaType.APPLICATION_XML).build();

        return VMTemplatesProvider.logger.exit(rep);
    }

    /**
     * 
     * Handles the POST request on this path.
     * 
     * 
     * @param doc
     *            post body
     * @return location header set iff successfull.
     * 
     * @throws ParseException
     *             iff xml conversion fails.
     */
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public final Response postTemplates(final Document doc) throws ParseException {
        VMTemplatesProvider.logger.entry();
        Response rep = null;
        final String key = UUIDGenerator.generate(VMTemplate.UUID_PREFIX);
        final VMTemplate template = new VMTemplateFactory()
                .fromXML(XMLConverter.convertW3CDocumentJDOMElement(doc)).withUuid(key).build();
        try {
            RequestSender.get().putTemplate(key,
                                            XMLConverter.convertW3CDocumentJDOMElement(doc)
                                                    .getChild(VMTemplateFactory.TAG_VMMCONFIG));
            this.app.putVMTemplate(template);
            final URI location = URI.create(key);
            rep = Response.created(location).build();
        } catch (final RESTRequestException e) {
            VMTemplatesProvider.logger.catching(Level.ERROR, e);
            rep = Response.status(e.getHttpCode()).build();
        }
        return VMTemplatesProvider.logger.exit(rep);
    }

}
