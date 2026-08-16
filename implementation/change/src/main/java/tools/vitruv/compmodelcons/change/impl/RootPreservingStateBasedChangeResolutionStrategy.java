package tools.vitruv.compmodelcons.change.impl;

import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.IdentifierEObjectMatcher;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.changederivation.DefaultStateBasedChangeResolutionStrategy;

import java.util.Collection;
import java.util.List;

public class RootPreservingStateBasedChangeResolutionStrategy extends DefaultStateBasedChangeResolutionStrategy {
    private static String getIdentifierForRootEObject(EObject eObject) {
        if (eObject == null || eObject.eContainer() != null) {
            return null;
        }

        return eObject.eClass().getEPackage().getNsURI() + "#" + eObject.eClass().getName();
    }

    @Override
    protected Collection<IMatchEngine.Factory> getMatchEngineFactories() {
        return List.of(new RootPreservingMatchEngineFactory(useIdentifiers));
    }

    private static final class RootPreservingMatchEngineFactory extends MatchEngineFactoryImpl {
        private final UseIdentifiers useIdentifiers;

        public RootPreservingMatchEngineFactory(UseIdentifiers useIdentifiers) {
            super(useIdentifiers);
            this.useIdentifiers = useIdentifiers;
        }

        @Override
        public IMatchEngine getMatchEngine() {
            if (matchEngine == null) {
                IEObjectMatcher defaultMatcher = DefaultMatchEngine.createDefaultEObjectMatcher(useIdentifiers);
                IEObjectMatcher rootPreservingMatcher = new IdentifierEObjectMatcher(defaultMatcher, RootPreservingStateBasedChangeResolutionStrategy::getIdentifierForRootEObject);

                matchEngine = new DefaultMatchEngine(rootPreservingMatcher, new DefaultComparisonFactory(new DefaultEqualityHelperFactory()));
            }
            return matchEngine;
        }
    }
}
