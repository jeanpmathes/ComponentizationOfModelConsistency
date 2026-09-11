package tools.vitruv.compmodelcons.generator.backend.description;

public sealed interface DeclarationDescription
    permits SourceObjectFactoryDeclarationDescription, TransformDeclarationDescription,
                OnPutDeclarationDescription {
}
