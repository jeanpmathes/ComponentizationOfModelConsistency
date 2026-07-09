package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.FeatureBinding;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.Optional;

public class FeatureSource implements FeatureOperation {
    private final EClass sourceClass;
    private final EStructuralFeature sourceFeature;

    public FeatureSource(EStructuralFeature sourceFeature) {
        this.sourceClass = sourceFeature.getEContainingClass();
        this.sourceFeature = sourceFeature;
    }

    private Optional<EObject> getSource(ObjectBinding subject) {
        return subject.originObjects().stream().filter(eObject -> sourceClass.isSuperTypeOf(eObject.eClass())).findFirst();
    }

    @Override
    public FeatureBinding GET(ObjectBinding subject, GetContext context) {
        return getSource(subject).map(eObject -> {
            Object value = eObject.eGet(sourceFeature);
            return FeatureBinding.ofOriginObject(eObject, value);
        }).orElseThrow();
    }

    @Override
    public FeatureBinding PUT(EChange<EObject> change, FeatureBinding feature, ObjectBinding subject, Optional<Object> value, PutContext context) {
        return getSource(subject).map(eObject -> {
            if (value.isEmpty()) {
                eObject.eUnset(sourceFeature);
            } else {
                eObject.eSet(sourceFeature, value.get());
            }
            return FeatureBinding.ofOriginObject(eObject, eObject.eGet(sourceFeature));
        }).orElseThrow();
    }

    @Override
    public Optional<EChange<EObject>> GET_CHANGE(EChange<EObject> change) {
        return Optional.empty();
    }
}
