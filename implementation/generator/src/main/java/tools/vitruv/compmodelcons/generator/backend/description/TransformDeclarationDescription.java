package tools.vitruv.compmodelcons.generator.backend.description;

public record TransformDeclarationDescription(
    String expressionMethodName,
    ExpressionDescription expression
) implements DeclarationDescription {
  public String getGetMethodName() {
    return "doGet_" + expressionMethodName;
  }

  public String getPutMethodName() {
    return "doPut_" + expressionMethodName;
  }

  public String getExpressionName() {
    return "EXPRESSION_" + expressionMethodName;
  }
}
