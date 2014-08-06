package nl.bitbrains.nebu.deployer.policies;

import java.text.ParseException;

import nl.bitbrains.nebu.deployer.DeployerPolicyFactory;

import org.jdom2.Element;

/**
 * Factory for the instantiation of the {@link HDFSReplicationPolicy} class.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class HDFSReplicationPolicyFactory implements DeployerPolicyFactory {

    /**
     * Simple default constructor.
     */
    public HDFSReplicationPolicyFactory() {
    }

    @Override
    public final HDFSReplicationPolicy fromXML(final Element xml) throws ParseException {
        // TODO: Find some configuration options for HDFSReplicationPolicy
        // (number?)
        return this.newInstance();
    }

    @Override
    public final HDFSReplicationPolicy newInstance() {
        return new HDFSReplicationPolicy();
    }

}
