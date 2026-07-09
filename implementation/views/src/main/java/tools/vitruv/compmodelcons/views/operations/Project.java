package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.FeatureEChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Project implements Operation {
    private final EClass createdClass;
    private final Operation origin;
    private final List<FeatureOperation> features;

    private final Map<EStructuralFeature, Integer> featureIndices = new java.util.HashMap<>();

    public Project(EClass createdClass, Operation origin, List<FeatureProject> features) {
        this.createdClass = createdClass;
        this.origin = origin;
        this.features = List.copyOf(features);

        for (int index = 0; index < features.size(); index++) {
            featureIndices.put(features.get(index).getCreatedFeature(), index);
        }
    }

    @Override
    public List<ObjectBinding> GET(GetContext context) {
        return origin.GET(context).stream().map(originBinding -> {
            EObject result = createdClass.getEPackage().getEFactoryInstance().create(createdClass);

            context.getCorrespondences().addCorrespondence(originBinding.originObjects(), result);

            ProjectObjectBindingImpl projected = new ProjectObjectBindingImpl(originBinding, result, createUninitializedFeatureBindings());

            initializeFeatureBindings(projected.featureBindings, projected, context);

            return (ObjectBinding) projected;
        }).toList();
    }

    public ObjectBinding PUT(EChange<EObject> change, ObjectBinding target, PutContext context) {
        if (!target.viewObject().eClass().equals(createdClass)) {
            throw new IllegalArgumentException("Cannot put a change on an object that is not of the created class");
        }

        EObject viewObject = target.viewObject();
        ObjectBinding peeledTarget = target;
        List<FeatureBinding> featureBindings;

        if (!target.originObjects().isEmpty()) {
            ProjectObjectBindingImpl binding = (ProjectObjectBindingImpl) target;
            peeledTarget = binding.originBinding;
            featureBindings = new ArrayList<>(binding.featureBindings);
        } else {
            featureBindings = createUninitializedFeatureBindings();
        }

        if (change instanceof FeatureEChange<EObject, ?> featureEChange) {
            int featureIndex = featureIndices.get(featureEChange.getAffectedFeature());
            //noinspection OptionalAssignedToNull
            featureBindings.set(featureIndex, features.get(featureIndex).PUT(change, featureBindings.get(featureIndex), target, null, context));
            return new ProjectObjectBindingImpl(peeledTarget, viewObject, featureBindings);
        } else {
            ObjectBinding originBinding = origin.PUT(change, peeledTarget, context);

            ProjectObjectBindingImpl projected = new ProjectObjectBindingImpl(originBinding, viewObject, featureBindings);

            if (target.originObjects().isEmpty()) {
                initializeFeatureBindings(projected.featureBindings, projected, context);
            }

            return projected;
        }
    }


    public Optional<EChange<EObject>> GET_CHANGE(EChange<EObject> change) {
        return Optional.empty();
    }

    private List<FeatureBinding> createUninitializedFeatureBindings() {
        List<FeatureBinding> result = new ArrayList<>(features.size());

        for (int index = 0; index < features.size(); index++) {
            result.add(null);
        }

        return result;
    }

    private void initializeFeatureBindings(List<FeatureBinding> featureBindings, ObjectBinding subject, GetContext context) {
        for (int index = 0; index < featureBindings.size(); index++) {
            featureBindings.set(index, features.get(index).GET(subject, context));
        }
    }

    private record ProjectObjectBindingImpl(ObjectBinding originBinding,
                                            EObject viewObject,
                                            List<FeatureBinding> featureBindings) implements ObjectBinding {

        @Override
        public List<EObject> originObjects() {
            return originBinding.originObjects();
        }
    }
}
