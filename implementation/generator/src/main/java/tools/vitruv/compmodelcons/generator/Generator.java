package tools.vitruv.compmodelcons.generator;

import org.jspecify.annotations.NonNull;
import tools.vitruv.neojoin.aqr.AQR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Generator {
    private final Path outputDirectory;

    public Generator(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void generate(Path relativeSourcePath, AQR aqr) throws IOException {
        Path destinationPath = getDestinationPath(relativeSourcePath);
        String sourceCode = generateSourceCode(aqr);

        Files.writeString(destinationPath, sourceCode);
    }

    private Path getDestinationPath(Path relativeSourcePath) {
        Path destinationPath = outputDirectory.resolve(relativeSourcePath);

        String filename = destinationPath.getFileName().toString();
        filename = filename.replace(".nj", ".java");

        destinationPath = destinationPath.getParent().resolve(filename);
        return destinationPath;
    }

    private String generateSourceCode(AQR aqr) {
        return "";
    }
}
