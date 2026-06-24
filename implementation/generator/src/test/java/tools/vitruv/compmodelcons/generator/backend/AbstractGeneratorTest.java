package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.collector.PackageModelCollector;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class AbstractGeneratorTest {
    private static AQR createAQR(String query) throws URISyntaxException, IOException {
        PackageModelCollector collector = new PackageModelCollector(Paths.get(Objects.requireNonNull(GeneratorTest.class.getClassLoader().getResource("models")).toURI()).toString());
        EPackage.Registry registry = collector.collect();

        Path path = Files.createTempFile("view", ".nj");
        Files.writeString(path, """
                export package to "http://example.com"
                
                import "http://example.org/restaurant"
                import "http://example.org/reviewpage"
                
                """ + query);

        NeoJoinStandaloneSetup setup = new NeoJoinStandaloneSetup(registry);
        Parser.Result result = setup.getParser().parse(URI.createFileURI(path.toString()));

        Parser.Result.Success success = assertInstanceOf(Parser.Result.Success.class, result);
        return success.aqr();
    }

    protected static Generator createGenerator(String name, String neojoin) throws URISyntaxException, IOException {
        AQR aqr = createAQR(neojoin);
        return new Generator(name, aqr);
    }
}
