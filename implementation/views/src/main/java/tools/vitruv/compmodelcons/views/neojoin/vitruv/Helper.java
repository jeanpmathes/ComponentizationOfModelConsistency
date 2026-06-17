package tools.vitruv.compmodelcons.views.neojoin.vitruv;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.aqr.AQR;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Helper { // todo: this class should be removed at some point
    public static Optional<AQR> getAQR(String file) throws IOException {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("ecore")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                    "ecore", new EcoreResourceFactoryImpl());
        }

        var registry = new EPackageRegistryImpl();
        var packageResourceSet = new ResourceSetImpl();

        // todo: for registry creation, Vitruv probably already loads them so reusing that registry would be nice, see what reactions do
        // todo: the created view should maybe also have a package, but NeoJoin already has code to create ecore from NeoJoin

        for (var path : List.of("/imdb.ecore", "/library.ecore")) {
            var resource = packageResourceSet.createResource(URI.createURI(path));
            var input = Objects.requireNonNull(Helper.class.getResourceAsStream(path));
            resource.load(input, null);
            var loadedPackage = (EPackage) resource.getContents().get(0);
            registry.put(loadedPackage.getNsURI(), loadedPackage);
        }

        NeoJoinStandaloneSetup setup = new NeoJoinStandaloneSetup(registry);
        Parser.Result result = setup.getParser().parse(URI.createFileURI(file));

        if (result instanceof Parser.Result.Success success) {
            return Optional.of(success.aqr());
        }

        return Optional.empty();
    }
}
