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
    private final boolean isRoot;
    private final Operation inner;

    public Project(EClass createdClass, boolean isRoot, Operation inner) {
        this.createdClass = createdClass;
        this.isRoot = isRoot;
        this.inner = inner;
    }

    @Override
    public List<ObjectBinding> get(Context context) {
        return inner.get(context).stream().map(origin -> {
            EObject result = createdClass.getEPackage().getEFactoryInstance().create(createdClass);
            context.getCorrespondences().addCorrespondence(origin.originObjects(), result);

            if (isRoot) {
                context.getViewModel().getContents().add(result);
            }
            // todo: add mechanism to add this created EObject to the root, could be an operation-like that wraps the entire query
            // todo: or see how NeoJoin intends to do this, might be a subquery thing

            return (ObjectBinding) new ProjectObjectBindingImpl(origin, result);
        }).toList();
    }

    public Optional<ObjectBinding> put(EChange<EObject> eChange, ObjectBinding target, Context context) {
        if (!target.viewObject().eClass().equals(createdClass)) {
            return Optional.empty();
        }

        boolean isResponsibleForHandlingChange;

        if (eChange instanceof EObjectExistenceEChange<EObject> eObjectEObjectExistenceEChange) {
            isResponsibleForHandlingChange = eObjectEObjectExistenceEChange.getAffectedElement().eClass().equals(createdClass);
        } else if (eChange instanceof EObjectAddedEChange<EObject> eObjectEObjectAddedEChange) {
            isResponsibleForHandlingChange = eObjectEObjectAddedEChange.getNewValue().eClass().equals(createdClass);
        } else if (eChange instanceof EObjectSubtractedEChange<EObject> eObjectEObjectSubtractedEChange) {
            isResponsibleForHandlingChange = eObjectEObjectSubtractedEChange.getOldValue().eClass().equals(createdClass);
        } else if (eChange instanceof FeatureEChange<EObject, ?> featureEChange) {
            isResponsibleForHandlingChange = featureEChange.getAffectedElement().eClass().equals(createdClass);
        } else {
            throw new IllegalStateException("Unknown change type: " + eChange.getClass().getSimpleName());
        }

        if (!isResponsibleForHandlingChange) {
            return Optional.empty();
        }

        EObject viewObject = target.viewObject();
        ObjectBinding peeledTarget = target;

        if (!target.originObjects().isEmpty()) {
            ProjectObjectBindingImpl binding = (ProjectObjectBindingImpl) target;
            peeledTarget = binding.origin;
        }

        return inner
                .put(eChange, peeledTarget, context)
                .map(origin -> new ProjectObjectBindingImpl(origin, viewObject));
    }


    public Optional<EChange<EObject>> getChange(EChange<EObject> change) {
        return Optional.empty();
    }

    private record ProjectObjectBindingImpl(ObjectBinding origin, EObject viewObject) implements ObjectBinding {

        @Override
            public List<EObject> originObjects() {
                return origin.originObjects();
            }
        }
}
