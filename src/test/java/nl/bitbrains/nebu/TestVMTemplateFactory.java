package nl.bitbrains.nebu;

import java.text.ParseException;

import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateBuilder;
import nl.bitbrains.nebu.containers.VMTemplateFactory;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class TestVMTemplateFactory {

    private final String id = "id";
    private final String name = "name";
    private final int cpu = 1;
    private final int mem = 2;
    private final int io = 3;
    private final int net = 4;

    private VMTemplateFactory extensiveFactory;
    private VMTemplateFactory nonExtensiveFactory;
    private VMTemplate template;

    @Before
    public void setUp() {
        this.extensiveFactory = new VMTemplateFactory();
        this.nonExtensiveFactory = new VMTemplateFactory(false);
        this.template = new VMTemplateBuilder().withUuid(this.id).withName(this.name)
                .withCPU(this.cpu).withIO(this.io).withNet(this.net).withMem(this.mem).build();
    }

    public Element makeLoadElement() {
        final Element res = new Element(VMTemplateFactory.TAG_LOADPROFILE);
        final Element cpuElem = new Element(VMTemplateFactory.TAG_CPU).addContent(Integer
                .toString(this.cpu));
        final Element memElem = new Element(VMTemplateFactory.TAG_MEM).addContent(Integer
                .toString(this.mem));
        final Element ioElem = new Element(VMTemplateFactory.TAG_IO).addContent(Integer
                .toString(this.io));
        final Element netElem = new Element(VMTemplateFactory.TAG_NET).addContent(Integer
                .toString(this.net));
        res.addContent(cpuElem).addContent(memElem).addContent(ioElem).addContent(netElem);
        return res;
    }

    @Test
    public void testFromXMLNull() throws ParseException {
        boolean caught = false;
        try {
            this.extensiveFactory.fromXML(null);
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testFromXMLWrongRoot() throws ParseException {
        final Element elem = new Element("WRONGROOTNAME");
        boolean caught = false;
        try {
            this.extensiveFactory.fromXML(elem);
        } catch (final ParseException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testFromXMLWithIDNonExtensive() throws ParseException {
        final Element root = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        root.setAttribute(IdentifiableFactory.TAG_ID, this.id);
        root.setAttribute(VMTemplateFactory.ATTRIBUTE_NAME, this.name);
        final VMTemplate template = this.nonExtensiveFactory.fromXML(root).build();
        Assert.assertEquals(this.id, template.getUniqueIdentifier());
        Assert.assertEquals(this.name, template.getName());
    }

    @Test
    public void testFromXMLWithIDNonExtensiveWithProfile() throws ParseException {
        final Element root = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        root.setAttribute(IdentifiableFactory.TAG_ID, this.id);
        root.setAttribute(VMTemplateFactory.ATTRIBUTE_NAME, this.name);
        root.addContent(this.makeLoadElement());
        final VMTemplate template = this.nonExtensiveFactory.fromXML(root).build();
        Assert.assertEquals(0, template.getCpu());
    }

    @Test
    public void testFromXMLWithoutIDNonExtensive() throws ParseException {
        final Element root = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        root.setAttribute(VMTemplateFactory.ATTRIBUTE_NAME, this.name);
        boolean caught = false;
        try {
            this.nonExtensiveFactory.fromXML(root).build();
        } catch (final IllegalStateException e) {
            caught = true;
        }

        Assert.assertTrue(caught);
    }

    @Test
    public void testFromXMLWithIDExtensive() throws ParseException {
        final Element root = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        root.setAttribute(IdentifiableFactory.TAG_ID, this.id);
        root.setAttribute(VMTemplateFactory.ATTRIBUTE_NAME, this.name);
        root.addContent(this.makeLoadElement());
        final VMTemplate template = this.extensiveFactory.fromXML(root).build();
        Assert.assertEquals(this.id, template.getUniqueIdentifier());
        Assert.assertEquals(this.name, template.getName());
    }

    @Test
    public void testFromXMLWithIDExtensiveCheckLoad() throws ParseException {
        final Element root = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        root.setAttribute(IdentifiableFactory.TAG_ID, this.id);
        root.setAttribute(VMTemplateFactory.ATTRIBUTE_NAME, this.name);
        root.addContent(this.makeLoadElement());
        final VMTemplate template = this.extensiveFactory.fromXML(root).build();
        Assert.assertEquals(this.cpu, template.getCpu());
        Assert.assertEquals(this.mem, template.getMem());
        Assert.assertEquals(this.io, template.getIo());
        Assert.assertEquals(this.net, template.getNet());
    }

    @Test
    public void testFromXMLWithoutIDExtensive() throws ParseException {
        final Element root = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        root.setAttribute(VMTemplateFactory.ATTRIBUTE_NAME, this.name);
        root.addContent(this.makeLoadElement());
        boolean caught = false;
        try {
            this.extensiveFactory.fromXML(root).build();
        } catch (final IllegalStateException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testToXMLNull() {
        boolean caught = false;
        try {
            this.extensiveFactory.toXML(null);
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testToXMLNotExtensive() {
        final Element elem = this.nonExtensiveFactory.toXML(this.template);
        Assert.assertEquals(this.id, elem.getAttributeValue(IdentifiableFactory.TAG_ID));
        Assert.assertEquals(this.name, elem.getAttributeValue(VMTemplateFactory.ATTRIBUTE_NAME));
    }

    @Test
    public void testToXMLNotExtensiveNoProfile() {
        final Element elem = this.nonExtensiveFactory.toXML(this.template);
        Assert.assertNull(elem.getChild(VMTemplateFactory.TAG_LOADPROFILE));
    }

    @Test
    public void testToXMLExtensive() {
        final Element elem = this.extensiveFactory.toXML(this.template);
        Assert.assertEquals(this.id, elem.getAttributeValue(IdentifiableFactory.TAG_ID));
        Assert.assertEquals(this.name, elem.getAttributeValue(VMTemplateFactory.ATTRIBUTE_NAME));
    }

    @Test
    public void testToXMLExtensiveHasLoadProfile() {
        final Element elem = this.extensiveFactory.toXML(this.template);
        Assert.assertNotNull(elem.getChild(VMTemplateFactory.TAG_LOADPROFILE));
    }

    @Test
    public void testToXMLExtensiveHasFilledInLoadProfile() {
        final Element elem = this.extensiveFactory.toXML(this.template);
        final Element loadProfile = elem.getChild(VMTemplateFactory.TAG_LOADPROFILE);
        Assert.assertEquals(this.cpu, Integer.parseInt(loadProfile
                .getChildTextTrim(VMTemplateFactory.TAG_CPU)));
        Assert.assertEquals(this.mem, Integer.parseInt(loadProfile
                .getChildTextTrim(VMTemplateFactory.TAG_MEM)));
        Assert.assertEquals(this.io, Integer.parseInt(loadProfile
                .getChildTextTrim(VMTemplateFactory.TAG_IO)));
        Assert.assertEquals(this.net, Integer.parseInt(loadProfile
                .getChildTextTrim(VMTemplateFactory.TAG_NET)));
    }
}
