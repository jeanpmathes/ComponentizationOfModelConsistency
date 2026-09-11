@import org.eclipse.emf.ecore.EObject
@import tools.vitruv.change.atomic.EChange
@import tools.vitruv.compmodelcons.generator.backend.description.OnPutDeclarationDescription
@import tools.vitruv.compmodelcons.views.PutContext
@import tools.vitruv.compmodelcons.views.bindings.OriginBinding
@import tools.vitruv.dsls.common.JavaImportHelper

@param OnPutDeclarationDescription onPutDeclaration
@param JavaImportHelper imports

!{imports.typeRef(EChange.class);}
!{imports.typeRef(EObject.class);}
!{imports.typeRef(OriginBinding.class);}
!{imports.typeRef(PutContext.class);}

protected void ${onPutDeclaration.getMethodName()}(EChange<EObject> change, OriginBinding oldBinding, OriginBinding newBinding, PutContext context) {
}
