package tools.vitruv.compmodelcons.generator;

import com.google.j2objc.annotations.UsedByReflection;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.mwe2.ecore.EcoreGenerator;
import org.eclipse.emf.mwe2.runtime.workflow.IWorkflowComponent;
import org.eclipse.emf.mwe2.runtime.workflow.IWorkflowContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AggregateEcoreGenerator implements IWorkflowComponent {
    private final List<EcoreGenerator> generators = new ArrayList<>();

    private String genModels;
    private String srcPath;
    private Boolean generateCustomClasses;
    private ResourceSet resourceSet;

    @UsedByReflection
    public void setGenModels(String genModels) {
        this.genModels = genModels;
    }

    @UsedByReflection
    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    @UsedByReflection
    public void setGenerateCustomClasses(Boolean generateCustomClasses) {
        this.generateCustomClasses = generateCustomClasses;
    }

    private ResourceSet getResourceSet() {
        return resourceSet == null ? new ResourceSetImpl() : resourceSet;
    }

    @UsedByReflection
    public void setResourceSet(ResourceSet resourceSet) {
        this.resourceSet = resourceSet;
    }

    @Override
    public void preInvoke() {
        createGenerators();

        for (EcoreGenerator generator : generators) {
            generator.preInvoke();
        }
    }

    @Override
    public void invoke(IWorkflowContext ctx) {
        for (EcoreGenerator generator : generators) {
            generator.invoke(ctx);
        }
    }

    @Override
    public void postInvoke() {
        for (EcoreGenerator generator : generators) {
            generator.postInvoke();
        }

        generators.clear();
    }

    private void createGenerators() {
        for (String genModel : findGenModels()) {
            EcoreGenerator generator = new EcoreGenerator();

            generator.setGenModel(genModel);
            generator.addSrcPath(srcPath);
            generator.setGenerateCustomClasses(generateCustomClasses);

            generators.add(generator);
        }
    }

    private List<String> findGenModels() {
        URI uri = URI.createURI(this.genModels);
        URI fileURI = uri;

        if (uri.isPlatformResource()) {
            fileURI = EcorePlugin.resolvePlatformResourcePath(uri.toPlatformString(true));
        }

        if (!fileURI.isFile()) {
            throw new IllegalArgumentException("Could not resolve genModels URI");
        }

        Path root = Path.of(fileURI.toFileString());

        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".genmodel"))
                    .map(path -> attachToURI(uri, root, path))
                    .map(URI::toString)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Could not read from " + root, e);
        }
    }

    private URI attachToURI(URI base, Path root, Path file) {
        Path relative = root.relativize(file);

        URI uri = base;

        for (Path segment : relative) {
            uri = uri.appendSegment(segment.toString());
        }

        return uri;
    }
}
