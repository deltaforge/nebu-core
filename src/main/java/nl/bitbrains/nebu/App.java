package nl.bitbrains.nebu;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.config.Configuration;
import nl.bitbrains.nebu.common.config.InvalidConfigurationException;
import nl.bitbrains.nebu.deployer.Deployer;
import nl.bitbrains.nebu.deployer.DeployerPolicyFactories;
import nl.bitbrains.nebu.deployer.policies.AggregatedLocalityPolicy;
import nl.bitbrains.nebu.deployer.policies.AggregatedLocalityPolicyFactory;
import nl.bitbrains.nebu.deployer.policies.BasicReplicationPolicy;
import nl.bitbrains.nebu.deployer.policies.BasicReplicationPolicyFactory;
import nl.bitbrains.nebu.deployer.policies.HDFS2ReplicationPolicy;
import nl.bitbrains.nebu.deployer.policies.HDFS2ReplicationPolicyFactory;
import nl.bitbrains.nebu.deployer.policies.HDFSReplicationPolicy;
import nl.bitbrains.nebu.deployer.policies.HDFSReplicationPolicyFactory;
import nl.bitbrains.nebu.deployer.policies.LocalityPolicy;
import nl.bitbrains.nebu.deployer.policies.LocalityPolicyFactory;
import nl.bitbrains.nebu.deployer.policies.RandomPolicy;
import nl.bitbrains.nebu.deployer.policies.RandomPolicyFactory;
import nl.bitbrains.nebu.rest.RESTRequestException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.research.ws.wadl.Application;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 *         Class contains a main for debugging.
 */
public final class App extends Application {

    private static Logger logger = LogManager.getLogger();

    class CORSFilter implements ContainerResponseFilter {
        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.ws.rs.container.ContainerResponseFilter#filter(javax.ws.rs.
         * container.ContainerRequestContext,
         * javax.ws.rs.container.ContainerResponseContext)
         */
        @Override
        public void filter(final ContainerRequestContext requestContext,
                final ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        }
    }

    /**
     * Default empty constructor.
     */
    public App() {

    }

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
     * application.
     * 
     * @return Grizzly HTTP server.
     */
    private static HttpServer startServer(final int port) {
        // create a resource config that scans for JAX-RS resources and
        // providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig().packages("nl.bitbrains.nebu.rest.server");
        rc.register(App.CORSFilter.class);
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create("http://0.0.0.0:"
                                                                 + Integer.toString(port)),
                                                         rc);
    }

    /**
     * @return a new {@link Deployer} with a list of default policies set.
     */
    private static Deployer initializeDeployer() {
        final DeployerPolicyFactories factories = new DeployerPolicyFactories()
                .withFactory(RandomPolicy.POLICY_NAME, new RandomPolicyFactory())
                .withFactory(BasicReplicationPolicy.POLICY_NAME,
                             new BasicReplicationPolicyFactory())
                .withFactory(LocalityPolicy.POLICY_NAME, new LocalityPolicyFactory())
                .withFactory(HDFSReplicationPolicy.POLICY_NAME, new HDFSReplicationPolicyFactory())
                .withFactory(HDFS2ReplicationPolicy.POLICY_NAME,
                             new HDFS2ReplicationPolicyFactory())
                .withFactory(AggregatedLocalityPolicy.POLICY_NAME,
                             new AggregatedLocalityPolicyFactory());
        return new Deployer().withPolicyFactories(factories);
    }

    /**
     * Main used for debugging.
     * 
     * @param args
     *            Not used.
     * @throws RESTRequestException
     *             for http errors.
     * @throws IOException
     *             if config parsing fails.
     * @throws InvalidConfigurationException
     *             if the config is invalidly specified.
     */
    public static void main(final String[] args) throws RESTRequestException, IOException,
            InvalidConfigurationException {
        final Configuration config = Configuration.parseConfigurationFile(new File(args[0]));
        App.logger.info("Starting Server");
        final HttpServer server = App.startServer(config.getServerConfig().getPort());

        CacheManager.put(Deployer.CACHE_KEY, App.initializeDeployer());

        System.in.read();
        server.stop();
        App.logger.info("Server Terminated");
    }
}
