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
import org.w3c.dom.Document;

/**
 * Can reload the topology from the appropriate extension.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class TopologyCacheLoader implements CacheLoader<Object> {

    private static Logger logger = LogManager.getLogger();

    /**
     * Empty default Constructor.
     */
    public TopologyCacheLoader() {

    }

    @Override
    public final PhysicalTopology refresh() throws RESTRequestException {
        TopologyCacheLoader.logger.entry();
        final Invocation.Builder builder = RequestBuilder.get().newGetTopologyClient();
        final Document doc = RequestSender.performGETRequestAndCheckResponse(builder,
                                                                             Document.class);
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(doc);
        final TopologyFactories factories = TopologyFactories.createDefault();
        try {
            return TopologyCacheLoader.logger.exit(new PhysicalTopology(factories
                    .getPhysicalRootFactory().fromXML(elem).build()));
        } catch (final ParseException e) {
            throw TopologyCacheLoader.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE, Response.Status.INTERNAL_SERVER_ERROR
                            .getStatusCode(), e));
        }
    }

}
