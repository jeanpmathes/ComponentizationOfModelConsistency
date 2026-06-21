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
    public static Optional<AQR> getAQR(String file, EPackage.Registry registry) throws IOException {
        // todo: the created view should maybe also have a package, but NeoJoin already has code to create ecore from NeoJoin

        NeoJoinStandaloneSetup setup = new NeoJoinStandaloneSetup(registry);
        Parser.Result result = setup.getParser().parse(URI.createFileURI(file));

        if (result instanceof Parser.Result.Success success) {
            return Optional.of(success.aqr());
        }

        return Optional.empty();
    }
}
