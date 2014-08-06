package nl.bitbrains.nebu.containers;

import nl.bitbrains.nebu.common.interfaces.IBuilder;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.util.ErrorChecker;

/**
 * Builder class for the {@link VMTemplate}.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplateBuilder implements IBuilder<VMTemplate> {
    private String uuid;
    private String name;
    private int cpu;
    private int mem;
    private int io;
    private int net;

    /**
     * Simple constructor.
     */
    public VMTemplateBuilder() {
        this.reset();
    }

    /**
     * Resets the builder.
     */
    public final void reset() {
        this.uuid = null;
        this.name = null;
        this.cpu = 0;
        this.mem = 0;
        this.io = 0;
        this.net = 0;
    }

    /**
     * @param uuid
     *            to build with.
     * @return this for fluency
     */
    public final VMTemplateBuilder withUuid(final String uuid) {
        ErrorChecker.throwIfNullArgument(uuid, Identifiable.UUID_NAME);
        this.uuid = uuid;
        return this;
    }

    /**
     * @param name
     *            to build with
     * @return this for fluency.
     */
    public final VMTemplateBuilder withName(final String name) {
        ErrorChecker.throwIfNullArgument(name, "name");
        this.name = name;
        return this;
    }

    /**
     * 
     * @param cpu
     *            to build with.
     * @return this for fluency.
     */
    public final VMTemplateBuilder withCPU(final int cpu) {
        this.cpu = cpu;
        return this;
    }

    /**
     * 
     * @param mem
     *            to build with.
     * @return this for fluency.
     */
    public final VMTemplateBuilder withMem(final int mem) {
        this.mem = mem;
        return this;
    }

    /**
     * 
     * @param io
     *            to build with.
     * @return this for fluency.
     */
    public final VMTemplateBuilder withIO(final int io) {
        this.io = io;
        return this;
    }

    /**
     * 
     * @param net
     *            to build with.
     * @return this for fluency.
     */
    public final VMTemplateBuilder withNet(final int net) {
        this.net = net;
        return this;
    }

    /**
     * @return the build {@link VMTemplate} object.
     */
    public final VMTemplate build() {
        ErrorChecker.throwIfNotSet(this.uuid, Identifiable.UUID_NAME);
        final VMTemplate template = new VMTemplate(this.uuid, this.name, this.cpu, this.mem,
                this.io, this.net);
        this.reset();
        return template;
    }

}
