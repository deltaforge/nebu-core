package nl.bitbrains.nebu.rest.client;

import java.text.ParseException;
import java.util.List;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.cache.CacheLoader;
import nl.bitbrains.nebu.common.factories.StringFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.rest.RESTRequestException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.w3c.dom.Document;

/**
 * Can refresh the /virt resource from the vm-manager extension.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VirtListCacheLoader implements CacheLoader<Object> {

    private static Logger logger = LogManager.getLogger();

    /**
     * Empty default constructor.
     */
    public VirtListCacheLoader() {
    }

    @Override
    public final List<String> refresh() throws RESTRequestException {
        VirtListCacheLoader.logger.entry();
        final Invocation.Builder builder = RequestBuilder.get().newGetVirtualMachinesClient();
        final Document doc = RequestSender.performGETRequestAndCheckResponse(builder,
                                                                             Document.class);
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(doc);
        try {
            return VirtListCacheLoader.logger.exit(XMLConverter
                    .convertJDOMElementToList(elem, new StringFactory()));
        } catch (final ParseException e) {
            // A parse exception should never occur here, as a StringFactory can
            // not throw it.
            throw VirtListCacheLoader.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE, Response.Status.INTERNAL_SERVER_ERROR
                            .getStatusCode(), e));
        }
    }

}
