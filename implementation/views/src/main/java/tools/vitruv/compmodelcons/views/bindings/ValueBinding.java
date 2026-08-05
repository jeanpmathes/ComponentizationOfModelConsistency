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
                return new Many(List.copyOf((List<?>) subject.eGet(feature)));
            } else {
                return new Single(subject.eGet(feature));
            }
        } else {
            return new Unset();
        }
    }

    static ValueBinding ofDynamic(Object object) {
        if (object == null) {
            return new Unset();
        }
        if (object instanceof List) {
            return new Many((List<?>) object);
        }
        return new Single(object);
    }

    record Unset() implements ValueBinding {

    }

    record Single(Object value) implements ValueBinding {

    }

    record Many(List<?> values) implements ValueBinding {

    }
}
