package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.root.RootEChange;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.*;
import java.util.stream.Stream;

public class Root implements Operation {
    private final EClass rootClass;
    private final boolean isRootImplicit;
    private final Project root;
    private final List<Target> targets;

    private final Map<EClass, Integer> targetIndices = new HashMap<>();
    private final Set<EReference> targetContainmentReferences = new HashSet<>();

    public Root(EClass rootClass, Optional<Project> root, List<Target> targets) {
        this.rootClass = rootClass;
        this.isRootImplicit = root.isEmpty();
        this.root = root.orElse(new Project(rootClass, new Empty(), List.of()));
        this.targets = targets;

        for (int index = 0; index < targets.size(); index++) {
            targetIndices.put(targets.get(index).reference().getEReferenceType(), index);
            targetContainmentReferences.add(targets.get(index).reference());
        }
    }

    @Override
    public List<ObjectBinding> doGet(GetContext context) {
        List<RootObjectBindingImpl> roots = root.beginGetByCreatingViewObjects(context).stream()
                .map(rootBinding -> {
                    context.getViewModel().getContents().add(rootBinding.viewObject());
                    return new RootObjectBindingImpl(rootBinding, targets.stream()
                            .map(entry -> {
                                Map<EObject, ObjectBinding> result = new HashMap<>();
                                for (ObjectBinding targetBinding : entry.operation().beginGetByCreatingViewObjects(context)) {
                                    result.put(targetBinding.viewObject(), targetBinding);
                                    DynamicModels.getList(rootBinding.viewObject(), entry.reference()).add(targetBinding.viewObject());
                                }
                                return result;
                            }).toList());
                }).toList();

        for (RootObjectBindingImpl rootBinding : roots) {
            root.completeGetByCallingGetOnFeatures(rootBinding.rootBinding, context);
            for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
                for (var targetBinding : rootBinding.targetBindings.get(targetIndex).values()) {
                    targets.get(targetIndex).operation().completeGetByCallingGetOnFeatures(targetBinding, context);
                }
            }
        }

        return roots.stream().map(rootBinding -> (ObjectBinding) rootBinding).toList();
    }

    @Override
    public ObjectBinding doPut(EChange<EObject> change, ObjectBinding target, PutContext context) {
        if (change instanceof RootEChange<EObject>) {
            if (isRootImplicit) {
                throw new IllegalArgumentException("Cannot insert or remove the implicit root");
            }
            // The actual insertion / removal is handled by the associated creation / deletion changes.
            return target;
        }

        if (change instanceof InsertEReference<EObject> insertEReference && isContainmentRelevantChange(insertEReference)) {
            return target;
        }
        if (change instanceof RemoveEReference<EObject> removeEReference && isContainmentRelevantChange(removeEReference)) {
            return target;
        }

        EObject affectedViewObject = DynamicModels.getAffectedEObject(change);

        if (affectedViewObject.eClass().equals(rootClass)) {
            if (target.originObjects().isEmpty()) {
                ObjectBinding rootBinding = root.doPut(change, ObjectBinding.ofViewObject(affectedViewObject), context);
                return new RootObjectBindingImpl(
                        rootBinding,
                        Stream.generate(() -> (Map<EObject, ObjectBinding>) new HashMap<EObject, ObjectBinding>()).limit(targets.size()).toList());
            } else {
                RootObjectBindingImpl rootTarget = (RootObjectBindingImpl) target;
                ObjectBinding peeledTarget = rootTarget.rootBinding;
                ObjectBinding rootBinding = root.doPut(change, peeledTarget, context);
                return new RootObjectBindingImpl(rootBinding, rootTarget.targetBindings);
            }
        } else {
            RootObjectBindingImpl rootTarget = (RootObjectBindingImpl) target;
            int classIndex = targetIndices.get(affectedViewObject.eClass());
            ObjectBinding peeledTarget = Optional.ofNullable(rootTarget.targetBindings.get(classIndex).get(affectedViewObject)).orElse(ObjectBinding.ofViewObject(affectedViewObject));
            ObjectBinding targetBinding = targets.get(classIndex).operation().doPut(change, peeledTarget, context);
            if (targetBinding.originObjects().isEmpty()) {
                rootTarget.targetBindings.get(classIndex).remove(affectedViewObject);
            } else {
                rootTarget.targetBindings.get(classIndex).put(affectedViewObject, targetBinding);
            }
            return new RootObjectBindingImpl(rootTarget.rootBinding, rootTarget.targetBindings);
        }
    }

    private boolean isContainmentRelevantChange(FeatureEChange<EObject, EReference> featureEChange) {
        return featureEChange.getAffectedElement().eClass().equals(rootClass) && targetContainmentReferences.contains(featureEChange.getAffectedFeature());
    }

    @Override
    public Optional<EChange<EObject>> doGetChange(EChange<EObject> change) {
        return Optional.empty();
    }

    public record Target(EReference reference, Project operation) {

    }

    private record RootObjectBindingImpl(ObjectBinding rootBinding,
                                         List<Map<EObject, ObjectBinding>> targetBindings) implements ObjectBinding {

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
        public List<ObjectBinding> doGet(GetContext context) {
            return List.of(ObjectBinding.empty());
        }

        @Override
        public ObjectBinding doPut(EChange<EObject> change, ObjectBinding target, PutContext context) {
            throw new UnsupportedOperationException("Modification of the default, uncorresponding root is not supported");
        }

        @Override
        public Optional<EChange<EObject>> doGetChange(EChange<EObject> change) {
            return Optional.empty();
        }
    }
}
