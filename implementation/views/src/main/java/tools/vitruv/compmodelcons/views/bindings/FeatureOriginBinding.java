package tools.vitruv.compmodelcons.views.bindings;

import java.util.List;
import org.eclipse.emf.ecore.EObject;

public interface FeatureOriginBinding {
  static FeatureOriginBinding ofOriginObject(EObject eObject, ValueBinding value) {
    return new FeatureOriginBinding() {
      @Override
      public List<EObject> originSubjectObjects() {
        return List.of(eObject);
      }

      @Override
      public ValueBinding value() {
        return value;
      }
    };
  }

  static FeatureOriginBinding ofOriginBinding(OriginBinding originBinding, ValueBinding value) {
    return new FeatureOriginBinding() {
      @Override
      public List<EObject> originSubjectObjects() {
        return originBinding.originObjects();
      }

      @Override
      public ValueBinding value() {
        return value;
      }
    };
  }

  List<EObject> originSubjectObjects();

  ValueBinding value();
}
