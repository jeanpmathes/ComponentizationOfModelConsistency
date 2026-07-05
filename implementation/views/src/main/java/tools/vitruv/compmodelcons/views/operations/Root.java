package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
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

    public Root(EClass rootClass, Optional<Operation> root, List<Contained> contained) {
        this.rootClass = rootClass;
        this.root = root.orElse(new Project(rootClass, new Empty()));
        this.contained = contained;

        for (int index = 0; index < contained.size(); index++) {
            containmentIndices.put(contained.get(index).reference().getEReferenceType(), index);
            containmentReferences.add(contained.get(index).reference());
        }
    }

    @Override
    public List<ObjectBinding> get(GetContext context) {
        return root.get(context).stream().map(rootBinding -> {
            context.getViewModel().getContents().add(rootBinding.viewObject());

            return (ObjectBinding) new RootObjectBindingImpl(rootBinding, contained.stream()
                    .map(entry -> {
                        Map<EObject, ObjectBinding> result = new HashMap<>();
                        for (ObjectBinding containedBinding : entry.operation().get(context)) {
                            result.put(containedBinding.viewObject(), containedBinding);
                            DynamicModels.getList(rootBinding.viewObject(), entry.reference()).add(containedBinding.viewObject());
                        }
                        return result;
                    }).toList());
        }).toList();
    }

    @Override
    public ObjectBinding put(EChange<EObject> change, ObjectBinding target, PutContext context) {
        if (change instanceof InsertEReference<EObject> insertEReference && isContainmentRelevantChange(insertEReference)) {
            change = new InsertNonRootEObjectImpl<>(insertEReference.getNewValue());
        }
        if (change instanceof RemoveEReference<EObject> removeEReference && isContainmentRelevantChange(removeEReference)) {
            change = new RemoveNonRootEObjectImpl<>(removeEReference.getOldValue());
        }

        EObject affectedViewObject = DynamicModels.getAffectedEObject(change);

        if (affectedViewObject.eClass().equals(rootClass)) {
            if (target.originObjects().isEmpty()) {
                ObjectBinding rootBinding = root.put(change, ObjectBinding.ofViewObject(affectedViewObject), context);
                return new RootObjectBindingImpl(
                        rootBinding,
                        Stream.generate(() -> (Map<EObject, ObjectBinding>) new HashMap<EObject, ObjectBinding>()).limit(contained.size()).toList());
            } else {
                RootObjectBindingImpl rootTarget = (RootObjectBindingImpl) target;
                ObjectBinding peeledTarget = rootTarget.rootBinding;
                ObjectBinding rootBinding = root.put(change, peeledTarget, context);
                return new RootObjectBindingImpl(rootBinding, rootTarget.containedBindings);
            }
        } else {
            RootObjectBindingImpl rootTarget = (RootObjectBindingImpl) target;
            int classIndex = containmentIndices.get(affectedViewObject.eClass());
            ObjectBinding peeledTarget = Optional.ofNullable(rootTarget.containedBindings.get(classIndex).get(affectedViewObject)).orElse(ObjectBinding.ofViewObject(affectedViewObject));
            ObjectBinding containedBinding = contained.get(classIndex).operation().put(change, peeledTarget, context);
            if (containedBinding.originObjects().isEmpty()) {
                rootTarget.containedBindings.get(classIndex).remove(affectedViewObject);
            } else {
                rootTarget.containedBindings.get(classIndex).put(affectedViewObject, containedBinding);
            }
            return new RootObjectBindingImpl(rootTarget.rootBinding, rootTarget.containedBindings);
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

    private static class Empty implements Operation {
        @Override
        public List<ObjectBinding> get(GetContext context) {
            return List.of(ObjectBinding.empty());
        }

        @Override
        public ObjectBinding put(EChange<EObject> change, ObjectBinding target, PutContext context) {
            throw new UnsupportedOperationException("Modification of the default, uncorresponding root is not supported");
        }

        @Override
        public Optional<EChange<EObject>> getChange(EChange<EObject> change) {
            return Optional.empty();
        }
    }
}
