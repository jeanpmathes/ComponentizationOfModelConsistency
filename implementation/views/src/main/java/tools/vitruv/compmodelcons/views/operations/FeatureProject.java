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
    private final FeatureOperation origin;

    public FeatureProject(Optional<EStructuralFeature> sourceFeature, EStructuralFeature createdFeature, FeatureOperation origin) {
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
        if (originFeature.value() instanceof ValueBinding.Single single) {
            Object translated = translateOriginToView(single.value(), context);
            subject.viewObject().eSet(createdFeature, translated);
            return new FeatureProjectBindingImpl(originFeature, subject.viewObject(), new ValueBinding.Single(translated));
        }
        if (originFeature.value() instanceof ValueBinding.Many many) {
            List<?> translated = many.values().stream().map(value -> translateOriginToView(value, context)).toList();
            subject.viewObject().eSet(createdFeature, translated);
            return new FeatureProjectBindingImpl(originFeature, subject.viewObject(), new ValueBinding.Many(translated));
        }

        throw new UnsupportedOperationException();
    }

    public FeatureBinding doPut(EChange<EObject> change, FeatureBinding feature, ObjectBinding subject, PutContext context) {
        if (!hasKnownSource) {
            throw new IllegalArgumentException("Cannot put changes on a feature that has no known source");
        }

        FeatureProjectBindingImpl binding = (FeatureProjectBindingImpl) feature;

        ValueUpdateBinding value;

        if (change instanceof ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange) {
            value = new ValueUpdateBinding.Replace(translateViewToOrigin(replaceSingleValuedFeatureEChange.getNewValue(), context));
        } else if (change instanceof InsertInListEChange<EObject, ?, ?> insertInListEChange) {
            value = new ValueUpdateBinding.Insert(translateViewToOrigin(insertInListEChange.getNewValue(), context), insertInListEChange.getIndex());
        } else if (change instanceof RemoveFromListEChange<EObject, ?, ?> removeFromListEChange) {
            value = new ValueUpdateBinding.Remove(translateViewToOrigin(removeFromListEChange.getOldValue(), context), removeFromListEChange.getIndex());
        } else if (change instanceof UnsetFeature<EObject, ?>) {
            value = new ValueUpdateBinding.Unset();
        } else {
            throw new IllegalArgumentException("Unsupported change type: " + change.getClass().getSimpleName());
        }

        FeatureBinding originBinding = origin.doPut(change, binding.originBinding(), subject, value, context);

        return new FeatureProjectBindingImpl(originBinding, subject.viewObject(), ValueBinding.ofFeature(subject.viewObject(), createdFeature));
    }

    private Object translateOriginToView(Object originValue, GetContext context) {
        if (originValue instanceof EObject eObject) {
            return context.getCorrespondences().getCorrespondingViewObjectForPartialOriginObjects(eObject, (EClass) createdFeature.getEType());
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

    public Optional<EChange<EObject>> doGetChange(EChange<EObject> change) {
        return Optional.empty();
    }

    private record FeatureProjectBindingImpl(FeatureBinding originBinding, EObject viewSubjectObject,
                                             ValueBinding value) implements FeatureBinding {

        @Override
        public List<EObject> originSubjectObjects() {
            return originBinding.originSubjectObjects();
        }
    }
}
