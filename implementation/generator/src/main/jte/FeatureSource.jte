@import gg.jte.support.ForSupport
@import java.util.List
@import tools.vitruv.compmodelcons.generator.backend.description.FeatureSourceDescription
@import tools.vitruv.compmodelcons.views.operations.FeatureSource
@import tools.vitruv.dsls.common.JavaImportHelper

@param FeatureSourceDescription featureSource
@param JavaImportHelper imports
 
!{imports.typeRef(FeatureSource.class);} 
!{imports.typeRef(List.class);} 

new FeatureSource(
    new FeatureSource.Target(
        ${featureSource.targetIndex()},
        List.of(
            @for(var feature : ForSupport.of(featureSource.targetFeatures()))
                ${feature.get().getQualifiedFeatureAccessor()} @if(!feature.isLast()), @endif
            @endfor
        )
    )
)
