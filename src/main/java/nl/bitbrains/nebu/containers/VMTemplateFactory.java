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
public class VMTemplateFactory extends IdentifiableFactory implements XMLFactory<VMTemplate> {

    public static final String TAG_ELEMENT_ROOT = "vmTemplate";
    public static final String LIST_TAG_ELEMENT_ROOT = "vmTemplates";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String TAG_LOADPROFILE = "loadProfile";
    public static final String TAG_CPU = "cpu";
    public static final String TAG_MEM = "mem";
    public static final String TAG_IO = "io";
    public static final String TAG_NET = "net";
    public static final String TAG_VMMCONFIG = "vmm";
    private static Logger logger = LogManager.getLogger();

    private final boolean extensive;

    /**
     * Empty default constructor.
     */
    public VMTemplateFactory() {
        this(true);
    }

    /**
     * Empty default constructor.
     * 
     * @param extensive
     *            indicates whether a full xml should be read/written.
     */
    public VMTemplateFactory(final boolean extensive) {
        this.extensive = extensive;
    }

    /**
     * Converts the {@link Application} to XML.
     * 
     * @param object
     *            to convert to XML.
     * @return the created XML element.
     */
    public final Element toXML(final VMTemplate object) {
        VMTemplateFactory.logger.entry();
        final Element elem = super.createRootXMLElement(object, VMTemplateFactory.TAG_ELEMENT_ROOT);
        elem.setAttribute(new Attribute(VMTemplateFactory.ATTRIBUTE_NAME, object.getName()));
        if (this.extensive) {
            final Element loadConfigElem = new Element(VMTemplateFactory.TAG_LOADPROFILE);
            loadConfigElem.addContent(new Element(VMTemplateFactory.TAG_CPU).setText(Integer
                    .toString(object.getCpu())));
            loadConfigElem.addContent(new Element(VMTemplateFactory.TAG_MEM).setText(Integer
                    .toString(object.getMem())));
            loadConfigElem.addContent(new Element(VMTemplateFactory.TAG_IO).setText(Integer
                    .toString(object.getIo())));
            loadConfigElem.addContent(new Element(VMTemplateFactory.TAG_NET).setText(Integer
                    .toString(object.getNet())));
            elem.addContent(loadConfigElem);
        }
        return VMTemplateFactory.logger.exit(elem);
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
    public final VMTemplateBuilder fromXML(final Element xml) throws ParseException {
        VMTemplateFactory.logger.entry();
        super.throwIfInvalidRoot(xml, VMTemplateFactory.TAG_ELEMENT_ROOT);
        final VMTemplateBuilder builder = new VMTemplateBuilder();
        final Element loadProfile = xml.getChild(VMTemplateFactory.TAG_LOADPROFILE);

        final Attribute idAttribute = xml.getAttribute(IdentifiableFactory.TAG_ID);
        if (idAttribute != null) {
            builder.withUuid(idAttribute.getValue());
        }
        builder.withName(super.getAttribute(xml, VMTemplateFactory.ATTRIBUTE_NAME).getValue());

        if (this.extensive) {
            builder.withCPU(Integer.parseInt(loadProfile
                    .getChildTextTrim(VMTemplateFactory.TAG_CPU)))
                    .withMem(Integer.parseInt(loadProfile
                            .getChildTextTrim(VMTemplateFactory.TAG_MEM)))
                    .withIO(Integer.parseInt(loadProfile.getChildTextTrim(VMTemplateFactory.TAG_IO)))
                    .withNet(Integer.parseInt(loadProfile
                            .getChildTextTrim(VMTemplateFactory.TAG_NET)));
        }
        return VMTemplateFactory.logger.exit(builder);
    }
}
