package nl.bitbrains.nebu.rest.client;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 *         Enumeration that determines the behaviour of the servers used in unit
 *         tests.
 */
public enum TestServerStatus {
    OK, INTERNAL_SERVER_ERROR, PAGE_NOT_FOUND, WRONG_RESPONSE_TYPE
}
