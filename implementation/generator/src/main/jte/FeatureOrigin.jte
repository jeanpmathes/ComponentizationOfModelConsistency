@import tools.vitruv.compmodelcons.generator.backend.description.FeatureOriginDescription
@import tools.vitruv.compmodelcons.generator.backend.description.FeatureSourceDescription
@import tools.vitruv.compmodelcons.generator.backend.description.FeatureTransformDescription
@import tools.vitruv.dsls.common.JavaImportHelper

@param FeatureOriginDescription featureOrigin
@param JavaImportHelper imports
 
@if(featureOrigin instanceof FeatureSourceDescription featureSource)
    @template.FeatureSource(featureSource = featureSource, imports = imports)
@elseif(featureOrigin instanceof FeatureTransformDescription featureTransform)
    @template.FeatureTransform(featureTransform = featureTransform, imports = imports)
@endif