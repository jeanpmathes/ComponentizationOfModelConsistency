package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.utils.ResourceAccess;

import java.util.List;

public interface ChangePropagationView extends AutoCloseable {
    void update();

    ResourceAccess getViewResourceAccess();

    List<EChange<EObject>> doGetChange(EChange<EObject> originChange);

    default List<EChange<EObject>> doGetChange(List<EChange<EObject>> originChanges) {
        return originChanges.stream().flatMap(originChange -> doGetChange(originChange).stream()).toList();
    }

    void commit();
}
