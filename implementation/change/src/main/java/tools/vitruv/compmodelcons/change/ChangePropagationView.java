package tools.vitruv.compmodelcons.change;

import java.util.List;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.utils.ResourceAccess;

public interface ChangePropagationView extends AutoCloseable {
  ResourceAccess getViewResourceAccess();

  List<EChange<EObject>> fitAndDetermineChanges(
      ResourceAccess changedOrigin,
      CorrespondenceModelAccess changedCorrespondenceModel,
      List<EChange<EObject>> originChanges);

  CorrespondenceResolver getCorrespondenceResolver();

  void commit();
}
