package nl.bitbrains.nebu.rest.client;

import java.text.ParseException;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.cache.CacheLoader;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.rest.RESTRequestException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.w3c.dom.Document;

/**
 * Can update the /virt/uuid resource.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VirtItemCacheLoader implements CacheLoader<Object> {

    private final String uuid;
    private static Logger logger = LogManager.getLogger();

    /**
     * Simple constructor, takes the uuid to fetch.
     * 
     * @param uuid
     *            to fetch the information of.
     */
    public VirtItemCacheLoader(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public final VirtualMachine refresh() throws RESTRequestException {
        VirtItemCacheLoader.logger.entry();
        final Invocation.Builder builder = RequestBuilder.get()
                .newGetVirtualMachineClient(this.uuid);
        final Document doc = RequestSender.performGETRequestAndCheckResponse(builder,
                                                                             Document.class);
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(doc);
        final VirtualMachineFactory factory = new VirtualMachineFactory();
        try {
            return VirtItemCacheLoader.logger.exit(factory.fromXML(elem).build());
        } catch (final ParseException e) {
            throw VirtItemCacheLoader.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE, Response.Status.INTERNAL_SERVER_ERROR
                            .getStatusCode(), e));
        }
    }

}
