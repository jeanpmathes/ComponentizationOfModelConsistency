package tools.vitruv.compmodelcons.views;

import java.util.List;
import java.util.function.Function;
import org.eclipse.emf.ecore.EObject;

public interface EditableViewCorrespondences extends ViewCorrespondences {
  void addCorrespondence(List<EObject> originObjects, EObject viewObject);

  void joinCorrespondence(List<EObject> currentOriginObjects, List<EObject> addedOriginObjects,
                          EObject viewObject);

  void removeCorrespondence(List<EObject> originObjects, EObject viewObject);

  void unjoinCorrespondence(List<EObject> currentOriginObjects, List<EObject> removedOriginObjects,
                            EObject viewObject);

  void update(
      ViewCorrespondences newCorrespondences,
      Function<EObject, EObject> originObjectMapper,
      Function<EObject, EObject> viewObjectMapper);
}
