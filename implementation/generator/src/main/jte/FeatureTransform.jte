@import tools.vitruv.compmodelcons.generator.backend.description.FeatureTransformDescription
@import tools.vitruv.compmodelcons.views.operations.FeatureTransform
@import tools.vitruv.dsls.common.JavaImportHelper

@param FeatureTransformDescription featureTransform
@param JavaImportHelper imports

!{imports.typeRef(FeatureTransform.class);}

new FeatureTransform(
    this::${featureTransform.transformDeclaration().getGetMethodName()},
    this::${featureTransform.transformDeclaration().getPutMethodName()}
)
