package tools.vitruv.compmodelcons.generator.backend;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.XExpression;
import tools.vitruv.compmodelcons.change.ChangeSpecificationAwareViewType;
import tools.vitruv.compmodelcons.generator.tools.Metamodel;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.compmodelcons.views.operations.*;
import tools.vitruv.dsls.common.JavaFileGenerator;
import tools.vitruv.dsls.common.JavaImportHelper;
import tools.vitruv.neojoin.aqr.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ViewTypeSourceGenerator {
    private final JavaImportHelper importHelper = new JavaImportHelper();

    private final String name;
    private final AQR aqr;
    private final List<Metamodel> originMetamodels;
    private final Metamodel viewtypeMetamodel;
    private final ExpressionResolver expressions;

    public ViewTypeSourceGenerator(String name, List<Metamodel> originMetamodels, Metamodel viewtypeMetamodel, AQR aqr, ExpressionResolver expressions) {
        this.name = NamingGenerator.convertToPascalCase(name);
        this.aqr = aqr;
        this.originMetamodels = originMetamodels;
        this.viewtypeMetamodel = viewtypeMetamodel;
        this.expressions = expressions;
    }

    public String generate() {
        return JavaFileGenerator.generateClass(getImplementation(), getPackageName(), importHelper);
    }

    private CharSequence getImplementation() {
        StringBuilder builder = new StringBuilder();

        importHelper.typeRef(ChangeSpecificationAwareViewType.class);

        builder.append("public class ").append(getClassName()).append(" extends ChangeSpecificationAwareViewType {\n");
        appendBody(builder);
        builder.append("}");

        return builder;
    }

    private void appendBody(StringBuilder builder) {
        builder.append("    public static final String NAME = \"").append(StringEscapeUtils.escapeJava(name)).append("\";\n");

        importHelper.typeRef(List.class);
        importHelper.typeRef(EPackage.class);

        builder.append("    private static final List<EPackage> originMetamodels = List.of(\n");
        for (int index = 0; index < originMetamodels.size(); index++) {
            if (index > 0) {
                builder.append(",\n");
            }
            builder.append("        ").append(originMetamodels.get(index).getFullyQualifiedPackageInterfaceAccessor());
        }
        builder.append("\n");
        builder.append("    );\n");
        builder.append("    private static final EPackage viewtype = ").append(viewtypeMetamodel.getFullyQualifiedPackageInterfaceAccessor()).append(";\n\n");

        builder.append("    public ").append(getClassName()).append("() {\n");
        builder.append("        super(NAME, originMetamodels, viewtype);\n");
        builder.append("    }\n\n");

        importHelper.typeRef(Root.class);

        builder.append("    @Override\n");
        builder.append("    protected Root createStructure() {\n");
        appendRootOperation(builder);
        builder.append("    }\n");
    }

    private void appendRootOperation(StringBuilder builder) {
        importHelper.typeRef(Root.class);
        importHelper.typeRef(Optional.class);
        importHelper.typeRef(List.class);

        GenClass rootClass = viewtypeMetamodel.getGenClass(aqr.root().name());

        builder.append("        return new Root(\n");
        builder.append("            ").append(rootClass.getQualifiedClassifierAccessor()).append(",\n");

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
            if (feature instanceof AQRFeature.Reference reference && feature.kind() instanceof AQRFeature.Kind.Generate) {
                builder.append(first ? "\n" : ",\n");
                first = false;

                GenFeature containment = viewtypeMetamodel.getGenFeature(rootClass.getEcoreClass(), reference.name());

                builder.append("                ").append("new Root.Target(\n");
                builder.append("                    ").append(containment.getQualifiedFeatureAccessor()).append(",\n");
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
        builder.append(indent(level + 1)).append(targetClass.getQualifiedClassifierAccessor()).append(",\n");
        appendQueryOperations(builder, level + 1, Objects.requireNonNull(target.source()));

        builder.append(",\n").append(indent(level + 1)).append("List.of(");
        boolean first = true;
        for (AQRFeature feature : target.features()) {
            if (feature.kind() instanceof AQRFeature.Kind.Generate) {
                continue;
            }

            builder.append(first ? "\n" : ",\n");
            first = false;

            appendFeatureProjectOperation(builder, level + 2, targetClass, feature);
        }
        if (!first) {
            builder.append("\n").append(indent(level + 1));
        }
        builder.append(")\n");

        builder.append(indent(level)).append(")\n");
    }

    private void appendQueryOperations(StringBuilder builder, int level, AQRSource source) {
        if (source.condition() != null) {
            importHelper.typeRef(Filter.class);

            builder.append(indent(level)).append("new Filter(\n");
            appendConditionExpression(builder, level + 1, source.condition(), source.allFroms().toList());
            builder.append(",\n");

            appendQueryOperations(builder, level + 1, source, 0);
            builder.append("\n");

            builder.append(indent(level)).append(")");
        } else {
            appendQueryOperations(builder, level, source, 0);
        }
    }

    private void appendQueryOperations(StringBuilder builder, int level, AQRSource source, int joinIndex) {
        if (joinIndex >= source.joins().size()) {
            appendSourceOperation(builder, level, source.from());
        } else {
            importHelper.typeRef(Join.class);

            AQRJoin join = source.joins().get(joinIndex);

            if (join.type() != AQRJoin.Type.Inner || !join.featureConditions().isEmpty() || !join.expressionConditions().isEmpty()) {
                throw new NotImplementedException();
            }

            Metamodel originMetamodel = getOriginMetamodel(join.from().clazz().getEPackage());
            GenClass sourceClass = originMetamodel.getGenClass(join.from().clazz());

            builder.append(indent(level)).append("new Join(\n");
            builder.append(indent(level + 1)).append(sourceClass.getQualifiedClassifierAccessor()).append(",\n");
            appendQueryOperations(builder, level + 1, source, joinIndex + 1);
            builder.append("\n");
            builder.append(indent(level)).append(")");
        }
    }

    private void appendSourceOperation(StringBuilder builder, int level, AQRFrom from) {
        Metamodel originMetamodel = getOriginMetamodel(from.clazz().getEPackage());
        GenClass sourceClass = originMetamodel.getGenClass(from.clazz());

        importHelper.typeRef(Source.class);

        builder.append(indent(level)).append("new Source(").append(sourceClass.getQualifiedClassifierAccessor()).append(")");
    }

    private void appendFeatureProjectOperation(StringBuilder builder, int level, GenClass targetClass, AQRFeature feature) {
        GenFeature createdFeature = viewtypeMetamodel.getGenFeature(targetClass.getEcoreClass(), feature.name());

        importHelper.typeRef(FeatureProject.class);
        importHelper.typeRef(Optional.class);

        builder.append(indent(level)).append("new FeatureProject(\n");
        if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
            Metamodel originMetamodel = getOriginMetamodel(copy.source().getEContainingClass().getEPackage());
            GenFeature sourceFeature = originMetamodel.getGenFeature(copy.source());

            builder.append(indent(level + 1)).append("Optional.of(").append(sourceFeature.getQualifiedFeatureAccessor()).append("),\n");
        } else {
            builder.append(indent(level + 1)).append("Optional.of()").append(",\n");
        }
        builder.append(indent(level + 1)).append(createdFeature.getQualifiedFeatureAccessor()).append(",\n");
        if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
            appendFeatureSourceOperation(builder, level + 1, copy);
            builder.append("\n");
        } else {
            throw new UnsupportedOperationException();
        }
        builder.append(indent(level)).append(")");
    }

    private void appendFeatureSourceOperation(StringBuilder builder, int level, AQRFeature.Kind.Copy copy) {
        Metamodel originMetamodel = getOriginMetamodel(copy.source().getEContainingClass().getEPackage());
        GenFeature sourceFeature = originMetamodel.getGenFeature(copy.source());

        importHelper.typeRef(FeatureSource.class);

        builder.append(indent(level)).append("new FeatureSource(").append(sourceFeature.getQualifiedFeatureAccessor()).append(")");
    }

    private void appendConditionExpression(StringBuilder builder, int level, XExpression expression, List<AQRFrom> parameters) {
        builder.append(indent(level)).append("originBinding -> {\n");
        builder.append(indent(level + 1)).append("var originObjects = originBinding.originObjects();\n");
        builder.append(indent(level + 1)).append("return ").append(expressions.resolve(expression)).append("(\n");

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
            GenClass parameterClass = getOriginMetamodel(parameter.clazz().getEPackage()).getGenClass(parameter.clazz());

            builder.append(indent(level + 2)).append("(").append(parameterClass.getQualifiedInterfaceName()).append(") originObjects.get(").append(indexAlwaysZero ? 0 : index).append(")");
        }

        builder.append("\n");
        builder.append(indent(level + 1)).append(");\n");
        builder.append(indent(level)).append("}");
    }

    private Metamodel getOriginMetamodel(EPackage ePackage) {
        return originMetamodels.stream().filter(metamodel -> metamodel.ePackage().equals(ePackage)).findAny().orElseThrow();
    }

    private String indent(int indent) {
        return "    ".repeat(indent + 3);
    }

    public String getFileName() {
        return String.format("%s/%s%s", NamingGenerator.getPackagePath(aqr), getClassName(), JavaFileGenerator.JAVA_FILE_EXTENSION);
    }

    private String getPackageName() {
        return NamingGenerator.getPackageName(aqr);
    }

    private String getClassName() {
        return String.format("%sViewType", name);
    }
}
