package tools.vitruv.compmodelcons.change;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.command.internal.ApplyEChangeSwitch;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.resolve.AtomicEChangeResolverHelper;
import tools.vitruv.change.propagation.ModelSnapshot;
import tools.vitruv.compmodelcons.views.DynamicModels;

public class SnapshotChangeApplier {
    private final ModelSnapshot snapshot;

    public SnapshotChangeApplier(ModelSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public EChange<EObject> apply(EChange<EObject> repositoryChange) {
        if (repositoryChange instanceof CreateEObject<EObject> createEObject) {
            EObject repositoryObject = createEObject.getAffectedElement();
            if (snapshot.getSnapshotEObject(repositoryObject).isEmpty()) {
                EObject snapshotObject = DynamicModels.createEObject(repositoryObject.eClass());
                snapshot.registerEObjectMapping(repositoryObject, snapshotObject);
            }
        }

        EChange<EObject> snapshotChange = AtomicEChangeResolverHelper.resolveChange(
                repositoryChange,
                repositoryObject -> snapshot.getSnapshotEObject(repositoryObject).orElseThrow(),
                resource -> snapshot.getModelResource(resource.getURI())
        );

        ApplyEChangeSwitch.applyEChange(snapshotChange, true);

        return snapshotChange;
    }
}
