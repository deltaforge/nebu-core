package nl.bitbrains.nebu.containers;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.common.util.xml.XMLFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * Converts {@link Application} objects to and from XML.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DeploymentFactory extends IdentifiableFactory implements XMLFactory<Deployment> {

    public static final String TAG_ELEMENT_ROOT = "deployment";
    public static final String LIST_TAG_ELEMENT_ROOT = "deployments";
    private static final String TAG_ELEMENT_LAUNCHED = "launched";
    private static Logger logger = LogManager.getLogger();

    private final boolean extensive;

    /**
     * Creates a DeploymentFactory in the context of an {@link Application}.
     * 
     */
    public DeploymentFactory() {
        this(true);
    }

    /**
     * Creates a DeploymentFactory in the context of an {@link Application}.
     * 
     * @param extensive
     *            indicates whether full copies should be read/written.
     */
    public DeploymentFactory(final boolean extensive) {
        this.extensive = extensive;
    }

    @Override
    public final Element toXML(final Deployment object) {
        DeploymentFactory.logger.entry();
        final Element xml = super.createRootXMLElement(object, DeploymentFactory.TAG_ELEMENT_ROOT);
        if (this.extensive) {
            xml.addContent(new Element(DeploymentFactory.TAG_ELEMENT_LAUNCHED).setText(Boolean
                    .toString(object.isLaunched())));
            if (object.isLaunched()) {
                final Map<VirtualMachine, VMDeploymentSpecification> vms = object
                        .getVirtualMachinesWithSpecs(true);
                xml.addContent(XMLConverter.convertMapToJDOMElement(vms, new VirtualMachineFactory(
                        false), new VMDeploymentSpecificationFactory()));
            } else {
                final List<VMDeploymentSpecification> specs = object.getSpecs();
                for (final VMDeploymentSpecification spec : specs) {
                    final Element specElem = new VMDeploymentSpecificationFactory().toXML(spec);
                    xml.addContent(specElem);
                }
            }
        }
        return DeploymentFactory.logger.exit(xml);
    }

    /**
     * Creates a {@link Deployment} from XML.
     * 
     * @param xml
     *            element to base the object on.
     * @return the created {@link Application}.
     * @throws ParseException
     *             if XML is not valid.
     */
    public final DeploymentBuilder fromXML(final Element xml) throws ParseException {
        DeploymentFactory.logger.entry();
        super.throwIfInvalidRoot(xml, DeploymentFactory.TAG_ELEMENT_ROOT);
        final DeploymentBuilder result = new DeploymentBuilder();
        result.withUuid(xml.getAttributeValue(IdentifiableFactory.TAG_ID));
        if (this.extensive) {
            final boolean launched = Boolean.parseBoolean(xml
                    .getChildTextTrim(DeploymentFactory.TAG_ELEMENT_LAUNCHED));
            result.withLaunched(launched);
            if (launched) {
                final Map<VirtualMachine, VMDeploymentSpecification> vms = XMLConverter
                        .convertJDOMElementToMap(xml.getChild(XMLConverter.TAG_MAP),
                                                 new VirtualMachineFactory(false),
                                                 new VMDeploymentSpecificationFactory());
                for (final Entry<VirtualMachine, VMDeploymentSpecification> entry : vms.entrySet()) {
                    result.withVM(entry.getKey(), entry.getValue());
                }
            } else {
                for (final Element specElem : xml
                        .getChildren(VMDeploymentSpecificationFactory.TAG_ELEMENT_ROOT)) {
                    final VMDeploymentSpecification spec = new VMDeploymentSpecificationFactory()
                            .fromXML(specElem).build();
                    result.withSpec(spec);
                }
            }
        }
        return DeploymentFactory.logger.exit(result);
    }
}
