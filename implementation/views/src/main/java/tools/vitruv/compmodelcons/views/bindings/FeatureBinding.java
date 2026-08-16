package tools.vitruv.compmodelcons.views.bindings;

import java.util.List;
import org.eclipse.emf.ecore.EObject;

public interface FeatureBinding extends FeatureOriginBinding {
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

  EObject viewSubjectObject();
}
