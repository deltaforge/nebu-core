package nl.bitbrains.nebu.containers;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.rest.client.RequestSender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds the information related to a VMTemplate as specified by an user.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplate implements Identifiable {

    public static final String UUID_PREFIX = "vmTemplate";
    private static Logger logger = LogManager.getLogger();
    private final String uuid;
    private String name;
    private int cpu;
    private int mem;
    private int io;
    private int net;

    /**
     * Empty default constructor.
     * 
     * @param net
     *            to set.
     * @param io
     *            to set.
     * @param mem
     *            to set.
     * @param cpu
     *            to set.
     * @param name
     *            to set.
     * @param uuid
     *            to set.
     */
    protected VMTemplate(final String uuid, final String name, final int cpu, final int mem,
            final int io, final int net) {
        VMTemplate.logger.entry();
        this.uuid = uuid;
        this.name = name;
        this.cpu = cpu;
        this.mem = mem;
        this.io = io;
        this.net = net;
        VMTemplate.logger.exit();
    }

    /**
     * @return the uuid
     */
    public final String getUniqueIdentifier() {
        return this.uuid;
    }

    /**
     * @return the name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the cpu
     */
    public final int getCpu() {
        return this.cpu;
    }

    /**
     * @param cpu
     *            the cpu to set
     */
    public final void setCpu(final int cpu) {
        this.cpu = cpu;
    }

    /**
     * @return the mem
     */
    public final int getMem() {
        return this.mem;
    }

    /**
     * @param mem
     *            the mem to set
     */
    public final void setMem(final int mem) {
        this.mem = mem;
    }

    /**
     * @return the io
     */
    public final int getIo() {
        return this.io;
    }

    /**
     * @param io
     *            the io to set
     */
    public final void setIo(final int io) {
        this.io = io;
    }

    /**
     * @return the net
     */
    public final int getNet() {
        return this.net;
    }

    /**
     * @param net
     *            the net to set
     */
    public final void setNet(final int net) {
        this.net = net;
    }

    /**
     * @return the {@link PhysicalTopology} accessible to any VirtualMachine
     *         launched using this VMTemplate.
     * @throws CacheException
     *             if the topology could not be retrieved.
     */
    public final PhysicalTopology getTopology() throws CacheException {
        VMTemplate.logger.entry();
        return VMTemplate.logger.exit(RequestSender.get().getVMTemplateTopologyOptions(this
                .getUniqueIdentifier()));
    }

    @Override
    public final int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (!(obj instanceof VMTemplate)) {
            return false;
        }
        final VMTemplate other = (VMTemplate) obj;
        return this.uuid.equals(other.uuid);
    }

}
