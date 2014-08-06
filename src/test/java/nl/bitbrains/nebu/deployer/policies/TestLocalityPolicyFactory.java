package nl.bitbrains.nebu.deployer.policies;

import java.text.ParseException;

import nl.bitbrains.nebu.deployer.policies.LocalityPolicy;
import nl.bitbrains.nebu.deployer.policies.LocalityPolicyFactory;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class TestLocalityPolicyFactory {

    private LocalityPolicyFactory factory;

    @Before
    public void setUp() {
        this.factory = new LocalityPolicyFactory();
    }

    @Test
    public void testGetInstance() {
        Assert.assertNotNull(this.factory.newInstance());
    }

    @Test
    public void testFromXMLNoSettingContent() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        final LocalityPolicy policy = this.factory.fromXML(xml);
        Assert.assertNotNull(policy);
        Assert.assertEquals(LocalityPolicy.DEFAULT_MAX_VMS_PER_HOST, policy.getMaxVMsPerHost());

    }

    @Test
    public void testFromXMLNoSettingsXMLInteractions() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        this.factory.fromXML(xml);
        Mockito.verify(xml, Mockito.times(1)).getChild(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST);
        Mockito.verifyNoMoreInteractions(xml);
    }

    @Test
    public void testFromXMLWithSettingsContent() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        final Element elem = Mockito.mock(Element.class);
        final String string = "12";
        Mockito.when(xml.getChild(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST)).thenReturn(elem);
        Mockito.when(xml.getChildTextTrim(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST))
                .thenReturn(string);

        final LocalityPolicy policy = this.factory.fromXML(xml);
        Assert.assertNotNull(policy);
        Assert.assertEquals(Integer.parseInt(string), policy.getMaxVMsPerHost());
    }

    @Test
    public void testFromXMLWithSettingsXMLInteractions() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        final Element elem = Mockito.mock(Element.class);
        final String string = "12";
        Mockito.when(xml.getChild(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST)).thenReturn(elem);
        Mockito.when(xml.getChildTextTrim(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST))
                .thenReturn(string);

        final LocalityPolicy policy = this.factory.fromXML(xml);
        Mockito.verify(xml, Mockito.times(1)).getChild(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST);
        Mockito.verify(xml, Mockito.times(1))
                .getChildTextTrim(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST);
        Mockito.verifyNoMoreInteractions(xml);
    }

}
