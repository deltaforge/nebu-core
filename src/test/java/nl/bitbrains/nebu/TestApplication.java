package nl.bitbrains.nebu;

import java.util.ArrayList;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.ApplicationBuilder;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentBuilder;
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
public class TestApplication {

    final String appID = "id";
    final String otherId = "other";
    final String name = "name";

    Application app;
    Application copyApp;
    Application otherApp;

    @Before
    public void setUp() {
        this.app = new ApplicationBuilder().withUuid(this.appID).withName(this.name).build();
        this.copyApp = new ApplicationBuilder().withUuid(this.appID).withName(this.name).build();
        this.otherApp = new ApplicationBuilder().withUuid(this.otherId).withName(this.name)
                .build();
    }

    @Test
    public void testConstructor() {
        Assert.assertEquals(this.appID, this.app.getUniqueIdentifier());
        Assert.assertEquals(this.name, this.app.getName());
    }

    @Test
    public void testIDIsMandatory() {
        boolean caught = false;
        try {
            new ApplicationBuilder().build();
        } catch (final IllegalStateException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testSetGetName() {
        final String newName = "newName";
        this.app.setName(newName);
        Assert.assertEquals(newName, this.app.getName());
    }

    @Test
    public void testSetVMTemplatesInBuilder() {
        final int numTemp = 4;
        final List<VMTemplate> templates = new ArrayList<VMTemplate>();
        for (int i = 0; i < numTemp; i++) {
            final VMTemplate template = new VMTemplateBuilder().withUuid("template" + i).build();
            templates.add(template);
        }
        final Application app = new ApplicationBuilder().withUuid(this.appID)
                .withTemplates(templates).build();
        Assert.assertEquals(numTemp, app.getVMTemplates().size());
    }

    @Test
    public void testSetDeploymentsInBuilder() {
        final int numDep = 4;
        final List<Deployment> deployments = new ArrayList<Deployment>();
        for (int i = 0; i < numDep; i++) {
            final Deployment deployment = new DeploymentBuilder().withUuid("deployment" + i)
                    .build();
            deployments.add(deployment);
        }
        final Application app = new ApplicationBuilder().withUuid(this.appID)
                .withDeployments(deployments).build();
        Assert.assertEquals(numDep, app.getDeployments().size());
    }

    @Test
    public void testPutGetVMTemplate() {
        final String id = "vmTemplateID";
        final VMTemplate template = new VMTemplateBuilder().withUuid(id).build();
        this.app.putVMTemplate(template);
        Assert.assertEquals(template, this.app.getVMTemplate(id));
        Assert.assertTrue(this.app.getVMTemplates().contains(template));
    }

    @Test
    public void testPutGetDeployment() {
        final String id = "vmTemplateID";
        final Deployment dep = new DeploymentBuilder().withUuid(id).build();
        this.app.putDeployment(dep);
        Assert.assertEquals(dep, this.app.getDeployment(id));
        Assert.assertTrue(this.app.getDeployments().contains(dep));
    }

    @SuppressWarnings("unused")
    private Object[] equalsParams() {
        this.setUp();
        return JUnitParamsRunner.$(JUnitParamsRunner.$(this.app, null, false),
                                   JUnitParamsRunner.$(this.app, this.copyApp, true),
                                   JUnitParamsRunner.$(this.copyApp, this.app, true),
                                   JUnitParamsRunner.$(this.app, this.otherApp, false),
                                   JUnitParamsRunner.$(this.app, 1, false),
                                   JUnitParamsRunner.$(this.app,
                                                       this.app.getUniqueIdentifier(),
                                                       false));
    }

    @SuppressWarnings("unused")
    private Object[] hashCodeParams() {
        this.setUp();
        return JUnitParamsRunner.$(JUnitParamsRunner.$(this.app, this.copyApp, true),
                                   JUnitParamsRunner.$(this.app, this.app, true),
                                   JUnitParamsRunner.$(this.app, this.otherApp, false));
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

}
