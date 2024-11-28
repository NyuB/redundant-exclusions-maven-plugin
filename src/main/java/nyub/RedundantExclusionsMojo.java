package nyub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

@Mojo(
        name = "redundant-exclusions",
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        defaultPhase = LifecyclePhase.TEST_COMPILE)
public class RedundantExclusionsMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component private RepositorySystem repoSystem;

    /** The current repository/network configuration of Maven. */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of plugins and their
     * dependencies.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    public void execute() {
        final Set<Artifact> allArtifacts = project.getArtifacts();
        final List<Dependency> allDependencies = project.getDependencies();
        for (Dependency dependency : allDependencies) {
            for (Exclusion exclusion : dependency.getExclusions()) {
                final var maybeVersion = excludedVersion(exclusion, dependency);
                if (maybeVersion.isEmpty()) {
                    getLog().error(
                                    String.format(
                                            "Dependency %s  is not a dependency of %s",
                                            formatExclusion(exclusion),
                                            formatDependency(dependency)));
                } else if (!inclusionWouldClash(exclusion, maybeVersion.get(), allArtifacts)) {
                    getLog().error(
                                    String.format(
                                            "Dependency %s is excluded from %s but it would not"
                                                    + " clash with any other dependency",
                                            formatExclusion(exclusion),
                                            formatDependency(dependency)));
                }
            }
        }
    }

    private boolean inclusionWouldClash(
            Exclusion exclusion, String version, Set<Artifact> dependencies) {

        for (Artifact dependency : dependencies) {
            if (exclusionMatches(exclusion, dependency)) {
                if (!version.equals(dependency.getVersion())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<String> excludedVersion(Exclusion exclusion, Dependency dependency) {
        final var all = getDependencies(dependency);
        return all.stream()
                .filter(
                        d ->
                                d.getArtifact().getArtifactId().equals(exclusion.getArtifactId())
                                        && d.getArtifact()
                                                .getGroupId()
                                                .equals(exclusion.getGroupId()))
                .findFirst()
                .map(d -> d.getArtifact().getVersion());
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

    private String formatArtifact(Artifact dependency) {
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

    private boolean exclusionMatches(Exclusion exclusion, Artifact dependency) {
        if (!exclusion.getGroupId().equals(dependency.getGroupId())) return false;
        if (!exclusion.getArtifactId().equals(dependency.getArtifactId())) return false;
        return true;
    }

    /**
     * Used to filter for classes that will be on the classpath at runtime. We don't need
     * test-scoped dependencies, those aren't inherited between projects.
     */
    private static final DependencyFilter CLASSPATH_FILTER =
            DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME);

    /**
     * Copied from https://github.com/mfoo/unnecessary-exclusions-maven-plugin/tree/main
     *
     * <p>Use the Maven project/dependency APIs to fetch the list of dependencies for this
     * dependency. This will make API calls to the configured repositories.
     */
    private List<ArtifactResult> getDependencies(
            org.apache.maven.model.Dependency projectDependency) {

        List<ArtifactResult> results = new ArrayList<>();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(
                new org.eclipse.aether.graph.Dependency(
                        new org.eclipse.aether.artifact.DefaultArtifact(
                                projectDependency.getGroupId(),
                                projectDependency.getArtifactId(),
                                "pom",
                                projectDependency.getVersion()),
                        projectDependency.getScope()));

        for (RemoteRepository remoteRepo : remoteRepos) {
            collectRequest.addRepository(remoteRepo);
        }

        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, CLASSPATH_FILTER);

        try {
            results.addAll(
                    repoSystem
                            .resolveDependencies(repoSession, dependencyRequest)
                            .getArtifactResults());
        } catch (DependencyResolutionException e) {
            getLog().error(
                            String.format(
                                    "Could not fetch details for %s:%s",
                                    projectDependency.getGroupId(),
                                    projectDependency.getArtifactId()));
        }

        return results;
    }
}
