package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.compiler.IGeneratorConfigProvider;
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.jvmmodel.ILogicalContainerProvider;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;

public class ViewTypeExpressionSourceGenerator implements ExpressionResolver {
    private final ExpressionHelper expressionHelper;

    private final JvmModelGenerator jvmModelGenerator;
    private final IGeneratorConfigProvider generatorConfigProvider;
    private final ILogicalContainerProvider logicalContainerProvider;

    private final JvmGenericType expressionHoldingType;
    private final String fileName;

    public ViewTypeExpressionSourceGenerator(
            ExpressionHelper expressionHelper,
            ViewTypeDefinition viewTypeDefinition,
            JvmModelGenerator jvmModelGenerator,
            IGeneratorConfigProvider generatorConfigProvider,
            IJvmModelAssociations jvmModelAssociations,
            ILogicalContainerProvider logicalContainerProvider
                                            ) {
        this.expressionHelper = expressionHelper;

        this.jvmModelGenerator = jvmModelGenerator;
        this.generatorConfigProvider = generatorConfigProvider;
        this.logicalContainerProvider = logicalContainerProvider;

        this.expressionHoldingType = (JvmGenericType) jvmModelAssociations.getPrimaryJvmElement(viewTypeDefinition);
        this.fileName = this.expressionHoldingType.getQualifiedName().replace('.', '/') + ".java";
    }

    public String generate() {
        return jvmModelGenerator.generateType(expressionHoldingType, generatorConfigProvider.get(expressionHoldingType))
                                .toString();
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMethodName(XExpression expression) {
        return logicalContainerProvider.getLogicalContainer(expression).getSimpleName();
    }

    @Override
    public String getQualifiedMethodName(XExpression expression) {
        return expressionHoldingType.getSimpleName() + "." + getMethodName(expression);
    }

    @Override
    public EStructuralFeature getAccessedFeature(XExpression expression) {
        try {
            return expressionHelper.getFeatureOrNull(expression);
        } catch (TypeResolutionException e) {
            return null;
        }
    }
}
