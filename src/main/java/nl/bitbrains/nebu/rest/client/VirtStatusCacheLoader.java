package nl.bitbrains.nebu.rest.client;

import java.text.ParseException;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.VirtualMachineBuilder;
import nl.bitbrains.nebu.common.cache.CacheLoader;
import nl.bitbrains.nebu.common.factories.StringFactory;
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
public class VirtStatusCacheLoader implements CacheLoader<Object> {

    public static final int EXPIRATION_TIME = 5;
    private static Logger logger = LogManager.getLogger();
    private final String uuid;

    /**
     * Simple constructor, takes the uuid to fetch.
     * 
     * @param uuid
     *            to fetch the information of.
     */
    public VirtStatusCacheLoader(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public final VirtualMachine refresh() throws RESTRequestException {
        VirtStatusCacheLoader.logger.entry();
        final Invocation.Builder builder = RequestBuilder.get()
                .newGetVirtualMachineStatusClient(this.uuid);
        final Document doc = RequestSender.performGETRequestAndCheckResponse(builder,
                                                                             Document.class);
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(doc);
        final StringFactory factory = new StringFactory();
        try {
            return VirtStatusCacheLoader.logger.exit(new VirtualMachineBuilder().withUuid(factory
                    .fromXML(elem).build()).build());
        } catch (final ParseException e) {
            throw VirtStatusCacheLoader.logger.throwing(new RESTRequestException(
                    RequestSender.INVALID_RESPONSE, Response.Status.INTERNAL_SERVER_ERROR
                            .getStatusCode(), e));
        }
    }

}
