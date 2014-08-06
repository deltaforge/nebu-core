package nl.bitbrains.nebu.deployer.policies;

import java.text.ParseException;

import nl.bitbrains.nebu.deployer.DeployerPolicyFactory;

import org.jdom2.Element;

/**
 * Factory for the instantiation of the {@link BasicReplicationPolicy} class.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class BasicReplicationPolicyFactory implements DeployerPolicyFactory {

    /**
     * Simple default constructor.
     */
    public BasicReplicationPolicyFactory() {

    }

    @Override
    public final BasicReplicationPolicy fromXML(final Element xml) throws ParseException {
        // TODO: Find some configuration options for BasicReplicationPolicy
        // (rack vs dc-based?)
        return this.newInstance();
    }

    @Override
    public final BasicReplicationPolicy newInstance() {
        return new BasicReplicationPolicy();
    }

}
