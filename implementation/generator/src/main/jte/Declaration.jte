@import tools.vitruv.compmodelcons.generator.backend.description.DeclarationDescription
@import tools.vitruv.compmodelcons.generator.backend.description.OnPutDeclarationDescription
@import tools.vitruv.compmodelcons.generator.backend.description.SourceObjectFactoryDeclarationDescription
@import tools.vitruv.compmodelcons.generator.backend.description.TransformDeclarationDescription
@import tools.vitruv.dsls.common.JavaImportHelper

@param DeclarationDescription declaration
@param JavaImportHelper imports
 
@if(declaration instanceof SourceObjectFactoryDeclarationDescription sourceObjectFactoryDeclaration)
    @template.SourceObjectFactoryDeclaration(sourceObjectFactoryDeclaration = sourceObjectFactoryDeclaration, imports = imports)
@elseif(declaration instanceof TransformDeclarationDescription transformDeclaration)
    @template.TransformDeclaration(transformDeclaration = transformDeclaration, imports = imports)
@elseif(declaration instanceof OnPutDeclarationDescription onPutDeclaration)
    @template.OnPutDeclaration(onPutDeclaration = onPutDeclaration, imports = imports)
@endif
