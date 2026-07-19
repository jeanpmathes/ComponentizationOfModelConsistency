package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.List;

public interface ChangePropagationView extends AutoCloseable {
    ResourceAccess getViewResourceAccess();

    CorrespondenceResolver getCorrespondenceResolver();

    void commit();

    List<EChange<EObject>> fitAndDetermineChanges(ResourceAccess changedOrigin, List<EChange<EObject>> originChanges, ChangeDeterminationMode changeDeterminationMode);
}
