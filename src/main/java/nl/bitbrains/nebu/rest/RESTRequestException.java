package nl.bitbrains.nebu.rest;

import nl.bitbrains.nebu.common.cache.CacheException;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 *         Exception for REST requests made.
 */
public class RESTRequestException extends CacheException {

    private static final long serialVersionUID = -6710682055881172325L;
    private final int httpCode;

    /**
     * Exception for REST requests.
     * 
     * @param message
     *            describes the eror.
     * @param errorCode
     *            the HTTP status code.
     */
    public RESTRequestException(final String message, final int errorCode) {
        super(message);
        this.httpCode = errorCode;
    }

    /**
     * Exception for REST requests.
     * 
     * @param message
     *            describes the eror.
     * @param errorCode
     *            the HTTP status code.
     * @param e
     *            {@link Throwable} this exception is based on.
     */
    public RESTRequestException(final String message, final int errorCode, final Throwable e) {
        super(message, e);
        this.httpCode = errorCode;
    }

    /**
     * @return the httpCode
     */
    public final int getHttpCode() {
        return this.httpCode;
    }

    @Override
    public final String toString() {
        return super.toString() + " errorCode: " + this.getHttpCode();
    }
}
