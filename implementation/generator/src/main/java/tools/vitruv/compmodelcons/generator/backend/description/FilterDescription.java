package tools.vitruv.compmodelcons.generator.backend.description;

public record FilterDescription(
    ExpressionDescription condition,
    OriginDescription origin
) implements OriginDescription {
}
