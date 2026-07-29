package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.xtext.xbase.XExpression;

public interface ExpressionResolver {
    String resolve(XExpression expression);
}
