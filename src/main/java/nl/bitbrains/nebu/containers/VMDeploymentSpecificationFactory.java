package nl.bitbrains.nebu.containers;

import java.text.ParseException;

import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.common.interfaces.IBuilder;
import nl.bitbrains.nebu.common.topology.factory.PhysicalHostFactory;
import nl.bitbrains.nebu.common.topology.factory.PhysicalStoreFactory;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.common.util.xml.XMLFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMDeploymentSpecificationFactory implements XMLFactory<VMDeploymentSpecification> {

    public static final String TAG_ELEMENT_ROOT = "vmDeploymentSpecification";
    private static Logger logger = LogManager.getLogger();

    /**
     * Simple default constructor.
     */
    public VMDeploymentSpecificationFactory() {
    }

    @Override
    public final IBuilder<VMDeploymentSpecification> fromXML(final Element xml)
            throws ParseException {
        VMDeploymentSpecificationFactory.logger.entry();
        ErrorChecker.throwIfNullArgument(xml, "xml");
        final Element templateElem = xml.getChild(VMTemplateFactory.TAG_ELEMENT_ROOT);
        final Element hostElem = xml.getChild(PhysicalHostFactory.TAG_ELEMENT_ROOT);
        final Element storeElem = xml.getChild(PhysicalStoreFactory.TAG_ELEMENT_ROOT);

        final VMDeploymentSpecificationBuilder builder = new VMDeploymentSpecificationBuilder();
        final VMTemplate template = new VMTemplateFactory(false).fromXML(templateElem).build();
        final String host = hostElem.getTextTrim();
        builder.withTemplate(template).withHost(host);

        if (storeElem != null) {
            final String store = storeElem.getTextTrim();
            builder.withStore(store);
        }

        return VMDeploymentSpecificationFactory.logger.exit(builder);
    }

    @Override
    public final Element toXML(final VMDeploymentSpecification object) {
        VMDeploymentSpecificationFactory.logger.entry();
        ErrorChecker.throwIfNullArgument(object, "object");
        final Element xml = new Element(VMDeploymentSpecificationFactory.TAG_ELEMENT_ROOT);
        final Element templateElem = new VMTemplateFactory(false).toXML(object.getTemplate());

        final Element hostElem = new Element(PhysicalHostFactory.TAG_ELEMENT_ROOT);
        hostElem.setAttribute(IdentifiableFactory.TAG_ID, object.getHost());
        xml.addContent(templateElem).addContent(hostElem);

        if (object.getStore() != null) {
            final Element storeElem = new Element(PhysicalStoreFactory.TAG_ELEMENT_ROOT);
            storeElem.setAttribute(IdentifiableFactory.TAG_ID, object.getStore());
            xml.addContent(storeElem);
        }
        return VMDeploymentSpecificationFactory.logger.exit(xml);
    }
}
