package tools.vitruv.compmodelcons.generator;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.xtext.generator.IGenerator;
import org.jspecify.annotations.NonNull;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;

public class NeoJoinVitruvSetup extends NeoJoinStandaloneSetup {
    public NeoJoinVitruvSetup() {
        super(getRegistry());
    }

    private static EPackage.Registry getRegistry() {
        // See Vitruv-DSL :: ReactionsLanguageStandaloneSetup
        EcorePlugin.ExtensionProcessor.process(null);

        return EPackage.Registry.INSTANCE;
    }

    @Override
    protected @NonNull Module createModule() {
        return Modules.override(super.createModule()).with(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IGenerator.class).to(NeoJoinVitruvGenerator.class);
            }
        });
    }
}
