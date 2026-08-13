package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.List;

public interface ChangePropagationView extends AutoCloseable {
    ResourceAccess getViewResourceAccess();

    List<EChange<EObject>> fitAndDetermineChanges(ResourceAccess changedOrigin, CorrespondenceModelAccess changedCorrespondenceModel, List<EChange<EObject>> originChanges, ChangeDeterminationMode changeDeterminationMode);

    CorrespondenceResolver getCorrespondenceResolver();

    void commit();
}
