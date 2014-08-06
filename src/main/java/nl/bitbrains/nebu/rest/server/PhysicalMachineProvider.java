package nl.bitbrains.nebu.rest.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.topology.PhysicalDataCenter;
import nl.bitbrains.nebu.common.topology.PhysicalDataCenterBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalHostBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalRack;
import nl.bitbrains.nebu.common.topology.PhysicalRackBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalStoreBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactories;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.VMTemplate;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * Provides virtual machine information to clients.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class PhysicalMachineProvider {

    public static final String PATH = "/phys";
    private static Logger logger = LogManager.getLogger();
    private final Application app;
    private final boolean isReal;

    /**
     * Public constructor needed by Jersey.
     * 
     * @param app
     *            to get the topology for.
     * @param isReal
     *            true iff real topology should be sent.
     */
    public PhysicalMachineProvider(final Application app, final boolean isReal) {
        this.app = app;
        this.isReal = isReal;
    }

    /**
     * Returns a list of all virtual machine uuids known to the virtual machine
     * manager.
     * 
     * @return A list of virtual machine uuids.
     * @throws JDOMException
     *             if the xml can not be converted.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public final Response getPhysicalResources() throws JDOMException {
        PhysicalMachineProvider.logger.entry();
        Response rep = null;
        rep = null;

        final List<VirtualMachine> vms = this.getAllVMsFromApp();

        if (this.isReal) {
            rep = this.getRealResponse(vms);
        } else {
            rep = this.getMockedResponse(vms);
        }
        return rep;
    }

    /**
     * Creates a mocked topology with each vm in a different dc, with one rack,
     * one host and one disk.
     * 
     * @param vms
     *            to place in the topology.
     * @return the mocked topology.
     * @throws JDOMException
     *             if parsing fails.
     */
    public final Response getMockedResponse(final List<VirtualMachine> vms) throws JDOMException {
        final PhysicalTopology topo = new PhysicalTopology();
        for (final VirtualMachine vm : vms) {
            final String id = vm.getUniqueIdentifier();
            final PhysicalHost h = new PhysicalHostBuilder().withUuid("host-" + id).build();
            final PhysicalStore s = new PhysicalStoreBuilder().withUuid("store-" + id).build();
            final PhysicalRack r = new PhysicalRackBuilder().withUuid("rack-" + id).build();
            final PhysicalDataCenter dc = new PhysicalDataCenterBuilder().withUuid("dc-" + id)
                    .build();
            topo.addDataCenter(dc);
            topo.addRackToDataCenter(r, dc);
            topo.addCPUToRack(h, r);
            topo.addDiskToRack(s, r);
        }
        final TopologyFactories factories = TopologyFactories.createDefault();
        final Element xml = factories.getPhysicalRootFactory().toXML(topo.getRoot());
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(xml);
        return Response.ok(doc, MediaType.APPLICATION_XML).build();
    }

    /**
     * Gets the real topology from vmware filtered for the vms.
     * 
     * @param vms
     *            to filter it for.
     * @return the topology.
     * @throws JDOMException
     *             if parsing fails.
     */
    public final Response getRealResponse(final List<VirtualMachine> vms) throws JDOMException {
        final Set<String> hosts = this.getAllHostsFromVMs(vms);
        final Set<String> stores = this.getAllStoresFromVMs(vms);
        PhysicalTopology fullTopology;
        try {
            fullTopology = this.getFullTopology();
        } catch (final CacheException e) {
            PhysicalMachineProvider.logger.catching(Level.WARN, e);
            return PhysicalMachineProvider.logger.exit(Response
                    .status(Status.INTERNAL_SERVER_ERROR).build());
        }

        final PhysicalTopology topology = this.filterTopology(fullTopology, hosts, stores);
        final TopologyFactories factories = TopologyFactories.createDefault();
        final Element xml = factories.getPhysicalRootFactory().toXML(topology.getRoot());
        final Document doc = XMLConverter.convertJDOMElementW3CDocument(xml);
        return Response.ok(doc, MediaType.APPLICATION_XML).build();
    }

    /**
     * Removes all Hosts and Disks that are not in the specified sets in a new
     * topology object based on the full topology.
     * 
     * @param fullTopology
     *            to filter out of.
     * @param hosts
     *            to keep in the tree.
     * @param stores
     *            to keep in the tree.
     * @return the filtered topology.
     */
    private PhysicalTopology filterTopology(final PhysicalTopology fullTopology,
            final Set<String> hosts, final Set<String> stores) {
        PhysicalMachineProvider.logger.entry();
        final Set<PhysicalHost> filteredHosts = this
                .getFilteredItems(fullTopology.getCPUs(), hosts);
        final Set<PhysicalStore> filteredStores = this.getFilteredItems(fullTopology.getStores(),
                                                                        stores);
        final PhysicalTopology result = new PhysicalTopology(fullTopology);

        // Remove unwanted hosts.
        for (final PhysicalHost host : result.getCPUs()) {
            final List<PhysicalStore> hostStores = host.getDisks();
            // First remove unwanted disks from the rack.
            for (final PhysicalStore hostStore : hostStores) {
                if (!filteredStores.contains(hostStore)) {
                    result.removeDiskFromHost(hostStore, host);
                }
            }
            if (!filteredHosts.contains(host) && host.getDisks().isEmpty()) {
                result.removeCPUFromRack(host, host.getParent());
            }
        }

        // Remove unwanted Racks
        for (final PhysicalRack rack : result.getRacks()) {
            final List<PhysicalStore> rackStores = rack.getDisks();
            // First remove unwanted disks from the rack.
            for (final PhysicalStore rackStore : rackStores) {
                if (!filteredStores.contains(rackStore)) {
                    result.removeDiskFromRack(rackStore, rack);
                }
            }
            if (rack.getCPUs().isEmpty() && rack.getDisks().isEmpty()) {
                result.removeRackFromDataCenter(rack, rack.getParent());
            }
        }

        // Remove unwanted datacentres
        for (final PhysicalDataCenter dc : result.getDataCenters()) {
            if (dc.getRacks().isEmpty()) {
                result.removeDataCenter(dc);
            }
        }

        return PhysicalMachineProvider.logger.exit(result);
    }

    /**
     * @param fullList
     *            to get the items out of.
     * @param ids
     *            ids of the items.
     * @param <T>
     *            type of the item.
     * @return the items
     */
    private <T extends Identifiable> Set<T> getFilteredItems(final List<T> fullList,
            final Set<String> ids) {
        final Set<T> filteredItem = new HashSet<T>();
        for (final T item : fullList) {
            if (ids.contains(item.getUniqueIdentifier())) {
                filteredItem.add(item);
            }
        }
        return filteredItem;
    }

    /**
     * @return the vms of this app.
     */
    private List<VirtualMachine> getAllVMsFromApp() {
        final List<VirtualMachine> result = new ArrayList<VirtualMachine>();
        for (final Deployment deployment : this.app.getDeployments()) {
            deployment.refreshVMInformation();
            result.addAll(deployment.getVirtualMachines(true));
        }
        return result;
    }

    /**
     * @param vms
     *            to get the hosts for.
     * @return the hosts in a set.
     */
    private Set<String> getAllHostsFromVMs(final List<VirtualMachine> vms) {
        final Set<String> result = new HashSet<String>();
        for (final VirtualMachine vm : vms) {
            result.add(vm.getHost());
        }
        return result;
    }

    /**
     * @param vms
     *            to get the stores for.
     * @return the stores in a set.
     */
    private Set<String> getAllStoresFromVMs(final List<VirtualMachine> vms) {
        final Set<String> result = new HashSet<String>();
        for (final VirtualMachine vm : vms) {
            result.addAll(vm.getStores());
        }
        return result;
    }

    /**
     * Gets all topologies for all templates and merges them into the full
     * topology.
     * 
     * @return the full topology.
     * @throws CacheException
     *             if a REST request fails.
     */
    private PhysicalTopology getFullTopology() throws CacheException {
        PhysicalTopology result = null;
        for (final VMTemplate template : this.app.getVMTemplates()) {
            if (result == null) {
                result = template.getTopology();
            } else {
                result = PhysicalTopology.mergeTree(result, template.getTopology());
            }
        }
        if (result == null) {
            throw new CacheException("No templates in this app");
        }
        return result;
    }

}
