package nl.bitbrains.nebu.rest;

import nl.bitbrains.nebu.rest.RESTRequestException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Simple test to check the {@link RESTRequestException}
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class TestRESTRequestException {

    @Test
    public void test() {
        final int code = 200;
        final String message = "message";
        final RESTRequestException e = new RESTRequestException(message, code);
        Assert.assertEquals(code, e.getHttpCode());
        Assert.assertTrue(e.toString().contains(message));
    }
}
