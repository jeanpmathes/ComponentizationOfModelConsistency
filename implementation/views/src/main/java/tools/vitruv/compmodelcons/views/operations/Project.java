package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.EObjectAddedEChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.atomic.eobject.EObjectSubtractedEChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.compmodelcons.views.Context;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;
import java.util.Optional;

public class Project implements Operation {
    private final EClass createdClass;
    private final Operation origin;

    public Project(EClass createdClass, Operation origin) {
        this.createdClass = createdClass;
        this.origin = origin;
    }

    @Override
    public List<ObjectBinding> get(Context context) {
        return origin.get(context).stream().map(originBinding -> {
            EObject result = createdClass.getEPackage().getEFactoryInstance().create(createdClass);

            context.getCorrespondences().addCorrespondence(originBinding.originObjects(), result);

            return (ObjectBinding) new ProjectObjectBindingImpl(originBinding, result);
        }).toList();
    }

    public Optional<ObjectBinding> put(EChange<EObject> eChange, ObjectBinding target, Context context) {
        if (!target.viewObject().eClass().equals(createdClass)) {
            return Optional.empty();
        }

        boolean isResponsibleForHandlingChange;

        if (eChange instanceof EObjectExistenceEChange<EObject> eObjectEObjectExistenceEChange) {
            isResponsibleForHandlingChange = eObjectEObjectExistenceEChange.getAffectedElement().eClass().equals(createdClass);
        } else if (eChange instanceof FeatureEChange<EObject, ?> featureEChange) {
            isResponsibleForHandlingChange = featureEChange.getAffectedElement().eClass().equals(createdClass);
        } else if (eChange instanceof EObjectAddedEChange<EObject> eObjectEObjectAddedEChange) {
            isResponsibleForHandlingChange = eObjectEObjectAddedEChange.getNewValue().eClass().equals(createdClass);
        } else if (eChange instanceof EObjectSubtractedEChange<EObject> eObjectEObjectSubtractedEChange) {
            isResponsibleForHandlingChange = eObjectEObjectSubtractedEChange.getOldValue().eClass().equals(createdClass);
        } else {
            throw new IllegalArgumentException("Unknown change type: " + eChange.getClass().getSimpleName());
        }

        if (!isResponsibleForHandlingChange) {
            return Optional.empty();
        }

        EObject viewObject = target.viewObject();
        ObjectBinding peeledTarget = target;

        if (!target.originObjects().isEmpty()) {
            ProjectObjectBindingImpl binding = (ProjectObjectBindingImpl) target;
            peeledTarget = binding.originBinding;
        }

        return origin
                .put(eChange, peeledTarget, context)
                .map(originBinding -> new ProjectObjectBindingImpl(originBinding, viewObject));
    }


    public Optional<EChange<EObject>> getChange(EChange<EObject> change) {
        return Optional.empty();
    }

    private record ProjectObjectBindingImpl(ObjectBinding originBinding, EObject viewObject) implements ObjectBinding {

        @Override
            public List<EObject> originObjects() {
            return originBinding.originObjects();
            }
        }
}
