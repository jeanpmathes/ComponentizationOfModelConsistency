@import gg.jte.support.ForSupport
@import tools.vitruv.compmodelcons.generator.backend.description.ExpressionDescription
@import tools.vitruv.dsls.common.JavaImportHelper

@param ExpressionDescription expression
@param JavaImportHelper imports

originBinding -> {
    var originObjects = originBinding.originObjects();
    return ${expression.qualifiedMethodName()}(
        @for(var parameter : ForSupport.of(expression.parameters()))
            (${parameter.get().parameterClass().getQualifiedInterfaceName()}) originObjects.get(${parameter.get().index()})@if(!parameter.isLast()),@endif
        @endfor
    );
}
