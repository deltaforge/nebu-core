package nl.bitbrains.nebu.containers;

import java.text.ParseException;
import java.util.HashMap;

import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.common.util.xml.XMLReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * Converts {@link Application} objects to and from XML.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeploymentRequestFactory extends IdentifiableFactory implements
        XMLReader<DeploymentRequest> {

    public static final String TAG_ELEMENT_ROOT = "deploymentRequest";
    public static final String TAG_ELEMENT_TEMPLATE = "template";
    public static final String TAG_ELEMENT_NUMBER = "number";
    private static Logger logger = LogManager.getLogger();

    private final Application application;

    /**
     * @param application
     *            the {@link Application} for which the
     *            {@link DeploymentRequest} is created.
     */
    public DeploymentRequestFactory(final Application application) {
        this.application = application;
    }

    /**
     * Creates a {@link DeploymentRequest.Builder} and sets any fields found in
     * the input XML.
     * 
     * @param xml
     *            element to base the object on.
     * @return the created {@link DeploymentRequest.Builder}.
     * @throws ParseException
     *             if XML is not valid.
     */
    public final DeploymentRequest fromXML(final Element xml) throws ParseException {
        // TODO: Check exceptions for getAttributeValue, parseInt, templ = null,
        // etc.
        DeploymentRequestFactory.logger.entry();
        super.throwIfInvalidRoot(xml, DeploymentRequestFactory.TAG_ELEMENT_ROOT);

        final HashMap<VMTemplate, Integer> requests = new HashMap<VMTemplate, Integer>();
        for (final Element elem : xml.getChildren(DeploymentRequestFactory.TAG_ELEMENT_TEMPLATE)) {
            final VMTemplate templ = this.application.getVMTemplate(elem
                    .getAttributeValue(IdentifiableFactory.TAG_ID));
            final int number = Integer.parseInt(elem
                    .getChildTextTrim(DeploymentRequestFactory.TAG_ELEMENT_NUMBER));
            requests.put(templ, number);
        }
        return DeploymentRequestFactory.logger
                .exit(new DeploymentRequest.Builder().withApplication(this.application)
                        .withRequests(requests).createDeploymentRequest());
    }
}
