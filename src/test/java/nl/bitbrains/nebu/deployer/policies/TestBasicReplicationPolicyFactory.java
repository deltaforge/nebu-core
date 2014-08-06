package nl.bitbrains.nebu.deployer.policies;

import java.text.ParseException;

import nl.bitbrains.nebu.deployer.policies.BasicReplicationPolicyFactory;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class TestBasicReplicationPolicyFactory {

    private BasicReplicationPolicyFactory factory;

    @Before
    public void setUp() {
        this.factory = new BasicReplicationPolicyFactory();
    }

    @Test
    public void testGetInstance() {
        Assert.assertNotNull(this.factory.newInstance());
    }

    @Test
    public void testFromXML() throws ParseException {
        final Element xml = Mockito.mock(Element.class);
        Assert.assertNotNull(this.factory.fromXML(xml));
        Mockito.verifyZeroInteractions(xml);
    }

}
