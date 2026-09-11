package tools.vitruv.compmodelcons.generator.backend.description;

import org.eclipse.emf.codegen.ecore.genmodel.GenClass;

public record SourceDescription(
    GenClass sourceClass,
    SourceObjectFactoryDeclarationDescription sourceObjectFactoryDeclaration
) implements OriginDescription {
}
