package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelFactory;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.compmodelcons.generator.Metamodel;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.compmodelcons.views.DynamicModels;
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

        List<Metamodel> originMetamodels = registry.values().stream()
                .filter(value -> value instanceof EPackage)
                .map(value -> (EPackage) value)
                .map(ePackage -> createMetamodel(ePackage, "", "models"))
                .toList();

        AQR aqr = createAQR(neojoin, registry);

        Metamodel viewtypeMetamodel = createMetamodel(viewtype, NamingGenerator.convertToPascalCase(aqr.export().name()), NamingGenerator.PACKAGE_BASE);

        return new ViewTypeSourceGenerator(name, originMetamodels, viewtypeMetamodel, aqr);
    }

    private static Metamodel createMetamodel(EPackage ePackage, String prefix, String basePackage) {
        return new Metamodel(ePackage, createGenPackage(ePackage, prefix, basePackage));
    }

    private static GenPackage createGenPackage(EPackage ePackage, String prefix, String basePackage) {
        GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();
        genModel.initialize(List.of(ePackage));

        GenPackage genPackage = genModel.getGenPackages().get(0);
        genPackage.setPrefix(prefix);
        genPackage.setBasePackage(basePackage);

        return genPackage;
    }

    protected static EPackage createEPackage() {
        EPackage ePackage = DynamicModels.createEPackage();
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
            files.add(new JavaSourceFromString("models/restaurant/Package.java", createRestaurantStub()));
            files.add(new JavaSourceFromString("models/reviewpage/Package.java", createReviewPageStub()));
            files.addAll(List.of(stubs));

            boolean ok = compiler.getTask(null, fileManager, null, options, null, files).call();
            assertTrue(ok);
        }
    }

    private static String createRestaurantStub() {
        return """
                package models.restaurant;
                
                import org.eclipse.emf.ecore.EPackage;
                import org.eclipse.emf.ecore.EClass;
                import org.eclipse.emf.ecore.EAttribute;
                import org.eclipse.emf.ecore.EReference;
                import org.eclipse.emf.ecore.EDataType;
                import org.eclipse.emf.ecore.EEnum;
                
                public interface Package extends EPackage {
                    Package eINSTANCE = null;
                
                    interface Literals {
                        EClass RESTAURANT = null;
                        EAttribute RESTAURANT__NAME = null;
                        EAttribute RESTAURANT__ADDRESS = null;
                        EReference RESTAURANT__SELLS = null;
                        EAttribute RESTAURANT__NUM_EMPLOYEES = null;
                        EAttribute RESTAURANT__DAILY_REVENUE = null;
                
                        EClass FOOD = null;
                        EAttribute FOOD__NAME = null;
                        EAttribute FOOD__PRICE = null;
                        EAttribute FOOD__TYPE = null;
                
                        EClass STORE = null;
                        EReference STORE__RESTAURANTS = null;
                        EReference STORE__FOODS = null;
                
                        EDataType MONEY = null;
                        EEnum FOOD_TYPE = null;
                    }
                }
                """;
    }

    private static String createReviewPageStub() {
        return """
                package models.reviewpage;
                
                import org.eclipse.emf.ecore.EPackage;
                import org.eclipse.emf.ecore.EClass;
                import org.eclipse.emf.ecore.EAttribute;
                import org.eclipse.emf.ecore.EReference;
                import org.eclipse.emf.ecore.EDataType;
                import org.eclipse.emf.ecore.EEnum;
                
                public interface Package extends EPackage {
                    Package eINSTANCE = null;
                
                    interface Literals {
                        EClass REVIEW_PAGE = null;
                        EAttribute REVIEW_PAGE__NAME = null;
                        EReference REVIEW_PAGE__REVIEWS = null;
                
                        EClass REVIEW = null;
                        EAttribute REVIEW__USER = null;
                        EAttribute REVIEW__RATING = null;
                
                        EClass STORE = null;
                        EReference STORE__PAGES = null;
                        EReference STORE__REVIEWS = null;
                    }
                }
                """;
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
