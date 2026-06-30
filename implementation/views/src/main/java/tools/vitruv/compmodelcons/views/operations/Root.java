package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.compmodelcons.views.Context;
import tools.vitruv.compmodelcons.views.Utilities;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.impl.InsertNonRootEObjectImpl;
import tools.vitruv.compmodelcons.views.impl.RemoveNonRootEObjectImpl;

import java.util.*;
import java.util.stream.Stream;

public class Root implements Operation {
    private final EClass rootClass;
    private final Operation root;
    private final List<Contained> contained;

    private final Map<EClass, Integer> containmentIndices = new HashMap<>();
    private final Set<EReference> containmentReferences = new HashSet<>();

    public Root(EClass rootClass, Operation root, List<Contained> contained) {
        this.rootClass = rootClass;
        this.root = root;
        this.contained = contained;

        for (int index = 0; index < contained.size(); index++) {
            containmentIndices.put(contained.get(index).reference().getEReferenceType(), index);
            containmentReferences.add(contained.get(index).reference());
        }
    }

    @Override
    public List<ObjectBinding> get(Context context) {
        return root.get(context).stream().map(rootBinding -> {
            context.getViewModel().getContents().add(rootBinding.viewObject());

            return (ObjectBinding) new RootObjectBindingImpl(rootBinding, contained.stream()
                    .map(entry -> {
                        Map<EObject, ObjectBinding> result = new HashMap<>();
                        for (ObjectBinding containedBinding : entry.operation().get(context)) {
                            result.put(containedBinding.viewObject(), containedBinding);

                            Utilities.getList(rootBinding.viewObject(), entry.reference()).add(containedBinding.viewObject());
                        }
                        return result;
                    }).toList());
        }).toList();
    }

    @Override
    public Optional<ObjectBinding> put(EChange<EObject> eChange, ObjectBinding target, Context context) {
        if (eChange instanceof InsertEReference<EObject> insertEReference && isContainmentRelevantChange(insertEReference)) {
            eChange = new InsertNonRootEObjectImpl<>(insertEReference.getNewValue());
        }
        if (eChange instanceof RemoveEReference<EObject> removeEReference && isContainmentRelevantChange(removeEReference)) {
            eChange = new RemoveNonRootEObjectImpl<>(removeEReference.getOldValue());
        }

        EObject affectedViewObject = Utilities.getAffectedEObject(eChange);

        if (affectedViewObject.eClass().equals(rootClass)) {
            if (target.originObjects().isEmpty()) {
                return root
                        .put(eChange, ObjectBinding.ofViewObject(affectedViewObject), context)
                        .map(rootBinding -> new RootObjectBindingImpl(rootBinding, Stream.generate(() -> (Map<EObject, ObjectBinding>) new HashMap<EObject, ObjectBinding>()).limit(contained.size()).toList()));
            } else {
                RootObjectBindingImpl rootTarget = (RootObjectBindingImpl) target;
                ObjectBinding peeledTarget = rootTarget.rootBinding;
                return root
                        .put(eChange, peeledTarget, context)
                        .map(rootBinding -> new RootObjectBindingImpl(rootBinding, rootTarget.containedBindings));
            }
        } else {
            if (target.originObjects().isEmpty()) {
                throw new IllegalArgumentException("Cannot put a change on a non-root object if there is no root target");
            }
            RootObjectBindingImpl rootTarget = (RootObjectBindingImpl) target;
            int classIndex = containmentIndices.get(affectedViewObject.eClass());
            ObjectBinding peeledTarget = Optional.ofNullable(rootTarget.containedBindings.get(classIndex).get(affectedViewObject)).orElse(ObjectBinding.ofViewObject(affectedViewObject));

            return contained.get(classIndex).operation()
                    .put(eChange, peeledTarget, context)
                    .map(containedBinding -> {
                        rootTarget.containedBindings.get(classIndex).put(affectedViewObject, containedBinding);
                        return new RootObjectBindingImpl(rootTarget.rootBinding, rootTarget.containedBindings);
                    });
        }
    }

    private boolean isContainmentRelevantChange(FeatureEChange<EObject, EReference> featureEChange) {
        return featureEChange.getAffectedElement().eClass().equals(rootClass) && containmentReferences.contains(featureEChange.getAffectedFeature());
    }

    @Override
    public Optional<EChange<EObject>> getChange(EChange<EObject> change) {
        return Optional.empty();
    }

    public record Contained(EReference reference, Operation operation) {

    }

    private record RootObjectBindingImpl(ObjectBinding rootBinding,
                                         List<Map<EObject, ObjectBinding>> containedBindings) implements ObjectBinding {

        @Override
        public List<EObject> originObjects() {
            return rootBinding.originObjects();
        }

        @Override
        public EObject viewObject() {
            return rootBinding.viewObject();
        }
    }
}
