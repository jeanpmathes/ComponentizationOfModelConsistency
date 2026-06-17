package tools.vitruv.compmodelcons.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import tools.vitruv.compmodelcons.generator.Generator;
import tools.vitruv.compmodelcons.views.neojoin.vitruv.Helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        threadSafe = true
)
public class GeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(
            property = "neojoin.sourceDirectory",
            defaultValue = "${project.basedir}/src/main/neojoin"
    )
    private File sourceDirectory;

    @Parameter(
            property = "neojoin.outputDirectory",
            defaultValue = "${project.build.directory}/generated-sources/neojoin"
    )
    private File outputDirectory;

    private static boolean isNeoJoinFile(Path path) {
        return path.getFileName().toString().endsWith(".nj");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path inputDirectoryPath = sourceDirectory.toPath();
        Path outputDirectoryPath = outputDirectory.toPath();

        if (!Files.isDirectory(inputDirectoryPath)) {
            return;
        }

        Generator generator = new Generator(outputDirectoryPath);

        try {
            Files.createDirectories(outputDirectoryPath);

            for (Path neoJoinFilePath : discoverNeoJoinFiles(inputDirectoryPath)) {
                Path relativeNeoJoinFilePath = inputDirectoryPath.relativize(neoJoinFilePath);
                Helper.getAQR(neoJoinFilePath.toString()).ifPresent(aqr -> {
                    try {
                        generator.generate(relativeNeoJoinFilePath, aqr);
                    } catch (IOException e) {
                        getLog().error("Could not generate source code for " + neoJoinFilePath, e);
                    }
                });
                // todo: bring the helper stuff into here, load the EPackages once, also do something with the errors from parsing
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read source directory", e);
        }
    }

    private List<Path> discoverNeoJoinFiles(Path sourceDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            return paths.filter(Files::isRegularFile).filter(GeneratorMojo::isNeoJoinFile).toList();
        }
    }
}
