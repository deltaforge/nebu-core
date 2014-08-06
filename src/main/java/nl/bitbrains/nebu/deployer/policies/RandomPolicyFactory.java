package nl.bitbrains.nebu.deployer.policies;

import java.text.ParseException;

import nl.bitbrains.nebu.deployer.DeployerPolicy;
import nl.bitbrains.nebu.deployer.DeployerPolicyFactory;

import org.jdom2.Element;

/**
 * Factory for the instantiation of the {@link RandomPolicy} class.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class RandomPolicyFactory implements DeployerPolicyFactory {

    public static final String TAG_SEED = "seed";

    /**
     * Simple constructor.
     */
    public RandomPolicyFactory() {
    }

    @Override
    public final DeployerPolicy fromXML(final Element xml) throws ParseException {
        final RandomPolicy policy = (RandomPolicy) this.newInstance();

        if (xml.getChild(RandomPolicyFactory.TAG_SEED) != null) {
            policy.setSeed(Long.parseLong(xml.getChildTextTrim(RandomPolicyFactory.TAG_SEED)));
        }
        return policy;
    }

    @Override
    public final DeployerPolicy newInstance() {
        return new RandomPolicy();
    }

}
