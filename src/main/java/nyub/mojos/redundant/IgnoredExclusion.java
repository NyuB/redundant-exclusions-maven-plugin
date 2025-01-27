package nyub.mojos.redundant;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;

public class IgnoredExclusion {
    private String dependencyGroupId;

    private String dependencyArtifactId;

    private String exclusionGroupId;

    private String exclusionArtifactId;

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
