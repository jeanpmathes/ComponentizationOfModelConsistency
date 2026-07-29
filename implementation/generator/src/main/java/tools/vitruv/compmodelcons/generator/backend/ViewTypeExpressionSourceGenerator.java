package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.compiler.IGeneratorConfigProvider;
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.jvmmodel.ILogicalContainerProvider;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;

public class ViewTypeExpressionSourceGenerator implements ExpressionResolver {
    private final JvmModelGenerator jvmModelGenerator;
    private final IGeneratorConfigProvider generatorConfigProvider;
    private final ILogicalContainerProvider logicalContainerProvider;

    private final JvmGenericType expressionHoldingType;
    private final String fileName;

    public ViewTypeExpressionSourceGenerator(
            ViewTypeDefinition viewTypeDefinition,
            JvmModelGenerator jvmModelGenerator,
            IGeneratorConfigProvider generatorConfigProvider,
            IJvmModelAssociations jvmModelAssociations,
            ILogicalContainerProvider logicalContainerProvider
    ) {
        this.jvmModelGenerator = jvmModelGenerator;
        this.generatorConfigProvider = generatorConfigProvider;
        this.logicalContainerProvider = logicalContainerProvider;

        this.expressionHoldingType = (JvmGenericType) jvmModelAssociations.getPrimaryJvmElement(viewTypeDefinition);
        this.fileName = expressionHoldingType.getQualifiedName().replace('.', '/') + ".java";
    }

    public String generate() {
        return jvmModelGenerator.generateType(expressionHoldingType, generatorConfigProvider.get(expressionHoldingType)).toString();
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String resolve(XExpression expression) {
        JvmOperation operation = (JvmOperation) logicalContainerProvider.getLogicalContainer(expression);
        return expressionHoldingType.getSimpleName() + "." + operation.getSimpleName();
    }
}
