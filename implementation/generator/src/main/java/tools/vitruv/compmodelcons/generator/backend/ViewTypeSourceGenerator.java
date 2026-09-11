package tools.vitruv.compmodelcons.generator.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenFeature;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import tools.vitruv.compmodelcons.generator.backend.description.DeclarationDescription;
import tools.vitruv.compmodelcons.generator.backend.description.ExpressionDescription;
import tools.vitruv.compmodelcons.generator.backend.description.FeatureOriginDescription;
import tools.vitruv.compmodelcons.generator.backend.description.FeatureProjectDescription;
import tools.vitruv.compmodelcons.generator.backend.description.FeatureSourceDescription;
import tools.vitruv.compmodelcons.generator.backend.description.FeatureTransformDescription;
import tools.vitruv.compmodelcons.generator.backend.description.FilterDescription;
import tools.vitruv.compmodelcons.generator.backend.description.JoinDescription;
import tools.vitruv.compmodelcons.generator.backend.description.OnPutDeclarationDescription;
import tools.vitruv.compmodelcons.generator.backend.description.OriginDescription;
import tools.vitruv.compmodelcons.generator.backend.description.ProjectDescription;
import tools.vitruv.compmodelcons.generator.backend.description.RootDescription;
import tools.vitruv.compmodelcons.generator.backend.description.SourceDescription;
import tools.vitruv.compmodelcons.generator.backend.description.SourceObjectFactoryDeclarationDescription;
import tools.vitruv.compmodelcons.generator.backend.description.TransformDeclarationDescription;
import tools.vitruv.compmodelcons.generator.backend.description.ViewTypeDescription;
import tools.vitruv.compmodelcons.generator.tools.Metamodel;
import tools.vitruv.compmodelcons.generator.tools.NamingGenerator;
import tools.vitruv.compmodelcons.views.operations.FeatureSource;
import tools.vitruv.compmodelcons.views.operations.Join;
import tools.vitruv.dsls.common.JavaFileGenerator;
import tools.vitruv.dsls.common.JavaImportHelper;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRFeature;
import tools.vitruv.neojoin.aqr.AQRFrom;
import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.aqr.AQRSource;
import tools.vitruv.neojoin.aqr.AQRTargetClass;

/**
 * Generates an operation-based view type based on a provided NeoJoin AQR.
 */
public class ViewTypeSourceGenerator {
  private final String name;
  private final AQR aqr;
  private final List<Metamodel> originMetamodels;
  private final Metamodel viewtypeMetamodel;
  private final ExpressionResolver expressions;

  public ViewTypeSourceGenerator(
      String name, List<Metamodel> originMetamodels, Metamodel viewtypeMetamodel, AQR aqr,
      ExpressionResolver expressions) {
    this.name = NamingGenerator.convertToPascalCase(name);
    this.aqr = aqr;
    this.originMetamodels = originMetamodels;
    this.viewtypeMetamodel = viewtypeMetamodel;
    this.expressions = expressions;
  }

  public String generate() {
    Context context = new Context(new ArrayList<>(), new JavaImportHelper());

    String implementation = generateImplementation(context);
    String code =
        JavaFileGenerator.generateClass(implementation, getPackageName(), context.importHelper());

    code = format(code);

    return code;
  }

  private String format(String code) {
    Map<String, String> options = DefaultCodeFormatterConstants.getEclipse21Settings();
    options.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES,
                DefaultCodeFormatterConstants.TRUE);
    options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "0");
    options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "120");

    CodeFormatter formatter = ToolFactory.createCodeFormatter(options);

    TextEdit edit = formatter.format(
        CodeFormatter.K_COMPILATION_UNIT, code, 0, code.length(), 0, null);

    try {
      Document document = new Document(code);
      edit.apply(document);
      return document.get();
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  private String generateImplementation(Context context) {
    ViewTypeDescription viewTypeDescription = createViewTypeDescription(context);
    return ViewTypeGenerator.INSTANCE.generate(viewTypeDescription, context.importHelper());
  }

  private ViewTypeDescription createViewTypeDescription(Context context) {
    return new ViewTypeDescription(getClassName(), name, originMetamodels, viewtypeMetamodel,
                                   createRootDescription(context), context.declarations);
  }

  private RootDescription createRootDescription(Context context) {
    GenClass rootClass = viewtypeMetamodel.getGenClass(aqr.root().name());

    Optional<ProjectDescription> rootProject;
    if (aqr.root().source() == null) {
      rootProject = Optional.empty();
    } else {
      rootProject = Optional.of(createProjectDescription(aqr.root(), context));
    }

    List<RootDescription.Contained> contained = new ArrayList<>();
    for (AQRFeature feature : aqr.root().features()) {
      if (feature instanceof AQRFeature.Reference reference
              && feature.kind() instanceof AQRFeature.Kind.Generate) {
        GenFeature containment =
            viewtypeMetamodel.getGenFeature(rootClass.getEcoreClass(), reference.name());

        contained.add(new RootDescription.Contained(containment,
                                                    createProjectDescription(reference.type(),
                                                                             context)));
      }
    }

    return new RootDescription(rootClass, rootProject, contained);
  }

  private ProjectDescription createProjectDescription(AQRTargetClass target, Context context) {
    GenClass targetClass = viewtypeMetamodel.getGenClass(target.name());
    OriginDescription origin =
        createOriginDescription(targetClass, Objects.requireNonNull(target.source()), context);

    List<AQRFrom> parameters = target.source().allFroms().toList();

    List<FeatureProjectDescription> featureProjects = new ArrayList<>();
    for (AQRFeature feature : target.features()) {
      if (feature.kind() instanceof AQRFeature.Kind.Generate) {
        continue;
      }

      featureProjects.add(
          createFeatureProjectDescription(targetClass, feature, parameters, context));
    }

    OnPutDeclarationDescription onPutDeclaration =
        context.add(new OnPutDeclarationDescription(target.name()));

    return new ProjectDescription(targetClass, origin, featureProjects, onPutDeclaration);
  }

  private OriginDescription createOriginDescription(
      GenClass targetClass, AQRSource source, Context context) {
    if (source.condition() != null) {
      return new FilterDescription(
          createExpressionDescription(source.condition(), source.allFroms().toList(), context),
          createOriginDescription(targetClass, source, source.joins().size() - 1, context));
    } else {
      return createOriginDescription(targetClass, source, source.joins().size() - 1, context);
    }
  }

  private OriginDescription createOriginDescription(
      GenClass targetClass, AQRSource source,
      int joinIndex, Context context) {
    if (joinIndex < 0) {
      return createSourceDescription(targetClass, source.from(), context);
    } else {
      int fromIndex = joinIndex + 1; // The first 'from' element is not included in the joins.

      AQRJoin join = source.joins().get(joinIndex);
      List<AQRFrom> parameters = source.allFroms().limit(fromIndex + 1).toList();

      Metamodel originMetamodel = getOriginMetamodel(join.from().clazz().getEPackage());
      GenClass sourceClass = originMetamodel.getGenClass(join.from().clazz());

      SourceObjectFactoryDeclarationDescription sourceObjectFactoryDeclaration = context.add(
          new SourceObjectFactoryDeclarationDescription(sourceClass, targetClass, joinIndex + 1));

      OriginDescription origin =
          createOriginDescription(targetClass, source, joinIndex - 1, context);

      Join.Type type = switch (join.type()) {
        case Inner -> Join.Type.INNER;
        case Left -> Join.Type.LEFT;
      };

      List<JoinDescription.FeatureCondition> featureConditions = new ArrayList<>();
      for (var featureCondition : join.featureConditions()) {
        final int leftIndex = featureCondition.otherIndex();
        final int rightIndex = fromIndex;

        EClass leftClass = parameters.get(leftIndex).clazz();
        Metamodel leftMetamodel = getOriginMetamodel(leftClass.getEPackage());
        EClass rightClass = parameters.get(rightIndex).clazz();
        Metamodel rightMetamodel = getOriginMetamodel(rightClass.getEPackage());

        for (String feature : featureCondition.features()) {
          GenFeature leftFeature =
              leftMetamodel.getGenFeature(leftClass.getEStructuralFeature(feature));
          GenFeature rightFeature =
              rightMetamodel.getGenFeature(rightClass.getEStructuralFeature(feature));

          featureConditions.add(
              new JoinDescription.FeatureCondition(leftIndex, leftFeature, rightIndex,
                                                   rightFeature));
        }
      }

      List<ExpressionDescription> expressionConditions = new ArrayList<>();
      for (XExpression expression : join.expressionConditions()) {
        expressionConditions.add(createExpressionDescription(expression, parameters, context));
      }

      return new JoinDescription(sourceClass, sourceObjectFactoryDeclaration, origin, type,
                                 featureConditions, expressionConditions);
    }
  }

  private SourceDescription createSourceDescription(
      GenClass targetClass, AQRFrom from,
      Context context) {
    Metamodel originMetamodel = getOriginMetamodel(from.clazz().getEPackage());
    GenClass sourceClass = originMetamodel.getGenClass(from.clazz());

    SourceObjectFactoryDeclarationDescription sourceObjectFactoryDeclaration =
        context.add(new SourceObjectFactoryDeclarationDescription(sourceClass, targetClass, 0));

    return new SourceDescription(sourceClass, sourceObjectFactoryDeclaration);
  }

  private FeatureProjectDescription createFeatureProjectDescription(
      GenClass targetClass,
      AQRFeature feature,
      List<AQRFrom> parameters,
      Context context) {
    FeatureSource.Target target = null;

    if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
      target = copy.expression() != null ? getTargetFromExpression(copy.expression(), parameters)
                                         : getTargetFromFeature(copy.source(), parameters);
    }

    GenFeature createdFeature =
        viewtypeMetamodel.getGenFeature(targetClass.getEcoreClass(), feature.name());

    FeatureOriginDescription origin;
    if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
      if (target != null) {
        origin = createFeatureSourceDescription(target);
      } else {
        origin = createFeatureTransformDescription(copy.expression(), parameters, context);
      }
    } else if (feature.kind() instanceof AQRFeature.Kind.Calculate(XExpression expression)) {
      origin = createFeatureTransformDescription(expression, parameters, context);
    } else {
      throw new UnsupportedOperationException();
    }

    return new FeatureProjectDescription(
        Optional.ofNullable(target).map(FeatureSource.Target::index), createdFeature, origin);
  }

  private FeatureSourceDescription createFeatureSourceDescription(
      FeatureSource.Target target) {
    assert target != null;

    return new FeatureSourceDescription(target.index(), target.features()
                                                            .stream()
                                                            .map(feature -> getOriginMetamodel(
                                                                feature.getEContainingClass()
                                                                    .getEPackage()).getGenFeature(
                                                                feature))
                                                            .toList());
  }

  private FeatureTransformDescription createFeatureTransformDescription(
      XExpression expression,
      List<AQRFrom> parameters,
      Context context) {
    return new FeatureTransformDescription(context.add(
        new TransformDeclarationDescription(expressions.getMethodName(expression),
                                            createExpressionDescription(expression, parameters,
                                                                        context))));
  }

  private ExpressionDescription createExpressionDescription(
      XExpression expression,
      List<AQRFrom> parameters,
      Context context) {
    boolean indexAlwaysZero = false;
    if (parameters.size() == 1 && parameters.getFirst().alias() != null) {
      // NeoJoin adds 'it' as the first parameter if there is only one parameter, even if there
      // is an alias.
      // The alias then becomes a second parameter.
      parameters = new ArrayList<>(parameters);
      parameters.addFirst(parameters.getFirst());
      indexAlwaysZero = true;
    }

    List<ExpressionDescription.Parameter> arguments = new ArrayList<>();
    for (int index = 0; index < parameters.size(); index++) {
      AQRFrom parameter = parameters.get(index);
      GenClass parameterClass =
          getOriginMetamodel(parameter.clazz().getEPackage()).getGenClass(parameter.clazz());

      arguments.add(
          new ExpressionDescription.Parameter(parameterClass, indexAlwaysZero ? 0 : index));
    }

    return new ExpressionDescription(expressions.getQualifiedMethodName(expression), arguments);
  }

  private FeatureSource.Target getTargetFromExpression(
      XExpression expression,
      List<AQRFrom> parameters) {
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

          if (Objects.equals(formalParameter.getName(), parameter.alias()) || (
              parameters.size() == 1 && formalParameter.getName()
                                            .equals(Constants.ExpressionSelfReference))) {
            return new FeatureSource.Target(index, features.reversed());
          }
        }
      }
      break;
    }
    return null;
  }

  private FeatureSource.Target getTargetFromFeature(
      EStructuralFeature feature,
      List<AQRFrom> parameters) {
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

  public String getFileName() {
    return String.format("%s/%s%s", NamingGenerator.getPackagePath(aqr), getClassName(),
                         JavaFileGenerator.JAVA_FILE_EXTENSION);
  }

  private String getClassName() {
    return String.format("%sViewType", name);
  }

  private String getPackageName() {
    return NamingGenerator.getPackageName(aqr);
  }

  private record Context(List<DeclarationDescription> declarations, JavaImportHelper importHelper) {
    public <T extends DeclarationDescription> T add(T declaration) {
      declarations.add(declaration);
      return declaration;
    }
  }
}
