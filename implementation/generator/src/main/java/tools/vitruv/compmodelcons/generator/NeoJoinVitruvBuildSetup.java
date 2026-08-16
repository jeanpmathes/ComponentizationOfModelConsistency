package tools.vitruv.compmodelcons.generator;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.xtext.generator.IGenerator;
import org.jspecify.annotations.NonNull;
import tools.vitruv.compmodelcons.generator.backend.GeneratedQueryModelExpressionTypeConfiguration;
import tools.vitruv.compmodelcons.generator.backend.GeneratorBackedSourceTypeRegistryInitialization;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.QueryModelExpressionTypeConfiguration;
import tools.vitruv.neojoin.jvmmodel.SourceTypeRegistryInitialization;

public class NeoJoinVitruvBuildSetup extends NeoJoinStandaloneSetup {
  public NeoJoinVitruvBuildSetup() {
    super(getRegistry());
  }

  private static EPackage.Registry getRegistry() {
    // See Vitruv-DSL :: ReactionsLanguageStandaloneSetup
    EcorePlugin.ExtensionProcessor.process(null);

    return EPackage.Registry.INSTANCE;
  }

  @Override
  protected @NonNull Module createModule() {
    return Modules
        .override(super.createModule())
        .with(new AbstractModule() {
          @Override
          protected void configure() {
            bind(IGenerator.class).to(NeoJoinVitruvGenerator.class);
            bind(QueryModelExpressionTypeConfiguration.class).to(
                GeneratedQueryModelExpressionTypeConfiguration.class);
            bind(SourceTypeRegistryInitialization.class).to(
                GeneratorBackedSourceTypeRegistryInitialization.class);
          }
        });
  }
}
