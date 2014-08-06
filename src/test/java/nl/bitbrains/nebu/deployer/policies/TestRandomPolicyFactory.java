package nl.bitbrains.nebu.deployer.policies;

import java.text.ParseException;

import nl.bitbrains.nebu.deployer.policies.RandomPolicy;
import nl.bitbrains.nebu.deployer.policies.RandomPolicyFactory;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class TestRandomPolicyFactory {

    private RandomPolicyFactory factory;

    @Before
    public void setUp() {
        this.factory = new RandomPolicyFactory();
    }

    @Test
    public void testGetInstance() {
        Assert.assertNotNull(this.factory.newInstance());
    }

    @Test
    public void testFromXMLNoSeed() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        Assert.assertNotNull(this.factory.fromXML(xml));
        Mockito.verify(xml).getChild(RandomPolicyFactory.TAG_SEED);
        Mockito.verify(xml, Mockito.times(0)).getChildTextTrim(RandomPolicyFactory.TAG_SEED);
    }

    @Test
    public void testFromXMLWithSeedXML() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        final Element elem = Mockito.mock(Element.class);
        final String seed = "1346";
        Mockito.when(xml.getChild(RandomPolicyFactory.TAG_SEED)).thenReturn(elem);
        Mockito.when(xml.getChildTextTrim(RandomPolicyFactory.TAG_SEED)).thenReturn(seed);
        this.factory.fromXML(xml);
        Mockito.verify(xml).getChild(RandomPolicyFactory.TAG_SEED);
        Mockito.verify(xml).getChildTextTrim(RandomPolicyFactory.TAG_SEED);
    }

    @Test
    public void testFromXMLWithSeedContent() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        final Element elem = Mockito.mock(Element.class);
        final String seed = "1346";
        Mockito.when(xml.getChild(RandomPolicyFactory.TAG_SEED)).thenReturn(elem);
        Mockito.when(xml.getChildTextTrim(RandomPolicyFactory.TAG_SEED)).thenReturn(seed);
        final RandomPolicy policy = (RandomPolicy) this.factory.fromXML(xml);
        Assert.assertNotNull(policy);
        Assert.assertEquals(Long.parseLong(seed), policy.getSeed());
    }

}
