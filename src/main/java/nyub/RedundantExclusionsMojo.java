package nyub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
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
        defaultPhase = LifecyclePhase.VERIFY)
public class RedundantExclusionsMojo extends AbstractMojo {
    @Component private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    @Parameter(property = "ignoredExclusions", required = true)
    private List<IgnoredExclusion> ignoredExclusions;

    public void execute() throws MojoFailureException {
        final Set<Artifact> allArtifacts = project.getArtifacts();
        final List<Dependency> allDependencies = project.getDependencies();
        final List<String> errors = new ArrayList<>();
        getLog().warn(String.format("Ignoring %d patterns", ignoredExclusions.size()));
        for (Dependency dependency : allDependencies) {
            for (Exclusion exclusion : dependency.getExclusions()) {
                if (ignored(dependency, exclusion)) continue;
                final var maybeVersion = excludedVersion(exclusion, dependency);
                if (maybeVersion.isEmpty()) {
                    errors.add(
                            String.format(
                                    "Dependency %s  is not a dependency of %s",
                                    formatExclusion(exclusion), formatDependency(dependency)));
                } else if (!inclusionWouldClash(exclusion, maybeVersion.get(), allArtifacts)) {
                    errors.add(
                            String.format(
                                    "Dependency %s is excluded from %s but it would not"
                                            + " clash with any other dependency",
                                    formatExclusion(exclusion), formatDependency(dependency)));
                }
            }
        }
        errors.forEach(e -> getLog().error(e));
        if (!errors.isEmpty())
            throw new MojoFailureException("Redundant or suspicious dependencies detected");
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

    private static boolean inclusionWouldClash(
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

    private boolean ignored(Dependency dependency, Exclusion exclusion) {
        return this.ignoredExclusions.stream().anyMatch(i -> i.matches(dependency, exclusion));
    }

    private static String formatDependency(Dependency dependency) {
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

    private static String formatExclusion(Exclusion exclusion) {
        return String.format("%s:%s", exclusion.getGroupId(), exclusion.getArtifactId());
    }

    private static boolean exclusionMatches(Exclusion exclusion, Artifact dependency) {
        return exclusion.getGroupId().equals(dependency.getGroupId())
                && exclusion.getArtifactId().equals(dependency.getArtifactId());
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
