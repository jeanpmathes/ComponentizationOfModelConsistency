@import tools.vitruv.compmodelcons.generator.backend.description.FilterDescription
@import tools.vitruv.compmodelcons.views.operations.Filter
@import tools.vitruv.dsls.common.JavaImportHelper

@param FilterDescription filter
@param JavaImportHelper imports

!{imports.typeRef(Filter.class);}

new Filter(
    @template.Expression(expression = filter.condition(), imports = imports),
    @template.Origin(origin = filter.origin(), imports = imports)
)
