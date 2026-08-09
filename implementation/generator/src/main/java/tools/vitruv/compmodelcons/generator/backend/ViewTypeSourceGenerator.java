package tools.vitruv.compmodelcons.generator.backend;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.change.ChangeSpecificationAwareViewType;
import tools.vitruv.compmodelcons.generator.tools.Metamodel;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.*;
import tools.vitruv.compmodelcons.views.conditions.ConjunctiveCondition;
import tools.vitruv.compmodelcons.views.conditions.FeatureCondition;
import tools.vitruv.compmodelcons.views.operations.*;
import tools.vitruv.dsls.common.JavaFileGenerator;
import tools.vitruv.dsls.common.JavaImportHelper;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.aqr.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class ViewTypeSourceGenerator {
    private final JavaImportHelper importHelper = new JavaImportHelper();
    private final List<String> declarations = new ArrayList<>();

    private final String          name;
    private final AQR             aqr;
    private final List<Metamodel> originMetamodels;
    private final Metamodel       viewtypeMetamodel;
    private final ExpressionResolver expressions;

    public ViewTypeSourceGenerator(String name, List<Metamodel> originMetamodels, Metamodel viewtypeMetamodel, AQR aqr, ExpressionResolver expressions) {
        this.name             = NamingGenerator.convertToPascalCase(name);
        this.aqr              = aqr;
        this.originMetamodels = originMetamodels;
        this.viewtypeMetamodel = viewtypeMetamodel;
        this.expressions      = expressions;
    }

    public String generate() {
        return JavaFileGenerator.generateClass(getImplementation(), getPackageName(), importHelper);
    }

    private CharSequence getImplementation() {
        StringBuilder builder = new StringBuilder();

        importHelper.typeRef(ChangeSpecificationAwareViewType.class);

        builder.append("public class ")
                .append(getClassName())
                .append(" extends ChangeSpecificationAwareViewType {\n");
        appendBody(builder);
        builder.append("}");

        return builder;
    }

    private void appendBody(StringBuilder builder) {
        declarations.clear();

        builder.append("    public static final String NAME = \"")
                .append(StringEscapeUtils.escapeJava(name))
                .append("\";\n");

        importHelper.typeRef(List.class);
        importHelper.typeRef(EPackage.class);

        builder.append("    private static final List<EPackage> originMetamodels = List.of(\n");
        for (int index = 0; index < originMetamodels.size(); index++) {
            if (index > 0) {
                builder.append(",\n");
            }
            builder.append("        ")
                    .append(originMetamodels.get(index)
                                    .getFullyQualifiedPackageInterfaceAccessor());
        }
        builder.append("\n");
        builder.append("    );\n");
        builder.append("    private static final EPackage viewtype = ")
                .append(viewtypeMetamodel.getFullyQualifiedPackageInterfaceAccessor())
                .append(";\n\n");

        builder.append("    public ").append(getClassName()).append("() {\n");
        builder.append("        super(NAME, originMetamodels, viewtype);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(Root.class);

        builder.append("    @Override\n");
        builder.append("    protected final Root createStructure() {\n");
        appendRootOperation(builder);
        builder.append("    }\n");

        for (String declaration : declarations) {
            builder.append(declaration).append("\n\n");
        }
    }

    private void appendRootOperation(StringBuilder builder) {
        importHelper.typeRef(Root.class);
        importHelper.typeRef(Optional.class);
        importHelper.typeRef(List.class);

        GenClass rootClass = viewtypeMetamodel.getGenClass(aqr.root().name());

        builder.append("        return new Root(\n");
        builder.append("            ")
                .append(rootClass.getQualifiedClassifierAccessor())
                .append(",\n");

        if (aqr.root().source() == null) {
            builder.append("            ").append("Optional.empty(),\n");
        } else {
            builder.append("            ").append("Optional.of(\n");
            appendProjectOperation(builder, 1, aqr.root());
            builder.append("            ),\n");
        }

        builder.append("            List.of(");
        boolean first = true;
        for (AQRFeature feature : aqr.root().features()) {
            if (feature instanceof AQRFeature.Reference reference &&
                    feature.kind() instanceof AQRFeature.Kind.Generate) {
                builder.append(first ? "\n" : ",\n");
                first = false;

                GenFeature containment = viewtypeMetamodel.getGenFeature(rootClass.getEcoreClass(),
                                                                         reference.name()
                );

                builder.append("                ").append("new Root.Target(\n");
                builder.append("                    ")
                        .append(containment.getQualifiedFeatureAccessor())
                        .append(",\n");
                appendProjectOperation(builder, 2, reference.type());
                builder.append("                )");
            }
        }
        if (!first) {
            builder.append("\n").append("            ");
        }
        builder.append(")\n");

        builder.append("        );\n");
    }

    private void appendProjectOperation(StringBuilder builder, int level, AQRTargetClass target) {
        importHelper.typeRef(Project.class);

        GenClass targetClass = viewtypeMetamodel.getGenClass(target.name());

        builder.append(indent(level)).append("new Project(\n");
        builder.append(indent(level + 1))
                .append(targetClass.getQualifiedClassifierAccessor())
                .append(",\n");
        appendQueryOperations(builder, level + 1, targetClass, Objects.requireNonNull(target.source()));

        List<AQRFrom> context = target.source().allFroms().toList();

        builder.append(",\n").append(indent(level + 1)).append("List.of(");
        boolean first = true;
        for (AQRFeature feature : target.features()) {
            if (feature.kind() instanceof AQRFeature.Kind.Generate) {
                continue;
            }

            builder.append(first ? "\n" : ",\n");
            first = false;

            appendFeatureProjectOperation(builder, level + 2, targetClass, feature, context);
        }
        if (!first) {
            builder.append("\n").append(indent(level + 1));
        }
        builder.append("),\n");

        String onPutMethodName = String.format("onPut_%s", target.name());
        builder.append(indent(level + 1)).append("this::").append(onPutMethodName).append("\n");

        StringBuilder declaration = new StringBuilder();

        importHelper.typeRef(EChange.class);
        importHelper.typeRef(EObject.class);
        importHelper.typeRef(OriginBinding.class);
        importHelper.typeRef(PutContext.class);

        declaration.append("    protected void ")
                .append(onPutMethodName)
                .append("(EChange<EObject> change, OriginBinding oldBinding, OriginBinding newBinding, PutContext context) {\n");
        declaration.append("    }");

        declarations.add(declaration.toString());

        builder.append(indent(level)).append(")\n");
    }

    private void appendQueryOperations(StringBuilder builder, int level, GenClass targetClass, AQRSource source) {
        if (source.condition() != null) {
            importHelper.typeRef(Filter.class);

            builder.append(indent(level)).append("new Filter(\n");
            appendExpression(builder, level + 1, source.condition(), source.allFroms().toList());
            builder.append(",\n");

            appendQueryOperations(builder, level + 1, targetClass, source, source.joins().size() - 1);
            builder.append("\n");

            builder.append(indent(level)).append(")");
        } else {
            appendQueryOperations(builder, level, targetClass, source, source.joins().size() - 1);
        }
    }

    private void appendQueryOperations(StringBuilder builder, int level, GenClass targetClass, AQRSource source, int joinIndex) {
        if (joinIndex < 0) {
            appendSourceOperation(builder, level, targetClass, source.from());
        } else {
            importHelper.typeRef(Join.class);

            int fromIndex = joinIndex + 1; // The first 'from' element is not included in the joins.

            AQRJoin join = source.joins().get(joinIndex);
            List<AQRFrom> froms = source.allFroms().limit(fromIndex + 1).toList();

            Metamodel originMetamodel = getOriginMetamodel(join.from().clazz().getEPackage());
            GenClass sourceClass = originMetamodel.getGenClass(join.from().clazz());

            String factoryMethodName = getSourceObjectFactoryMethodName(sourceClass, targetClass, joinIndex + 1);

            builder.append(indent(level)).append("new Join(\n");
            builder.append(indent(level + 1)).append(sourceClass.getQualifiedClassifierAccessor()).append(",\n");
            builder.append(indent(level + 1)).append("this::").append(factoryMethodName).append(",\n");
            appendQueryOperations(builder, level + 1, targetClass, source, joinIndex - 1);
            builder.append(",\n");

            createSourceObjectFactoryMethod(factoryMethodName, sourceClass);

            builder.append(indent(level + 1));
            switch (join.type()) {
                case Inner -> builder.append("Join.Type.INNER");
                case Left -> builder.append("Join.Type.LEFT");
            }
            builder.append(",\n");

            importHelper.typeRef(ConjunctiveCondition.class);

            builder.append(indent(level + 1));
            builder.append("new ConjunctiveCondition(");
            boolean first = true;
            for (var featureCondition : join.featureConditions()) {
                final int leftIndex = featureCondition.otherIndex();
                final int rightIndex = fromIndex;

                EClass    leftClass     = froms.get(leftIndex).clazz();
                Metamodel leftMetamodel = getOriginMetamodel(leftClass.getEPackage());
                EClass    rightClass    = froms.get(rightIndex).clazz();
                Metamodel rightMetamodel = getOriginMetamodel(rightClass.getEPackage());

                importHelper.typeRef(FeatureCondition.class);

                for (String feature : featureCondition.features()) {
                    if (!first) {
                        builder.append(",");
                    }
                    first = false;

                    builder.append("\n");

                    GenFeature leftFeatureGen =
                            leftMetamodel.getGenFeature(leftClass.getEStructuralFeature(feature));
                    GenFeature rightFeatureGen =
                            rightMetamodel.getGenFeature(rightClass.getEStructuralFeature(feature));

                    builder.append(indent(level + 2)).append("new FeatureCondition(");
                    builder.append(indent(level + 3)).append(leftIndex).append(",\n");
                    builder.append(indent(level + 3))
                            .append(leftFeatureGen.getQualifiedFeatureAccessor())
                            .append(",\n");
                    builder.append(indent(level + 3)).append(rightIndex).append(",\n");
                    builder.append(indent(level + 3))
                            .append(rightFeatureGen.getQualifiedFeatureAccessor())
                            .append("\n");
                    builder.append(indent(level + 2)).append(")");
                }
            }
            for (XExpression expression : join.expressionConditions()) {
                if (!first) {
                    builder.append(",\n");
                }
                first = false;

                appendExpression(builder, level + 2, expression, froms);
            }
            if (!first) {
                builder.append("\n").append(indent(level + 1));
            }
            builder.append(")\n");

            builder.append(indent(level)).append(")");
        }
    }

    private void appendSourceOperation(StringBuilder builder, int level, GenClass targetClass, AQRFrom from) {
        Metamodel originMetamodel = getOriginMetamodel(from.clazz().getEPackage());
        GenClass sourceClass = originMetamodel.getGenClass(from.clazz());

        importHelper.typeRef(Source.class);

        String factoryMethodName = getSourceObjectFactoryMethodName(sourceClass, targetClass, 0);

        builder.append(indent(level)).append("new Source(\n");
        builder.append(indent(level + 1)).append(sourceClass.getQualifiedClassifierAccessor()).append(",\n");
        builder.append(indent(level + 1)).append("this::").append(factoryMethodName).append("\n");
        builder.append(indent(level)).append(")");

        createSourceObjectFactoryMethod(factoryMethodName, sourceClass);
    }

    private String getSourceObjectFactoryMethodName(GenClass sourceClass, GenClass targetClass, int index) {
        return "create" + sourceClass.getInterfaceName() + "For" + targetClass.getInterfaceName() + index;
    }

    private void createSourceObjectFactoryMethod(String methodName, GenClass sourceClass) {

        String declaration =
                "    protected EObject " + methodName + "(EObject viewObject) {\n" +
                        "        return " + sourceClass.getGenPackage().getQualifiedEFactoryInstanceAccessor() +
                        ".create(" + sourceClass.getQualifiedClassifierAccessor() + ");\n" +
                        "    }";

        declarations.add(declaration);
    }

    private void appendFeatureProjectOperation(StringBuilder builder, int level, GenClass targetClass, AQRFeature feature, List<AQRFrom> context) {
        GenFeature createdFeature =
                viewtypeMetamodel.getGenFeature(targetClass.getEcoreClass(), feature.name());

        importHelper.typeRef(FeatureProject.class);
        importHelper.typeRef(Optional.class);

        FeatureSource.Target target = null;

        builder.append(indent(level)).append("new FeatureProject(\n");
        if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
            target = copy.expression() != null
                     ? getTargetFromExpression(copy.expression(), context)
                     : getTargetFromFeature(copy.source(), context);
        }
        if (target != null) {
            builder.append(indent(level + 1))
                    .append("Optional.of(")
                    .append(target.index())
                    .append("),\n");
        } else {
            builder.append(indent(level + 1)).append("Optional.empty()").append(",\n");
        }
        builder.append(indent(level + 1))
                .append(createdFeature.getQualifiedFeatureAccessor())
                .append(",\n");
        if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
            if (target != null) {
                appendFeatureSourceOperation(builder, level + 1, target);
            } else {
                appendFeatureTransformOperation(builder, level + 1, copy.expression(), context);
            }
        } else if (feature.kind() instanceof AQRFeature.Kind.Calculate(XExpression expression)) {
            appendFeatureTransformOperation(builder, level + 1, expression, context);
        } else {
            throw new UnsupportedOperationException();
        }
        builder.append("\n");
        builder.append(indent(level)).append(")");
    }

    private void appendFeatureSourceOperation(StringBuilder builder, int level, FeatureSource.Target target) {
        importHelper.typeRef(FeatureSource.class);
        importHelper.typeRef(List.class);

        assert target != null;

        builder.append(indent(level)).append("new FeatureSource(\n");
        builder.append(indent(level + 1)).append("new FeatureSource.Target(\n");
        builder.append(indent(level + 2)).append(target.index()).append(",\n");
        builder.append(indent(level + 2)).append("List.of(\n");
        boolean first = true;
        for (EStructuralFeature current : target.features()) {
            if (!first) {
                builder.append(",\n");
            }
            first = false;

            GenFeature feature =
                    getOriginMetamodel(current.getEContainingClass().getEPackage()).getGenFeature(
                            current);

            builder.append(indent(level + 3)).append(feature.getQualifiedFeatureAccessor());
        }
        builder.append("\n").append(indent(level + 2)).append(")\n");
        builder.append(indent(level + 1)).append(")\n");
        builder.append(indent(level)).append(")");
    }

    private void appendFeatureTransformOperation(StringBuilder builder, int level, XExpression expression, List<AQRFrom> context) {
        importHelper.typeRef(FeatureTransform.class);

        String name           = expressions.getMethodName(expression);
        String expressionName = "EXPRESSION_" + name;
        String doGetName      = "doGet_" + name;
        String doPutName      = "doPut_" + name;
        String doUpdatingGetName = "doUpdatingGet_" + name;

        builder.append(indent(level)).append("new FeatureTransform(\n");
        builder.append(indent(level + 1)).append("this::").append(doGetName).append(",\n");
        builder.append(indent(level + 1)).append("this::").append(doPutName).append(",\n");
        builder.append(indent(level + 1)).append("this::").append(doUpdatingGetName).append("\n");
        builder.append(indent(level)).append(")");

        StringBuilder declaration = new StringBuilder();

        importHelper.typeRef(Function.class);
        importHelper.typeRef(OriginBinding.class);

        declaration.append("    protected static final Function<OriginBinding, Object> ")
                .append(expressionName)
                .append(" = ");
        appendExpression(declaration, 2, expression, context);
        declaration.append(";\n\n");

        importHelper.typeRef(FeatureBinding.class);
        importHelper.typeRef(ObjectBinding.class);
        importHelper.typeRef(GetContext.class);
        importHelper.typeRef(ValueBinding.class);

        declaration.append("    protected FeatureBinding ")
                .append(doGetName)
                .append("(ObjectBinding subjectBinding, GetContext context) {\n");
        declaration.append(
                        "        return FeatureBinding.ofOriginBinding(subjectBinding, ValueBinding.ofDynamic(")
                .append(expressionName)
                .append(".apply(subjectBinding)));\n");
        declaration.append("    }\n\n");

        importHelper.typeRef(FeatureBinding.class);
        importHelper.typeRef(EChange.class);
        importHelper.typeRef(EObject.class);
        importHelper.typeRef(ObjectBinding.class);
        importHelper.typeRef(ValueUpdateBinding.class);
        importHelper.typeRef(PutContext.class);
        importHelper.typeRef(UnsupportedOperationException.class);

        declaration.append("    protected FeatureBinding ")
                .append(doPutName)
                .append("(EChange<EObject> viewChange, FeatureBinding feature, ObjectBinding subjectBinding, ValueUpdateBinding value, PutContext context) {\n");
        declaration.append("        throw new UnsupportedOperationException();\n");
        declaration.append("    }\n\n");

        importHelper.typeRef(FeatureBinding.class);
        importHelper.typeRef(ObjectBinding.class);
        importHelper.typeRef(EChange.class);
        importHelper.typeRef(GetContext.class);
        importHelper.typeRef(ValueBinding.class);

        declaration.append("    protected FeatureBinding ")
                .append(doUpdatingGetName)
                .append("(FeatureBinding previous, ObjectBinding subjectBinding, EChange<EObject> originChange, GetContext context) {\n");
        declaration.append(
                        "        return FeatureBinding.ofOriginBinding(subjectBinding, ValueBinding.ofDynamic(")
                .append(expressionName)
                .append(".apply(subjectBinding)));\n");
        declaration.append("    }");

        declarations.add(declaration.toString());
    }

    private void appendExpression(StringBuilder builder, int level, XExpression expression, List<AQRFrom> parameters) {
        builder.append("originBinding -> {\n");
        builder.append(indent(level + 1))
                .append("var originObjects = originBinding.originObjects();\n");
        builder.append(indent(level + 1))
                .append("return ")
                .append(expressions.getQualifiedMethodName(expression))
                .append("(\n");

        boolean indexAlwaysZero = false;
        if (parameters.size() == 1 && parameters.getFirst().alias() != null) {
            // NeoJoin adds 'it' as the first parameter if there is only one parameter, even if there is an alias.
            // The alias then becomes a second parameter.
            parameters = new ArrayList<>(parameters);
            parameters.addFirst(parameters.getFirst());
            indexAlwaysZero = true;
        }

        for (int index = 0; index < parameters.size(); index++) {
            if (index > 0) {
                builder.append(",\n");
            }

            AQRFrom parameter = parameters.get(index);
            GenClass parameterClass = getOriginMetamodel(parameter.clazz()
                                                                 .getEPackage()).getGenClass(
                    parameter.clazz());

            builder.append(indent(level + 2))
                    .append("(")
                    .append(parameterClass.getQualifiedInterfaceName())
                    .append(") originObjects.get(")
                    .append(indexAlwaysZero ? 0 : index)
                    .append(")");
        }

        builder.append("\n");
        builder.append(indent(level + 1)).append(");\n");
        builder.append(indent(level)).append("}");
    }

    private FeatureSource.Target getTargetFromExpression(XExpression expression, List<AQRFrom> parameters) {
        List<EStructuralFeature> features = new ArrayList<>();

        XExpression current = expression;

        while (current instanceof XAbstractFeatureCall featureCall) {
            EStructuralFeature feature = expressions.getAccessedFeature(featureCall);

            if (feature != null) {
                features.add(feature);
                current = featureCall.getActualReceiver();
                continue;
            }

            if (featureCall.getFeature() instanceof JvmFormalParameter formalParameter) {
                for (int index = 0; index < parameters.size(); index++) {
                    AQRFrom parameter = parameters.get(index);

                    if (Objects.equals(formalParameter.getName(), parameter.alias()) ||
                            (parameters.size() == 1 &&
                                    formalParameter.getName()
                                            .equals(Constants.ExpressionSelfReference)
                            )) {
                        return new FeatureSource.Target(index, features.reversed());
                    }
                }
            }
            break;
        }
        return null;
    }

    private FeatureSource.Target getTargetFromFeature(EStructuralFeature feature, List<AQRFrom> parameters) {
        EClass eClass = feature.getEContainingClass();

        for (int index = 0; index < parameters.size(); index++) {
            AQRFrom parameter = parameters.get(index);
            if (eClass.equals(parameter.clazz())) {
                return new FeatureSource.Target(index, List.of(feature));
            }
        }

        return null;
    }

    private Metamodel getOriginMetamodel(EPackage ePackage) {
        return originMetamodels.stream()
                .filter(metamodel -> metamodel.ePackage().equals(ePackage))
                .findAny()
                .orElseThrow();
    }

    private String indent(int indent) {
        return "    ".repeat(indent + 3);
    }

    public String getFileName() {
        return String.format("%s/%s%s",
                             NamingGenerator.getPackagePath(aqr),
                             getClassName(),
                             JavaFileGenerator.JAVA_FILE_EXTENSION
        );
    }

    private String getPackageName() {
        return NamingGenerator.getPackageName(aqr);
    }

    private String getClassName() {
        return String.format("%sViewType", name);
    }
}
