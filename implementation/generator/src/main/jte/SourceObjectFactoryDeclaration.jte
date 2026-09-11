@import org.eclipse.emf.ecore.EObject
@import tools.vitruv.compmodelcons.generator.backend.description.SourceObjectFactoryDeclarationDescription
@import tools.vitruv.dsls.common.JavaImportHelper

@param SourceObjectFactoryDeclarationDescription sourceObjectFactoryDeclaration
@param JavaImportHelper imports

!{imports.typeRef(EObject.class);}

protected EObject ${sourceObjectFactoryDeclaration.getMethodName()}(EObject viewObject) {
    return ${sourceObjectFactoryDeclaration.sourceClass().getGenPackage().getQualifiedEFactoryInstanceAccessor()}.create(${sourceObjectFactoryDeclaration.sourceClass().getQualifiedClassifierAccessor()});
}
