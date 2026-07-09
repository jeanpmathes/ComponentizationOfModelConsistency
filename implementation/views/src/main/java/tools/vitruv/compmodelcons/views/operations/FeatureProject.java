package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;
import java.util.Optional;

public class FeatureProject implements FeatureOperation {
    private final EStructuralFeature createdFeature;
    private final FeatureOperation origin;

    public FeatureProject(EStructuralFeature createdFeature, FeatureOperation origin) {
        this.createdFeature = createdFeature;
        this.origin = origin;
    }

    public EStructuralFeature getCreatedFeature() {
        return createdFeature;
    }

    @Override
    public FeatureBinding GET(ObjectBinding subject, GetContext context) {
        FeatureBinding originFeature = origin.GET(subject, context);
        subject.viewObject().eSet(createdFeature, originFeature.value());
        return new FeatureProjectBindingImpl(originFeature, subject.viewObject(), originFeature.value());
    }

    @Override
    public FeatureBinding PUT(EChange<EObject> change, FeatureBinding feature, ObjectBinding subject, Optional<Object> value, PutContext context) {
        FeatureProjectBindingImpl binding = (FeatureProjectBindingImpl) feature;

        if (change instanceof ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange) {
            value = Optional.of(replaceSingleValuedFeatureEChange.getNewValue());
        } else if (change instanceof UnsetFeature<EObject, ?>) {
            value = Optional.empty();
        } else {
            throw new IllegalArgumentException("Unsupported change type: " + change.getClass().getSimpleName());
        }

        FeatureBinding originBinding = origin.PUT(change, binding.originBinding(), subject, value, context);
        return new FeatureProjectBindingImpl(originBinding, subject.viewObject(), subject.viewObject().eGet(createdFeature));
    }

    @Override
    public Optional<EChange<EObject>> GET_CHANGE(EChange<EObject> change) {
        return Optional.empty();
    }

    private record FeatureProjectBindingImpl(FeatureBinding originBinding, EObject viewSubjectObject,
                                             Object value) implements FeatureBinding {

        @Override
        public List<EObject> originSubjectObjects() {
            return originBinding.originSubjectObjects();
        }
    }
}
