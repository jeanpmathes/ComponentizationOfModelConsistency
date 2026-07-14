package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueBinding;
import tools.vitruv.compmodelcons.views.bindings.ValueUpdateBinding;

import java.util.List;
import java.util.Optional;

public class FeatureSource implements FeatureOperation {
    private final EClass sourceClass;
    private final EStructuralFeature sourceFeature;
    private final boolean isSourceFeatureAContainmentFeature;

    public FeatureSource(EStructuralFeature sourceFeature) {
        this.sourceClass = sourceFeature.getEContainingClass();
        this.sourceFeature = sourceFeature;

        if (this.sourceFeature instanceof EReference eReference) {
            this.isSourceFeatureAContainmentFeature = eReference.isContainment();
        } else {
            this.isSourceFeatureAContainmentFeature = false;
        }
    }

    private Optional<EObject> getSource(ObjectBinding subject) {
        return subject.originObjects().stream().filter(eObject -> sourceClass.isSuperTypeOf(eObject.eClass())).findFirst();
    }

    @Override
    public FeatureBinding doGet(ObjectBinding subjectBinding, GetContext context) {
        return getSource(subjectBinding).map(subject -> FeatureBinding.ofOriginObject(subject, ValueBinding.ofFeature(subject, sourceFeature))).orElseThrow();
    }

    public FeatureBinding doPut(EChange<EObject> change, FeatureBinding feature, ObjectBinding subjectBinding, ValueUpdateBinding value, PutContext context) {
        return getSource(subjectBinding).map(subject -> {
            Object object = null;

            switch (value) {
                case ValueUpdateBinding.Unset ignored -> subject.eUnset(sourceFeature);
                case ValueUpdateBinding.Replace(Object newValue) -> {
                    subject.eSet(sourceFeature, newValue);
                    object = newValue;
                }
                case ValueUpdateBinding.Insert(Object inserted, int index) -> {
                    //noinspection unchecked
                    var list = ((List<Object>) subject.eGet(sourceFeature));
                    if (index != -1) {
                        if (index >= list.size() || list.get(index) != inserted) {
                            list.add(index, inserted);
                        }
                    } else {
                        list.add(inserted);
                    }
                    object = inserted;
                }
                case ValueUpdateBinding.Remove(Object removed, int index) -> {
                    //noinspection unchecked
                    var list = ((List<Object>) subject.eGet(sourceFeature));
                    if (index != -1 && list.get(index) == removed) {
                        list.remove(index);
                    } else {
                        list.remove(removed);
                    }
                    object = removed;
                }
                default ->
                        throw new IllegalArgumentException("Unsupported value update type: " + value.getClass().getSimpleName());
            }

            if (isSourceFeatureAContainmentFeature && object instanceof EObject eObject) {
                context.trackOriginObjectAttachmentChange(eObject);
            }

            return FeatureBinding.ofOriginObject(subject, ValueBinding.ofFeature(subject, sourceFeature));
        }).orElseThrow();
    }

    @Override
    public Optional<EChange<EObject>> doGetChange(EChange<EObject> change) {
        return Optional.empty();
    }
}
