package nl.bitbrains.nebu.containers;

import java.text.ParseException;

import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.common.util.xml.XMLFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;

/**
 * Converts {@link Application} objects to and from XML.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class ApplicationFactory extends IdentifiableFactory implements XMLFactory<Application> {

    public static final String TAG_ELEMENT_ROOT = "app";
    public static final String LIST_TAG_ELEMENT_ROOT = "apps";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String TAG_DEPLOYMENT_POLICY = "deploymentpolicy";

    private static Logger logger = LogManager.getLogger();
    private final boolean extensive;

    /**
     * Empty default constructor.
     */
    public ApplicationFactory() {
        this(true);
    }

    /**
     * @param extensive
     *            te set.
     */
    public ApplicationFactory(final boolean extensive) {
        this.extensive = extensive;
    }

    /**
     * Converts the {@link Application} to XML.
     * 
     * @param object
     *            to convert to XML.
     * @return the created XML element.
     */
    public final Element toXML(final Application object) {
        ApplicationFactory.logger.entry();
        final Element elem = super
                .createRootXMLElement(object, ApplicationFactory.TAG_ELEMENT_ROOT);
        if (object.getName() != null) {
            elem.setAttribute(new Attribute(ApplicationFactory.ATTRIBUTE_NAME, object.getName()));
        }
        if (this.extensive) {
            final Element policy = new Element(ApplicationFactory.TAG_DEPLOYMENT_POLICY);
            policy.setAttribute(ApplicationFactory.ATTRIBUTE_NAME, object.getDeploymentPolicy());
            elem.addContent(policy);
        }
        return ApplicationFactory.logger.exit(elem);
    }

    /**
     * Creates a {@link Application} from XML.
     * 
     * @param xml
     *            element to base the object on.
     * @return the created {@link Application}.
     * @throws ParseException
     *             if XML is not valid.
     */
    public final ApplicationBuilder fromXML(final Element xml) throws ParseException {
        ApplicationFactory.logger.entry();
        super.throwIfInvalidRoot(xml, ApplicationFactory.TAG_ELEMENT_ROOT);

        final ApplicationBuilder builder = new ApplicationBuilder();
        if (xml.getAttribute(IdentifiableFactory.TAG_ID) != null) {
            final Attribute idAttribute = super.getAttribute(xml, IdentifiableFactory.TAG_ID);
            builder.withUuid(idAttribute.getValue());
        }
        builder.withName(xml.getAttributeValue(ApplicationFactory.ATTRIBUTE_NAME));
        if (this.extensive) {
            builder.withDeploymentPolicy(xml.getChild(ApplicationFactory.TAG_DEPLOYMENT_POLICY)
                    .getAttributeValue(ApplicationFactory.ATTRIBUTE_NAME));
        }
        return ApplicationFactory.logger.exit(builder);
    }

    /**
     * Gets the xml element representing the deploymentpolicy (so that it can be
     * parsed by the right factory).
     * 
     * @param xml
     *            rootXMl to look in.
     * @return the xmlElement representing the deploymentpolicy.
     * @throws ParseException
     *             if the root is invalid.
     */
    public final Element getPolicyXML(final Element xml) throws ParseException {
        super.throwIfInvalidRoot(xml, ApplicationFactory.TAG_ELEMENT_ROOT);
        return xml.getChild(ApplicationFactory.TAG_DEPLOYMENT_POLICY);
    }
}
