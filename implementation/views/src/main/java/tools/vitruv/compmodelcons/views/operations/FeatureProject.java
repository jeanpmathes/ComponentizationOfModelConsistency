package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.list.InsertInListEChange;
import tools.vitruv.change.atomic.feature.list.RemoveFromListEChange;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

import java.util.List;
import java.util.Optional;

public class FeatureProject {
    private final boolean hasKnownSource;
    private final EClass sourceObjectClass;
    private final EStructuralFeature createdFeature;
    private final boolean isReference;
    private final FeatureOriginOperation origin;

    public FeatureProject(Optional<EStructuralFeature> sourceFeature, EStructuralFeature createdFeature, FeatureOriginOperation origin) {
        this.hasKnownSource = sourceFeature.isPresent();
        if (sourceFeature.isPresent() && sourceFeature.get().getEType() instanceof EClass eClass) {
            this.sourceObjectClass = eClass;
        } else {
            this.sourceObjectClass = null;
        }
        this.createdFeature = createdFeature;
        this.isReference = createdFeature instanceof EReference;
        this.origin = origin;
    }

    public EStructuralFeature getCreatedFeature() {
        return createdFeature;
    }

    public FeatureBinding doGet(ObjectBinding subject, GetContext context) {
        FeatureBinding originFeature = origin.doGet(subject, context);

        if (originFeature.value() instanceof ValueBinding.Unset) {
            subject.viewObject().eUnset(createdFeature);
            return new FeatureProjectBindingImpl(originFeature, subject.viewObject(), originFeature.value());
        }
        if (originFeature.value() instanceof ValueBinding.Single(Object value)) {
            Object translated = translateOriginToView(value, context);
            subject.viewObject().eSet(createdFeature, translated);
            return new FeatureProjectBindingImpl(originFeature, subject.viewObject(), new ValueBinding.Single(translated));
        }
        if (originFeature.value() instanceof ValueBinding.Many(List<?> values)) {
            List<?> translated = values.stream().map(value -> translateOriginToView(value, context)).toList();
            subject.viewObject().eSet(createdFeature, translated);
            return new FeatureProjectBindingImpl(originFeature, subject.viewObject(), new ValueBinding.Many(translated));
        }

        throw new UnsupportedOperationException();
    }

    public FeatureBinding doPut(EChange<EObject> change, FeatureBinding feature, ObjectBinding subject, PutContext context) {
        if (!hasKnownSource) {
            throw new UnsupportedOperationException("Cannot put changes on a feature that has no known source");
        }

        FeatureProjectBindingImpl binding = (FeatureProjectBindingImpl) feature;

        ValueUpdateBinding value = switch (change) {
            case ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange ->
                    new ValueUpdateBinding.Replace(translateViewToOrigin(replaceSingleValuedFeatureEChange.getNewValue(), context));
            case InsertInListEChange<EObject, ?, ?> insertInListEChange ->
                    new ValueUpdateBinding.Insert(translateViewToOrigin(insertInListEChange.getNewValue(), context), insertInListEChange.getIndex());
            case RemoveFromListEChange<EObject, ?, ?> removeFromListEChange ->
                    new ValueUpdateBinding.Remove(translateViewToOrigin(removeFromListEChange.getOldValue(), context), removeFromListEChange.getIndex());
            case UnsetFeature<EObject, ?> ignored -> new ValueUpdateBinding.Unset();
            default ->
                    throw new IllegalArgumentException("Unsupported change type: " + change.getClass().getSimpleName());
        };

        FeatureBinding originBinding = origin.doPut(change, binding.originBinding(), subject, value, context);

        return new FeatureProjectBindingImpl(originBinding, subject.viewObject(), ValueBinding.ofFeature(subject.viewObject(), createdFeature));
    }

    private Object translateOriginToView(Object originValue, GetContext context) {
        if (originValue instanceof EObject eObject) {
            var candidates = context.getCorrespondences().getCorrespondingViewObjectForPartialOriginObjects(eObject, (EClass) createdFeature.getEType());
            if (candidates.isEmpty()) {
                throw new IllegalStateException("Could not find view object for origin object " + eObject);
            }
            if (candidates.size() > 1) {
                throw new IllegalStateException("Found multiple view objects for origin object " + eObject);
            }
            return candidates.stream().findAny().orElseThrow();

        }
        return originValue;
    }

    private Object translateViewToOrigin(Object viewValue, PutContext context) {
        if (isReference && viewValue instanceof EObject eObject) {
            if (sourceObjectClass == null) {
                throw new UnsupportedOperationException();
            }

            return context.getCorrespondences().getCorrespondingOriginObjectsForViewObject(eObject).stream()
                    .filter(originObject -> originObject.eClass().equals(sourceObjectClass))
                    .findFirst().orElseThrow();
        }

        return viewValue;
    }

    public FeatureBinding doGet(FeatureBinding previous, EChange<EObject> originChange, GetContext context) {
        return null;
    }

    private record FeatureProjectBindingImpl(FeatureBinding originBinding, EObject viewSubjectObject,
                                             ValueBinding value) implements FeatureBinding {

        @Override
        public List<EObject> originSubjectObjects() {
            return originBinding.originSubjectObjects();
        }
    }
}
