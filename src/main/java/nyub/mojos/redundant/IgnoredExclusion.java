package nyub.mojos.redundant;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

/** Represents an exclusion from a dependency that should be ignored even if redundant */
public class IgnoredExclusion {
    private String dependencyGroupId;

    private String dependencyArtifactId;

    private String exclusionGroupId;

    private String exclusionArtifactId;

    /**
     * @param dependency the dependency we are excluding {@code exclusion} from
     * @param exclusion the dependency we are excluding from another {@code dependency}
     * @return true if the tuple {@code dependency, exclusion} should be ignored
     */
    boolean matches(Dependency dependency, Exclusion exclusion) {
        return match(dependencyGroupId, dependency.getGroupId())
                && match(dependencyArtifactId, dependency.getArtifactId())
                && match(exclusionGroupId, exclusion.getGroupId())
                && match(exclusionArtifactId, exclusion.getArtifactId());
    }

    private boolean match(String pattern, String id) {
        return pattern.equals("*") || pattern.equals(id);
    }
}
