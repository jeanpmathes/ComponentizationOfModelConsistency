package tools.vitruv.compmodelcons.generator;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.generator.*;
import tools.vitruv.compmodelcons.generator.backend.Generator;
import tools.vitruv.neojoin.Parser;

public class NeoJoinVitruvGenerator implements IGenerator {
    private final Parser parser;

    @Inject
    public NeoJoinVitruvGenerator(Parser parser) {
        this.parser = parser;
    }

    @Override
    public void doGenerate(Resource input, IFileSystemAccess fsa) {
        Parser.Result result = parser.parse(input.getURI());

        if (result instanceof Parser.Result.Success success) {
            Generator generator = new Generator(input.getURI().trimFileExtension().lastSegment(), success.aqr());
            fsa.generateFile(generator.getFileName(), generator.generate());
        }
    }
}
