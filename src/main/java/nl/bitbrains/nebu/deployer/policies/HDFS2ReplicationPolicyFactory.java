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
public class HDFS2ReplicationPolicyFactory implements DeployerPolicyFactory {

    /**
     * Simple default constructor.
     */
    public HDFS2ReplicationPolicyFactory() {
    }

    @Override
    public final HDFS2ReplicationPolicy fromXML(final Element xml) throws ParseException {
        // TODO: Find some configuration options for HDFSReplicationPolicy
        // (number?)
        return this.newInstance();
    }

    @Override
    public final HDFS2ReplicationPolicy newInstance() {
        return new HDFS2ReplicationPolicy();
    }

}
