@import tools.vitruv.compmodelcons.generator.backend.description.SourceDescription
@import tools.vitruv.compmodelcons.views.operations.Source
@import tools.vitruv.dsls.common.JavaImportHelper

@param SourceDescription source
@param JavaImportHelper imports

!{imports.typeRef(Source.class);}

new Source(
    ${source.sourceClass().getQualifiedClassifierAccessor()},
    this::${source.sourceObjectFactoryDeclaration().getMethodName()}
)
