package nl.bitbrains.nebu.containers;

import java.util.HashMap;
import java.util.Map;

import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.common.util.UUIDGenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Container class representing a request for a {@link Deployment} of Virtual
 * Machines.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public final class DeploymentRequest implements Identifiable {

    public static final String UUID_PREFIX = "deployment";
    private static Logger logger = LogManager.getLogger();
    private final String uniqueIdentifier;
    private final Map<VMTemplate, Integer> requests;
    private final Application application;

    /**
     * @param uuid
     *            the UUID of the new DeploymentRequest.
     * @param application
     *            the {@link Application} in which this DeploymentRequest is
     *            made.
     * @param requests
     *            to set.
     */
    private DeploymentRequest(final String uuid, final Application application,
            final Map<VMTemplate, Integer> requests) {
        DeploymentRequest.logger.entry();
        this.uniqueIdentifier = uuid;
        this.application = application;
        this.requests = requests;
        DeploymentRequest.logger.exit();
    }

    /**
     * @return the UUID of this DeploymentRequest.
     */
    public String getUniqueIdentifier() {
        return this.uniqueIdentifier;
    }

    /**
     * @return the {@link Application} for which this DeploymentRequest was
     *         made.
     */
    public Application getApplication() {
        return this.application;
    }

    /**
     * @return a list of requests (number of requested VMs per
     *         {@link VMTemplate}).
     */
    public Map<VMTemplate, Integer> getTemplateRequests() {
        return new HashMap<VMTemplate, Integer>(this.requests);
    }

    /**
     * Adds a request for a number of VirtualMachines of a specific
     * {@link VMTemplate}. Overwrites a previously added request for the given
     * {@link VMTemplate} if it exists.
     * 
     * @param templateUUID
     *            the UUID of the template to request.
     * @param count
     *            the amount of VirtualMachines to request.
     * @throws IllegalArgumentException
     *             iff the given UUID does not represent a known
     *             {@link VMTemplate}.
     */
    public void addRequest(final String templateUUID, final Integer count) {
        final VMTemplate vmTemplate = this.application.getVMTemplate(templateUUID);
        if (vmTemplate == null) {
            throw new IllegalArgumentException("VMTemplate not found for id '" + templateUUID + "'");
        } else {
            this.requests.put(vmTemplate, count);
        }
    }

    /**
     * Helper class to create {@link DeploymentRequest} objects.
     * 
     * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
     * 
     */
    public static class Builder {

        private String uniqueIdentifier;
        private Application application;
        private final Map<VMTemplate, Integer> requests;

        /**
         * Creates a new {@link DeploymentRequest} builder. Generates a random
         * unique identifier.
         */
        public Builder() {
            this.application = null;
            this.requests = new HashMap<VMTemplate, Integer>();

            this.uniqueIdentifier = UUIDGenerator.generate(DeploymentRequest.UUID_PREFIX);
        }

        /**
         * @param uniqueIdentifier
         *            the unique identifier for the new
         *            {@link DeploymentRequest}.
         * @return this for fluency.
         */
        public Builder withUniqueIdentifier(final String uniqueIdentifier) {
            ErrorChecker.throwIfNullArgument(uniqueIdentifier, "uniqueIdentifier");

            this.uniqueIdentifier = uniqueIdentifier;
            return this;
        }

        /**
         * @param application
         *            the {@link Application} for which the
         *            {@link DeploymentRequest} is made.
         * @return this.
         */
        public Builder withApplication(final Application application) {
            ErrorChecker.throwIfNullArgument(application, "application");

            this.application = application;
            return this;
        }

        /**
         * @param requestTemplate
         *            a {@link VMTemplate} to use for a VM request.
         * @param requestTimes
         *            the number of machines to launch for the given template.
         * @return this.
         */
        public Builder withRequest(final VMTemplate requestTemplate, final Integer requestTimes) {
            ErrorChecker.throwIfNullArgument(requestTemplate, "requestTemplate");

            this.requests.put(requestTemplate, requestTimes);
            return this;
        }

        /**
         * @param requests
         *            a map of {@link VMTemplate}s to the number of machines to
         *            launch using each of these templates.
         * @return this.
         */
        public Builder withRequests(final Map<VMTemplate, Integer> requests) {
            ErrorChecker.throwIfNullArgument(requests, "requests");

            for (final Map.Entry<VMTemplate, Integer> request : requests.entrySet()) {
                this.withRequest(request.getKey(), request.getValue());
            }
            return this;
        }

        /**
         * Creates a new {@link DeploymentRequest} using the values set through
         * other functions of the Builder. Requires
         * {@link Builder#withApplication(Application) Application} to be set.
         * 
         * @return a new {@link DeploymentRequest}.
         */
        public DeploymentRequest createDeploymentRequest() {
            ErrorChecker.throwIfNotSet(this.application, "application");
            return new DeploymentRequest(this.uniqueIdentifier, this.application, this.requests);
        }
    }

}
