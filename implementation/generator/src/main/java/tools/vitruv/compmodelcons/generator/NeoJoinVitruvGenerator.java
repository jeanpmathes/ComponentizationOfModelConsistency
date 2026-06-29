package tools.vitruv.compmodelcons.generator;

import com.google.inject.Inject;
import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.generator.GeneratorAdapterFactory;
import org.eclipse.emf.codegen.ecore.genmodel.*;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.xtext.generator.IFileSystemAccess;
import org.eclipse.xtext.generator.IGenerator;
import tools.vitruv.compmodelcons.generator.backend.ViewTypeSourceGenerator;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.generation.ModelInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class NeoJoinVitruvGenerator implements IGenerator {
    private final static String PACKAGE_EXTENSION = ".ecore";
    private final static String GENMODEL_EXTENSION = ".genmodel";

    private final Parser parser;

    @Inject
    public NeoJoinVitruvGenerator(Parser parser) {
        this.parser = parser;
    }

    @Override
    public void doGenerate(Resource input, IFileSystemAccess fsa) {
        Parser.Result result = parser.parse(input.getURI());

        if (result instanceof Parser.Result.Success success) {
            String name = input.getURI().trimFileExtension().lastSegment();
            AQR aqr = success.aqr();

            EPackage metaModel;

            try {
                metaModel = generateMetaModel(input, name, aqr, fsa);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            generateSource(name, metaModel, aqr, fsa);


        }
    }

    private EPackage generateMetaModel(Resource input, String name, AQR aqr, IFileSystemAccess fsa) throws IOException {
        ResourceSet resourceSet = input.getResourceSet();
        String baseFileName = String.format("ecore/%s/%s", aqr.export().name(), name);

        MetaModelGenerator metaModelGenerator = new MetaModelGenerator(aqr);
        ModelInfo metaModelInfo = metaModelGenerator.generate();
        EPackage metaModel = metaModelInfo.pack();
        fsa.generateFile(baseFileName + PACKAGE_EXTENSION, getContentsForFile(resourceSet, name, PACKAGE_EXTENSION, metaModel));

        GenModel genModel = createGenModel(input, name, aqr, metaModel);
        fsa.generateFile(baseFileName + GENMODEL_EXTENSION, getContentsForFile(resourceSet, name, GENMODEL_EXTENSION, genModel));

        generateMetaModelCode(genModel);

        return metaModel;
    }

    private GenModel createGenModel(Resource input, String name, AQR aqr, EPackage ePackage) {
        String modelName = NamingGenerator.convertToPascalCase(aqr.export().name());
        String modelPackage = NamingGenerator.getPackageName(aqr);

        GenModel genModel = GenModelFactory.eINSTANCE.createGenModel();

        genModel.setModelName(modelName);
        genModel.getForeignModel().add(name + PACKAGE_EXTENSION);

        genModel.setImporterID("org.eclipse.emf.importer.ecore");
        genModel.setComplianceLevel(GenJDKLevel.JDK170_LITERAL);
        genModel.setCopyrightFields(false);
        genModel.setOperationReflection(true);
        genModel.setImportOrganizing(true);

        genModel.setModelPluginID(modelPackage);
        genModel.setModelDirectory(String.format("/%s/target/generated-sources/ecore", getProjectPackage(input)));

        genModel.initialize(List.of(ePackage));
        GenPackage genPackage = genModel.getGenPackages().get(0);

        genPackage.setPrefix(modelName);
        genPackage.setBasePackage(modelPackage);
        genPackage.setDisposableProviderFactory(true);

        return genModel;
    }

    private String getProjectPackage(Resource input) {
        URI uri = input.getURI();

        if (uri.isPlatformResource()) {
            return uri.segment(1);
        }

        if (uri.isFile()) {
            Path inputPath = Path.of(uri.toFileString()).toAbsolutePath().normalize();

            return EcorePlugin.getPlatformResourceMap().entrySet().stream()
                    .filter(entry -> entry.getValue().isFile())
                    .filter(entry -> {
                        Path projectPath = Path.of(entry.getValue().toFileString()).toAbsolutePath().normalize();
                        return inputPath.startsWith(projectPath);
                    })
                    .max(Comparator.comparingInt(entry -> Path.of(entry.getValue().toFileString()).getNameCount()))
                    .map(Map.Entry::getKey)
                    .orElseThrow();
        }

        throw new IllegalArgumentException("Could not determine project package for " + input);
    }

    private CharSequence getContentsForFile(ResourceSet resourceSet, String name, String extension, EObject eObject) throws IOException {
        URI uri = URI.createURI(name + extension);

        Resource genModelResource = resourceSet.createResource(uri);
        genModelResource.getContents().add(eObject);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        genModelResource.save(output, Map.of(XMLResource.OPTION_ENCODING, "UTF-8"));

        return output.toString();
    }

    private void generateMetaModelCode(GenModel genModel) {
        GeneratorAdapterFactory.Descriptor.Registry.INSTANCE.addDescriptor(
                GenModelPackage.eNS_URI,
                GenModelGeneratorAdapterFactory.DESCRIPTOR
        );

        Generator generator = new Generator();
        generator.setInput(genModel);

        Diagnostic result = generator.generate(genModel, GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE, new BasicMonitor.Printing(System.err));
        if (result.getSeverity() >= Diagnostic.WARNING) {
            throw new IllegalStateException(result.toString());
        }
    }

    private void generateSource(String name, EPackage metaModel, AQR aqr, IFileSystemAccess fsa) {
        ViewTypeSourceGenerator sourceGenerator = new ViewTypeSourceGenerator(name, metaModel, aqr);
        fsa.generateFile(sourceGenerator.getFileName(), sourceGenerator.generate());
    }
}
