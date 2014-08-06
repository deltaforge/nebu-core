package nl.bitbrains.nebu.rest.client;

import java.text.ParseException;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.cache.CacheLoader;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactories;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.rest.RESTRequestException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Document;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplateTopologyCacheLoader implements CacheLoader<Object> {

    private final String uuid;
    private static Logger logger = LogManager.getLogger();

    /**
     * Empty default Constructor.
     * 
     * @param uuid
     *            of the VMtemplate to get the topology for.
     */
    public VMTemplateTopologyCacheLoader(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public final PhysicalTopology refresh() throws RESTRequestException {
        VMTemplateTopologyCacheLoader.logger.entry();
        final Invocation.Builder builder = RequestBuilder.get()
                .newGetTopologyForVMTemplateClient(this.uuid);
        final Document doc = RequestSender.performGETRequestAndCheckResponse(builder,
                                                                             Document.class);
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(doc);

        // Printing XML for debugging.
        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        VMTemplateTopologyCacheLoader.logger.debug(outputter.outputString(elem));

        final TopologyFactories factories = TopologyFactories.createDefault();
        try {
            return VMTemplateTopologyCacheLoader.logger.exit(new PhysicalTopology(factories
                    .getPhysicalRootFactory().fromXML(elem).build()));
        } catch (final ParseException e) {
            throw VMTemplateTopologyCacheLoader.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE, Response.Status.INTERNAL_SERVER_ERROR
                            .getStatusCode(), e));
        }
    }
}
