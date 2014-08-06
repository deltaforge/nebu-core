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
public class LocalityPolicyFactory implements DeployerPolicyFactory {

    public static final String TAG_MAX_VMS_PER_HOST = "maxVmsPerHost";

    /**
     * Default empty constructor.
     */
    public LocalityPolicyFactory() {

    }

    @Override
    public final LocalityPolicy fromXML(final Element xml) throws ParseException {
        final LocalityPolicy policy = this.newInstance();

        if (xml.getChild(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST) != null) {
            final int maxVmsPerHost = Integer.parseInt(xml
                    .getChildTextTrim(LocalityPolicyFactory.TAG_MAX_VMS_PER_HOST));
            policy.setMaxVMsPerHost(maxVmsPerHost);
        }

        return policy;
    }

    @Override
    public final LocalityPolicy newInstance() {
        return new LocalityPolicy();
    }

}
