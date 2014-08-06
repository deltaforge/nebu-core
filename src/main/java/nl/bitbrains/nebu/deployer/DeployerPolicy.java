package nl.bitbrains.nebu.deployer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalRack;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.containers.Application;
import nl.bitbrains.nebu.containers.Deployment;
import nl.bitbrains.nebu.containers.DeploymentRequest;
import nl.bitbrains.nebu.containers.VMDeploymentSpecification;
import nl.bitbrains.nebu.containers.VMTemplate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract definition of a policy for the Deployer. A concrete subclass of
 * DeployerPolicy is responsible for allocating {@link VMTemplate}s to
 * {@link PhysicalCPU}s, based on a {@link DeploymentRequest}.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public abstract class DeployerPolicy {

    private static final double MAX_SUITABLE_STORAGE_PERCENTAGE = 0.6;
    private static Logger logger = LogManager.getLogger();
    private Map<PhysicalRack, Integer> vmsPerRack;
    private Map<String, Integer> vmsPerHost;
    private Map<String, Integer> vmsPerStore;

    /**
     * @param request
     *            a {@link DeploymentRequest} to generate the deployment for.
     * @return a set of allocations of {@link VMTemplate}s to @{link
     *         PhysicalCPU}s.
     * @throws DeployerException
     *             when vital data is missing or no valid deployment can be
     *             found.
     */
    public abstract List<VMDeploymentSpecification> generateDeployment(
            final DeploymentRequest request) throws DeployerException;

    /**
     * @return the vmsPerRack
     */
    public final Map<PhysicalRack, Integer> getVmsPerRack() {
        return this.vmsPerRack;
    }

    /**
     * @return the vmsPerHost
     */
    public final Map<String, Integer> getVmsPerHost() {
        return this.vmsPerHost;
    }

    /**
     * @return the vmsPerStore
     */
    public final Map<String, Integer> getVmsPerStore() {
        return this.vmsPerStore;
    }

    /**
     * @param template
     *            the {@link VMTemplate} to retrieve a topology for.
     * @return the {@link PhysicalTopology} accessible to VMs launched with the
     *         given {@link VMTemplate}.
     * @throws DeployerException
     *             if getting the topoly fails.
     */
    protected static PhysicalTopology retrieveTopology(final VMTemplate template)
            throws DeployerException {
        DeployerPolicy.logger.entry();
        PhysicalTopology topology;
        try {
            topology = template.getTopology();
        } catch (final CacheException ex) {
            throw DeployerPolicy.logger.throwing(new DeployerException(
                    "Could not retrieve topology for VMTemplate '" + template.getUniqueIdentifier()
                            + "'", ex));
        }
        if (topology.getCPUs().isEmpty()) {
            throw DeployerPolicy.logger.throwing(new DeployerException(
                    "No CPUs to deploy template (" + template.getUniqueIdentifier() + ") on."));
        }
        return DeployerPolicy.logger.exit(topology);
    }

    /**
     * Initializes the VMPerX counters based on the topology.
     * 
     * @param topology
     *            to get the hosts, racks and stores from.
     */
    protected void initializeVmCounters(final PhysicalTopology topology) {
        this.vmsPerRack = this.newVmsPerRack(topology.getRacks());
        this.vmsPerHost = this.newVmsPerHost(topology.getCPUs());
        this.vmsPerStore = this.newVmsPerStore(topology.getStores());
    }

    /**
     * Updates the vmCounters based on the application, for only the specific
     * template and the given topology.
     * 
     * @param app
     *            to get the vms out of.
     * @param template
     *            to match the vms against.
     * @param topology
     *            to get the cpus out of (for updating the rack counter)
     */
    protected void updateVMCountersForApplicationAndTemplate(final Application app,
            final VMTemplate template, final PhysicalTopology topology) {
        this.updateVmsPerHostForExistingVMs(this.vmsPerHost, app, template);
        this.updateVmsPerStoreForExistingVMs(this.vmsPerStore, app, template);
        this.updateVmsPerRackUsingHosts(this.vmsPerRack, this.vmsPerHost, topology.getCPUs());
        DeployerPolicy.logger.debug(this.vmsPerRack);
        DeployerPolicy.logger.debug(this.vmsPerHost);
        DeployerPolicy.logger.debug(this.vmsPerStore);
    }

    /**
     * @return the least used PhysicalRack.
     */
    protected PhysicalRack getLeastUsedRack() {
        return this.getLeastUsed(this.vmsPerRack.keySet(), this.vmsPerRack);
    }

    /**
     * @param list
     *            to get the host out of.
     * @return the least used host.
     */
    protected PhysicalHost getLeastUsedHost(final List<PhysicalHost> list) {
        return this.getLeastUsedString(list, this.vmsPerHost);
    }

    /**
     * @param list
     *            to get the store out of.
     * @return the least used store.
     * 
     */
    protected PhysicalStore getLeastUsedStore(final List<PhysicalStore> list) {
        return this.getLeastUsedString(list, this.vmsPerStore);
    }

    /**
     * Updates te VMperHost and perRack counters based on the newly chosen host.
     * 
     * @param host
     *            that was chosen.
     */
    protected void updateVMCountersWithNewlyChosenHost(final PhysicalHost host) {
        this.vmsPerHost.put(host.getUniqueIdentifier(),
                            this.vmsPerHost.get(host.getUniqueIdentifier()) + 1);
        this.vmsPerRack.put(host.getParent(), this.vmsPerRack.get(host.getParent()) + 1);
    }

    /**
     * Updates te VMperHost, perStore and perRack counters based on the newly
     * chosen host and store. *
     * 
     * @param host
     *            that was chosen.
     * @param store
     *            that was chosen.
     */
    protected void updateVMCountersWithNewlyChosenHostAndStore(final PhysicalHost host,
            final PhysicalStore store) {
        this.updateVMCountersWithNewlyChosenHost(host);
        this.vmsPerStore.put(store.getUniqueIdentifier(),
                             this.vmsPerStore.get(store.getUniqueIdentifier()) + 1);
    }

    /**
     * @param stores
     *            to initialize.
     * @return a new Map.
     */
    protected final Map<String, Integer> newVmsPerStore(final List<PhysicalStore> stores) {
        final Map<String, Integer> vmsPerStore = new HashMap<String, Integer>();
        for (final PhysicalStore store : stores) {
            vmsPerStore.put(store.getUniqueIdentifier(), 0);
        }
        return vmsPerStore;
    }

    /**
     * @param cpus
     *            to initialize to 0.
     * @return a new Map.
     */
    protected final Map<String, Integer> newVmsPerHost(final List<PhysicalHost> cpus) {
        final Map<String, Integer> vmsPerHost = new HashMap<String, Integer>();
        for (final PhysicalHost host : cpus) {
            vmsPerHost.put(host.getUniqueIdentifier(), 0);
        }
        return vmsPerHost;
    }

    /**
     * @param racks
     *            to initialize to 0.
     * @return a new Map.
     */
    protected final Map<PhysicalRack, Integer> newVmsPerRack(final List<PhysicalRack> racks) {
        final Map<PhysicalRack, Integer> vmsPerRack = new HashMap<PhysicalRack, Integer>();
        for (final PhysicalRack rack : racks) {
            vmsPerRack.put(rack, 0);
        }
        return vmsPerRack;
    }

    /**
     * @param vmsPerStore
     *            Map to update.
     * @param application
     *            to get the vms out of.
     * @param template
     *            to filter the vms by.
     */
    protected final void updateVmsPerStoreForExistingVMs(final Map<String, Integer> vmsPerStore,
            final Application application, final VMTemplate template) {
        for (final Deployment dep : application.getDeployments()) {
            dep.refreshVMInformation();
            for (final VirtualMachine vm : dep.getVirtualMachines()) {
                if (template == null || dep.getSpecForVM(vm).getTemplate().equals(template)) {
                    for (final String store : vm.getStores()) {
                        vmsPerStore.put(store, vmsPerStore.get(store) + 1);
                    }
                }
            }
        }
    }

    /**
     * @param vmsPerHost
     *            Map to update.
     * @param application
     *            to get the vms out of.
     * @param template
     *            to filter the vms by.
     */
    protected final void updateVmsPerHostForExistingVMs(final Map<String, Integer> vmsPerHost,
            final Application application, final VMTemplate template) {
        for (final Deployment dep : application.getDeployments()) {
            DeployerPolicy.logger.debug("Got a deployment");
            dep.refreshVMInformation();
            for (final VirtualMachine vm : dep.getVirtualMachines()) {
                DeployerPolicy.logger.debug("Got a vm");
                if (template == null || dep.getSpecForVM(vm).getTemplate().equals(template)) {
                    vmsPerHost.put(vm.getHost(), vmsPerHost.get(vm.getHost()) + 1);
                }
            }
        }
    }

    /**
     * Updates the vmsPerRack list based on the vmsPerHost list.
     * 
     * @param vmsPerRack
     *            to update.
     * @param vmsPerHost
     *            to get vms per host info out of.
     * @param cpus
     *            the cpus in the tree.
     */
    protected final void updateVmsPerRackUsingHosts(final Map<PhysicalRack, Integer> vmsPerRack,
            final Map<String, Integer> vmsPerHost, final List<PhysicalHost> cpus) {
        for (final PhysicalHost host : cpus) {
            final PhysicalRack rack = host.getParent();
            vmsPerRack.put(rack, vmsPerRack.get(rack) + vmsPerHost.get(host.getUniqueIdentifier()));
        }
    }

    /**
     * Gets the least used item out of the list, based on the map.
     * 
     * @param list
     *            to get the item out of.
     * @param map
     *            used records of the items
     * @param <T>
     *            type of the item.
     * @return the least used item
     */
    protected final <T> List<T> getLeastUsedList(final Collection<T> list, final Map<T, Integer> map) {
        final List<T> result = new ArrayList<T>();
        int max = Integer.MAX_VALUE;
        for (final T item : list) {
            if (map.get(item) < max) {
                result.clear();
                result.add(item);
                max = map.get(item);
            } else if (map.get(item) == max) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Gets the least used item out of the list, based on the map.
     * 
     * @param list
     *            to get the item out of.
     * @param map
     *            used records of the items
     * @param <T>
     *            type of the item.
     * @return the least used item
     */
    protected final <T extends Identifiable> List<T> getLeastUsedListString(
            final Collection<T> list, final Map<String, Integer> map) {
        final List<T> result = new ArrayList<T>();
        int max = Integer.MAX_VALUE;
        for (final T item : list) {
            if (map.get(item.getUniqueIdentifier()) < max) {
                result.clear();
                result.add(item);
                max = map.get(item.getUniqueIdentifier());
            } else if (map.get(item.getUniqueIdentifier()) == max) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Gets the least used item out of the list, based on the map.
     * 
     * @param list
     *            to get the item out of.
     * @param map
     *            used records of the items
     * @param <T>
     *            type of the item.
     * @return the least used item
     */
    protected final <T> T getLeastUsed(final Collection<T> list, final Map<T, Integer> map) {
        final List<T> result = this.getLeastUsedList(list, map);
        final int index = new Random().nextInt(result.size());
        return result.get(index);
    }

    /**
     * Gets the least used item out of the list, based on the map.
     * 
     * @param list
     *            to get the item out of.
     * @param map
     *            used records of the items
     * @param <T>
     *            type of the item.
     * @return the least used item
     */
    protected final <T extends Identifiable> T getLeastUsedString(final Collection<T> list,
            final Map<String, Integer> map) {
        final List<T> result = this.getLeastUsedListString(list, map);
        final int index = new Random().nextInt(result.size());
        return result.get(index);
    }

    /**
     * Selects hosts based on "least used" criteria first, after which it picks
     * the least stressed one.
     * 
     * @param list
     *            to pick from.
     * @param map
     *            holds information on the number of VMs currently in place on
     *            the host.
     * @param template
     *            to get required resource information from.
     * @return the least stressed host.
     */
    protected final PhysicalHost getLeastStressedHostFromLeastUsed(
            final Collection<PhysicalHost> list, final Map<String, Integer> map,
            final VMTemplate template) {
        final List<PhysicalHost> leastUsed = this.getLeastUsedListString(list, map);
        return this.getLeastStressedFromList(leastUsed, template);
    }

    /**
     * Selects hosts based on "least used" criteria first, after which it picks
     * the least stressed one.
     * 
     * @param list
     *            to pick from.
     * @param template
     *            to get required resource information from.
     * @return the least stressed host.
     */
    protected final PhysicalHost getLeastStressedHostFromLeastUsed(
            final Collection<PhysicalHost> list, final VMTemplate template) {
        final List<PhysicalHost> leastUsed = this.getLeastUsedListString(list, this.vmsPerHost);
        return this.getLeastStressedFromList(leastUsed, template);
    }

    /**
     * Picks a suitable store from the least used stores.
     * 
     * @param list
     *            to pick out of.
     * @return a suitable disk
     * @throws DeployerException
     *             if none of the disks are suitable.
     */
    protected final PhysicalStore getSuitableStoreFromLeastUsed(final Collection<PhysicalStore> list)
            throws DeployerException {
        final List<PhysicalStore> leastUsed = this.getLeastUsedListString(list, this.vmsPerStore);
        return this.getSuitableStoreFromList(leastUsed);
    }

    /**
     * Picks a suitable store from the least used stores if possible otherwise
     * from all.
     * 
     * @param list
     *            to pick out of.
     * @return a suitable disk
     * @throws DeployerException
     *             if none of the disks are suitable.
     */
    protected final PhysicalStore getSuitableStoreFromLeastUsedOrFull(
            final Collection<PhysicalStore> list) throws DeployerException {
        final List<PhysicalStore> leastUsed = this.getLeastUsedListString(list, this.vmsPerStore);
        try {
            return this.getSuitableStoreFromList(leastUsed);
        } catch (final DeployerException e) {
            DeployerPolicy.logger
                    .warn("None of the leastUsed disks are suitable. Will try full list now.");
            return this.getSuitableStoreFromList(new ArrayList<PhysicalStore>(list));
        }
    }

    /**
     * @param list
     *            to pick a store out of.
     * @return a suitable store.
     * @throws DeployerException
     *             if not enough disk space is availabe.
     */
    protected PhysicalStore getSuitableStoreFromList(final List<PhysicalStore> list)
            throws DeployerException {
        PhysicalStore suitable = null;
        double maxi = DeployerPolicy.MAX_SUITABLE_STORAGE_PERCENTAGE;
        for (final PhysicalStore store : list) {
            if ((double) store.getUsed() / store.getCapacity() < maxi) {
                suitable = store;
                maxi = (double) store.getUsed() / store.getCapacity();
            }
        }
        if (suitable == null) {
            throw new DeployerException("Not enough disk space");
        }
        return suitable;
    }

    /**
     * Very simple selection of least stressd host. TODO: Make this slightly
     * smarter.
     * 
     * @param list
     *            to pick a host out of.
     * @param template
     *            to get the required resource information out of.
     * @return the least stressed host.
     */
    protected final PhysicalHost getLeastStressedFromList(final List<PhysicalHost> list,
            final VMTemplate template) {
        final int cpuRequired = template.getCpu();
        final int memRequired = template.getMem();

        final boolean isCPUPreferred = cpuRequired > memRequired;

        PhysicalHost best = null;
        double statistic = Double.MAX_VALUE;
        for (final PhysicalHost host : list) {
            if (isCPUPreferred && host.getCpuUsage() < statistic) {
                statistic = host.getCpuUsage();
                best = host;
            } else if (!isCPUPreferred && host.getMemUsage() < statistic) {
                statistic = host.getMemUsage();
                best = host;
            }
        }
        return best;
    }
}
