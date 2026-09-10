package tools.vitruv.compmodelcons.change.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.IdentifierEObjectMatcher;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.changederivation.DefaultStateBasedChangeResolutionStrategy;

public class ViewAdaptedStateBasedChangeResolutionStrategy
    extends DefaultStateBasedChangeResolutionStrategy {
  private final Function<EObject, List<EObject>> viewToOriginObjects;

  public ViewAdaptedStateBasedChangeResolutionStrategy(
      Function<EObject, List<EObject>> viewToOriginObjects) {
    this.viewToOriginObjects = viewToOriginObjects;
  }

  @Override
  protected Collection<IMatchEngine.Factory> getMatchEngineFactories() {
    return List.of(new ViewAdaptedMatchEngineFactory(useIdentifiers, viewToOriginObjects));
  }

  private static final class ViewAdaptedMatchEngineFactory extends MatchEngineFactoryImpl {
    private final UseIdentifiers useIdentifiers;
    private final Function<EObject, List<EObject>> viewToOriginObjects;

    public ViewAdaptedMatchEngineFactory(
        UseIdentifiers useIdentifiers,
        Function<EObject, List<EObject>> viewToOriginObjects) {
      super(useIdentifiers);
      this.useIdentifiers = useIdentifiers;
      this.viewToOriginObjects = viewToOriginObjects;
    }

    @Override
    public IMatchEngine getMatchEngine() {
      if (matchEngine == null) {
        IEObjectMatcher defaultMatcher =
            DefaultMatchEngine.createDefaultEObjectMatcher(useIdentifiers);

        IEObjectMatcher viewCorrespondenceBasedMatcher =
            new IdentifierEObjectMatcher(defaultMatcher, new IdentifierProvider()::getIdentifier);

        IEObjectMatcher rootPreservingMatcher =
            new IdentifierEObjectMatcher(viewCorrespondenceBasedMatcher,
                                         this::getIdentifierForRootEObject);

        matchEngine = new DefaultMatchEngine(rootPreservingMatcher,
                                             new DefaultComparisonFactory(
                                                 new DefaultEqualityHelperFactory()));
      }
      return matchEngine;
    }

    private String getIdentifierForRootEObject(EObject eObject) {
      if (eObject == null || eObject.eContainer() != null) {
        return null;
      }

      return eObject.eClass().getEPackage().getNsURI() + "#" + eObject.eClass().getName();
    }

    private class IdentifierProvider {
      private final Map<Identifier, String> identifiers = new HashMap<>();
      private long id = 0;

      public String getIdentifier(EObject viewObject) {
        List<EObject> originObjects = viewToOriginObjects.apply(viewObject);
        if (originObjects == null || originObjects.isEmpty()) {
          return null;
        }

        return identifiers.computeIfAbsent(
            new Identifier(viewObject.eClass(), List.copyOf(originObjects)),
            ignored -> "view2orign-" + id++);
      }

      private record Identifier(EClass viewClass, List<EObject> originObjects) {

      }
    }
  }
}
