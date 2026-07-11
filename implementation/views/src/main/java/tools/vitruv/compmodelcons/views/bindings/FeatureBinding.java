package tools.vitruv.compmodelcons.views.bindings;

import org.eclipse.emf.ecore.EObject;

import java.util.List;

public interface FeatureBinding {
    static FeatureBinding ofOriginObject(EObject eObject, ValueBinding value) {
        return new FeatureBinding() {
            @Override
            public List<EObject> originSubjectObjects() {
                return List.of(eObject);
            }

            @Override
            public EObject viewSubjectObject() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ValueBinding value() {
                return value;
            }
        };
    }

    List<EObject> originSubjectObjects();

    EObject viewSubjectObject();

    ValueBinding value();
}
