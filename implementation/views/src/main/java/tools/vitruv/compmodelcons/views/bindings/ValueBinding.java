package tools.vitruv.compmodelcons.views.bindings;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.List;

public interface ValueBinding {
    static ValueBinding of(Object single) {
        return new Single(single);
    }

    static ValueBinding ofFeature(EObject subject, EStructuralFeature feature) {
        if (subject.eIsSet(feature)) {
            if (feature.isMany()) {
                return new Many((List<?>) subject.eGet(feature));
            } else {
                return new Single(subject.eGet(feature));
            }
        } else {
            return new Unset();
        }
    }

    record Unset() implements ValueBinding {

    }

    record Single(Object value) implements ValueBinding {

    }

    record Many(List<?> values) implements ValueBinding {

    }
}
