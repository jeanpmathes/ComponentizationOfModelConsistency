package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;
import java.util.Optional;

public class Project implements Operation {
    private static final String wrongClassForPutMessage = "Cannot put a change on an object that is not of the created class";

    private final EClass createdClass;
    private final Operation origin;

    public Project(EClass createdClass, Operation origin) {
        this.createdClass = createdClass;
        this.origin = origin;
    }

    @Override
    public List<ObjectBinding> get(GetContext context) {
        return origin.get(context).stream().map(originBinding -> {
            EObject result = createdClass.getEPackage().getEFactoryInstance().create(createdClass);

            context.getCorrespondences().addCorrespondence(originBinding.originObjects(), result);

            return (ObjectBinding) new ProjectObjectBindingImpl(originBinding, result);
        }).toList();
    }

    public ObjectBinding put(EChange<EObject> change, ObjectBinding target, PutContext context) {
        if (!target.viewObject().eClass().equals(createdClass)) {
            throw new IllegalArgumentException(wrongClassForPutMessage);
        }

        EObject viewObject = target.viewObject();
        ObjectBinding peeledTarget = target;

        if (!target.originObjects().isEmpty()) {
            ProjectObjectBindingImpl binding = (ProjectObjectBindingImpl) target;
            peeledTarget = binding.originBinding;
        }

        ObjectBinding originBinding = origin.put(change, peeledTarget, context);
        return new ProjectObjectBindingImpl(originBinding, viewObject);
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
