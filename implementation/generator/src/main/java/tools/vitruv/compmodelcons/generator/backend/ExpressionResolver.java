package tools.vitruv.compmodelcons.generator.backend;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.XExpression;

public interface ExpressionResolver {
  String getMethodName(XExpression expression);

  String getQualifiedMethodName(XExpression expression);

  EStructuralFeature getAccessedFeature(XExpression expression);

  default boolean isFeatureAccess(XExpression expression) {
    return getAccessedFeature(expression) != null;
  }
}
