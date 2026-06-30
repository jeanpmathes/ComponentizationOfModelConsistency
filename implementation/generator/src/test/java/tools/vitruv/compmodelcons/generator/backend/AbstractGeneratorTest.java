package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.collector.PackageModelCollector;

import javax.tools.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractGeneratorTest {
    protected final static String MODEL_NAME = "mymodel";

    private static AQR createAQR(String query, EPackage.Registry registry) throws IOException {
        Path path = Files.createTempFile("view", ".nj");

        Files.writeString(path, "export " + MODEL_NAME + " " + """
                to "http://example.com"
                
                import "http://example.org/restaurant"
                import "http://example.org/reviewpage"
                
                """ + query);

        NeoJoinStandaloneSetup setup = new NeoJoinStandaloneSetup(registry);
        Parser.Result result = setup.getParser().parse(URI.createFileURI(path.toString()));

        Parser.Result.Success success = assertInstanceOf(Parser.Result.Success.class, result);
        return success.aqr();
    }

    protected static ViewTypeSourceGenerator createGenerator(EPackage viewtype, String name, String neojoin) throws URISyntaxException, IOException {
        PackageModelCollector collector = new PackageModelCollector(Paths.get(Objects.requireNonNull(ViewTypeSourceGeneratorTest.class.getClassLoader().getResource("models")).toURI()).toString());
        EPackage.Registry registry = collector.collect();

        List<EPackage> originMetamodels = registry.values().stream()
                .filter(value -> value instanceof EPackage)
                .map(value -> (EPackage) value)
                .toList();

        AQR aqr = createAQR(neojoin, registry);

        GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();
        genModel.initialize(List.of(viewtype));

        GenPackage genPackage = genModel.getGenPackages().get(0);
        genPackage.setPrefix(NamingGenerator.convertToPascalCase(aqr.export().name()));
        genPackage.setBasePackage(NamingGenerator.PACKAGE_BASE);

        return new ViewTypeSourceGenerator(name, originMetamodels, viewtype, genPackage, aqr);
    }

    protected static EPackage createEPackage() {
        EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName(MODEL_NAME);
        return ePackage;
    }

    protected static void compile(ViewTypeSourceGenerator generator, JavaFileObject... stubs) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            fileManager.setLocationFromPaths(
                    StandardLocation.CLASS_OUTPUT,
                    List.of(Files.createTempDirectory(null))
            );

            List<String> options = List.of(
                    "-proc:none"
            );

            List<JavaFileObject> files = new ArrayList<>();
            files.add(new JavaSourceFromString(generator.getFileName(), generator.generate()));
            files.addAll(List.of(stubs));

            boolean ok = compiler.getTask(null, fileManager, null, options, null, files).call();
            assertTrue(ok);
        }
    }

    private static String createStubClass() {
        return "";
    }

    public static class JavaSourceFromString extends SimpleJavaFileObject {
        private final String code;

        protected JavaSourceFromString(String name, String code) {
            super(java.net.URI.create(String.format("string:///%s", name)), Kind.SOURCE);

            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
