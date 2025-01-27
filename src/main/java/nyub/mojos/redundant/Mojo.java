package nyub.mojos.redundant;

import java.util.Collections;
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

@org.apache.maven.plugins.annotations.Mojo(
        name = "redundant-exclusions",
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        defaultPhase = LifecyclePhase.VERIFY)
public class Mojo extends AbstractMojo {
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
        final RedundantExclusions redundantExclusions = new RedundantExclusions();
        for (Dependency dependency : allDependencies) {
            for (Exclusion exclusion : dependency.getExclusions()) {
                if (ignored(dependency, exclusion)) continue;

                final Optional<String> maybeVersion = excludedVersion(exclusion, dependency);

                if (maybeVersion.isEmpty())
                    redundantExclusions.exclusionIsNotADependency(exclusion, dependency);
                else if (!inclusionWouldClash(exclusion, maybeVersion.get(), allArtifacts))
                    redundantExclusions.exclusionWouldNotClash(exclusion, dependency);
            }
        }
        redundantExclusions.failIfAnyError(getLog());
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

    private static boolean exclusionMatches(Exclusion exclusion, Artifact dependency) {
        return exclusion.getGroupId().equals(dependency.getGroupId())
                && exclusion.getArtifactId().equals(dependency.getArtifactId());
    }

    /**
     * From https://github.com/mfoo/unnecessary-exclusions-maven-plugin/tree/main
     *
     * <p>Used to filter for classes that will be on the classpath at runtime. We don't need
     * test-scoped dependencies, those aren't inherited between projects.
     */
    private static final DependencyFilter CLASSPATH_FILTER =
            DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME);

    /**
     * From https://github.com/mfoo/unnecessary-exclusions-maven-plugin/tree/main
     *
     * <p>Use the Maven project/dependency APIs to fetch the list of dependencies for this
     * dependency. This will make API calls to the configured repositories.
     *
     * @return an empty list if the dependency resolution fails
     */
    private List<ArtifactResult> getDependencies(Dependency projectDependency) {
        DependencyRequest dependencyRequest = setupDependencyRequest(projectDependency);

        try {
            return repoSystem
                    .resolveDependencies(repoSession, dependencyRequest)
                    .getArtifactResults();
        } catch (DependencyResolutionException e) {
            getLog().error(
                            "Could not fetch details for %s:%s"
                                    .formatted(
                                            projectDependency.getGroupId(),
                                            projectDependency.getArtifactId()));
            return Collections.emptyList();
        }
    }

    /**
     * From https://github.com/mfoo/unnecessary-exclusions-maven-plugin/tree/main
     *
     * @return a request for dependency resolution, configured with the available repositories and
     *     {@link Mojo#CLASSPATH_FILTER}
     */
    private DependencyRequest setupDependencyRequest(Dependency projectDependency) {
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
        return new DependencyRequest(collectRequest, CLASSPATH_FILTER);
    }
}
