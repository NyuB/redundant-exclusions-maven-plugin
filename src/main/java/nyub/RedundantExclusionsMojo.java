package nyub;

import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(
        name = "redundant-exclusions",
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        defaultPhase = LifecyclePhase.VALIDATE)
public class RedundantExclusionsMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    public void execute() {
        @SuppressWarnings("unchecked")
        final List<Dependency> all = project.getDependencies();
        for (Dependency dependency : all) {
            for (Exclusion exclusion : dependency.getExclusions()) {
                if (!inclusionWouldClash(exclusion, dependency, all)) {
                    getLog().error(
                                    "Dependency "
                                            + formatExclusion(exclusion)
                                            + " is excluded from "
                                            + formatDependency(dependency)
                                            + " but it would not clash with any other dependency");
                }
            }
        }
    }

    private boolean inclusionWouldClash(
            Exclusion exclusion, Dependency excludedFrom, List<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            if (exclusionMatches(exclusion, dependency)) {
                if (!excludedVersion(exclusion, excludedFrom).equals(dependency.getVersion())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String excludedVersion(Exclusion exclusion, Dependency dependency) {
        return TODO("Implement retrieval of excluded dependency exact version");
    }

    private String formatDependency(Dependency dependency) {
        var optionalSuffix = "";
        if (dependency.getClassifier() != null) optionalSuffix += ":" + dependency.getClassifier();
        if (dependency.getType().equals("jar")) optionalSuffix += ":" + dependency.getType();
        return String.format(
                "%s:%s:%s%s",
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                optionalSuffix);
    }

    private String formatExclusion(Exclusion exclusion) {
        return String.format("%s:%s", exclusion.getGroupId(), exclusion.getArtifactId());
    }

    private boolean exclusionMatches(Exclusion exclusion, Dependency dependency) {
        if (!exclusion.getGroupId().equals(dependency.getGroupId())) return false;
        if (!exclusion.getArtifactId().equals(dependency.getArtifactId())) return false;
        return true;
    }

    private static <T> T TODO(String msg) {
        throw new IllegalStateException(String.format("TODO: '%s'", msg));
    }
}
