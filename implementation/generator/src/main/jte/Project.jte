@import gg.jte.support.ForSupport
@import java.util.List
@import tools.vitruv.compmodelcons.generator.backend.description.ProjectDescription
@import tools.vitruv.compmodelcons.views.operations.Project
@import tools.vitruv.dsls.common.JavaImportHelper

@param ProjectDescription project
@param JavaImportHelper imports

!{imports.typeRef(Project.class);}
!{imports.typeRef(List.class);}

new Project(
    ${project.targetClass().getQualifiedClassifierAccessor()},
    @template.Origin(origin = project.origin(), imports = imports),
    List.of(
        @for(var feature : ForSupport.of(project.features()))
            @template.FeatureProject(featureProject = feature.get(), imports = imports) @if(!feature.isLast()), @endif
        @endfor
    ),
    this::${project.onPutDeclaration().getMethodName()}
)
