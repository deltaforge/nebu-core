package nl.bitbrains.nebu.containers;

import java.util.Collection;

/**
 * Defines an interface to retrieve {@link VMTemplate}s by UUID or as a
 * collection.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public interface VMTemplateCollection {

    /**
     * @param uuid
     *            the UUID of a {@link VMTemplate}.
     * @return the corresponding {@link VMTemplate}.
     */
    VMTemplate getVMTemplate(String uuid);

    /**
     * @return a collection of available {@link VMTemplate}s.
     */
    Collection<VMTemplate> getVMTemplates();

}
