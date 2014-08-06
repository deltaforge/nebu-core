package nl.bitbrains.nebu;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import nl.bitbrains.nebu.containers.VMTemplate;
import nl.bitbrains.nebu.containers.VMTemplateBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@RunWith(JUnitParamsRunner.class)
public class TestVMTemplate {

    final String templateID = "id";
    final String otherId = "other";
    final String name = "name";

    VMTemplate template;
    VMTemplate copyTemplate;
    VMTemplate otherTemplate;

    @Before
    public void setUp() {
        this.template = new VMTemplateBuilder().withUuid(this.templateID).withName(this.name)
                .build();
        this.copyTemplate = new VMTemplateBuilder().withUuid(this.templateID).withName(this.name)
                .build();
        this.otherTemplate = new VMTemplateBuilder().withUuid(this.otherId).withName(this.name)
                .build();
    }

    @SuppressWarnings("unused")
    private Object[] equalsParams() {
        this.setUp();
        return JUnitParamsRunner.$(JUnitParamsRunner.$(this.template, null, false),
                                   JUnitParamsRunner.$(this.template, this.copyTemplate, true),
                                   JUnitParamsRunner.$(this.copyTemplate, this.template, true),
                                   JUnitParamsRunner.$(this.template, this.otherTemplate, false),
                                   JUnitParamsRunner.$(this.template, 1, false),
                                   JUnitParamsRunner.$(this.template,
                                                       this.template.getUniqueIdentifier(),
                                                       false));
    }

    @SuppressWarnings("unused")
    private Object[] hashCodeParams() {
        this.setUp();
        return JUnitParamsRunner.$(JUnitParamsRunner.$(this.template, this.copyTemplate, true),
                                   JUnitParamsRunner.$(this.template, this.template, true),
                                   JUnitParamsRunner.$(this.template, this.otherTemplate, false));
    }

    @Test
    @Parameters(method = "equalsParams")
    public void equalsTest(final Object a, final Object b, final boolean result) {
        if (result) {
            Assert.assertEquals(a, b);
        } else {
            Assert.assertNotEquals(a, b);
        }
    }

    @Test
    @Parameters(method = "hashCodeParams")
    public void hashCodeTest(final Object a, final Object b, final boolean result) {
        if (result) {
            Assert.assertEquals(a.hashCode(), b.hashCode());
        } else {
            Assert.assertNotEquals(a.hashCode(), b.hashCode());
        }
    }

    @Test
    public void testSetGetCpu() {
        final int num = 20;
        this.template.setCpu(num);
        Assert.assertEquals(num, this.template.getCpu());
    }

    @Test
    public void testGetCpuBuilder() {
        final int num = 20;
        final VMTemplate tmp = new VMTemplateBuilder().withUuid(this.templateID).withCPU(num)
                .build();
        Assert.assertEquals(num, tmp.getCpu());
    }

    @Test
    public void testSetGetMem() {
        final int num = 20;
        this.template.setMem(num);
        Assert.assertEquals(num, this.template.getMem());
    }

    @Test
    public void testGetMemBuilder() {
        final int num = 20;
        final VMTemplate tmp = new VMTemplateBuilder().withUuid(this.templateID).withMem(num)
                .build();
        Assert.assertEquals(num, tmp.getMem());
    }

    @Test
    public void testSetGetIO() {
        final int num = 20;
        this.template.setIo(num);
        Assert.assertEquals(num, this.template.getIo());
    }

    @Test
    public void testGetIoBuilder() {
        final int num = 20;
        final VMTemplate tmp = new VMTemplateBuilder().withUuid(this.templateID).withIO(num)
                .build();
        Assert.assertEquals(num, tmp.getIo());
    }

    @Test
    public void testSetGetNet() {
        final int num = 20;
        this.template.setNet(num);
        Assert.assertEquals(num, this.template.getNet());
    }

    @Test
    public void testGetNetBuilder() {
        final int num = 20;
        final VMTemplate tmp = new VMTemplateBuilder().withUuid(this.templateID).withNet(num)
                .build();
        Assert.assertEquals(num, tmp.getNet());
    }

    @Test
    public void testSetGetName() {
        this.template.setName(this.templateID);
        Assert.assertEquals(this.templateID, this.template.getName());
    }

}
