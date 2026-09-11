@import java.util.function.Function
@import org.eclipse.emf.ecore.EObject
@import tools.vitruv.change.atomic.EChange
@import tools.vitruv.compmodelcons.generator.backend.description.TransformDeclarationDescription
@import tools.vitruv.compmodelcons.views.GetContext
@import tools.vitruv.compmodelcons.views.PutContext
@import tools.vitruv.compmodelcons.views.bindings.FeatureOriginBinding
@import tools.vitruv.compmodelcons.views.bindings.ObjectBinding
@import tools.vitruv.compmodelcons.views.bindings.OriginBinding
@import tools.vitruv.compmodelcons.views.bindings.ValueBinding
@import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding
@import tools.vitruv.dsls.common.JavaImportHelper

@param TransformDeclarationDescription transformDeclaration
@param JavaImportHelper imports

!{imports.typeRef(Function.class);}
!{imports.typeRef(OriginBinding.class);}

protected static final Function<OriginBinding, Object> ${transformDeclaration.getExpressionName()} = @template.Expression(expression = transformDeclaration.expression(), imports = imports);

!{imports.typeRef(FeatureOriginBinding.class);}
!{imports.typeRef(ObjectBinding.class);}
!{imports.typeRef(GetContext.class);}
!{imports.typeRef(ValueBinding.class);}

protected FeatureOriginBinding ${transformDeclaration.getGetMethodName()}(ObjectBinding subjectBinding, GetContext context) {
    return FeatureOriginBinding.ofOriginBinding(subjectBinding, ValueBinding.ofDynamic(
        ${transformDeclaration.getExpressionName()}.apply(subjectBinding)));
}

!{imports.typeRef(FeatureOriginBinding.class);}
!{imports.typeRef(EChange.class);}
!{imports.typeRef(EObject.class);}
!{imports.typeRef(ObjectBinding.class);}
!{imports.typeRef(ValueUpdateBinding.class);}
!{imports.typeRef(PutContext.class);}
!{imports.typeRef(UnsupportedOperationException.class);}

protected FeatureOriginBinding ${transformDeclaration.getPutMethodName()}(EChange<EObject> viewChange, FeatureOriginBinding feature, ObjectBinding subjectBinding, ValueUpdateBinding value, PutContext context) {
    throw new UnsupportedOperationException();
}
