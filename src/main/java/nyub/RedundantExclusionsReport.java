package nyub;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * A listing of redundant exclusion error cases, each exposed method representing an exclusion
 * scenario we want to detect and report
 */
public class RedundantExclusionsReport {
    private final List<String> errors = new ArrayList<>();

    void exclusionIsNotADependency(Exclusion exclusion, Dependency excludedFrom) {
        errors.add(
                "Dependency %s is excluded from %s but is not one of its dependency"
                        .formatted(formatExclusion(exclusion), formatDependency(excludedFrom)));
    }

    void exclusionWouldNotClash(Exclusion exclusion, Dependency excludedFrom) {
        errors.add(
                "Dependency %s is excluded from %s but it would not clash with any other dependency"
                        .formatted(formatExclusion(exclusion), formatDependency(excludedFrom)));
    }

    /**
     * Logs each reported redundant exclusion error, then throw if any error was reported
     *
     * @param log logging system to report each redundant exclusion error
     * @throws MojoFailureException if any redundant exclusion error was reported
     */
    void failIfAnyError(Log log) throws MojoFailureException {
        errors.forEach(log::error);
        if (!errors.isEmpty())
            throw new MojoFailureException(
                    "Redundant dependency exclusions detected (%d errors, check previous logs)"
                            .formatted(errors.size()));
    }

    private static String formatDependency(Dependency dependency) {
        var optionalSuffix = "";
        if (dependency.getClassifier() != null) optionalSuffix += ":" + dependency.getClassifier();
        if (dependency.getType().equals("jar")) optionalSuffix += ":" + dependency.getType();
        return "%s:%s:%s%s"
                .formatted(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        optionalSuffix);
    }

    private static String formatExclusion(Exclusion exclusion) {
        return "%s:%s".formatted(exclusion.getGroupId(), exclusion.getArtifactId());
    }
}
