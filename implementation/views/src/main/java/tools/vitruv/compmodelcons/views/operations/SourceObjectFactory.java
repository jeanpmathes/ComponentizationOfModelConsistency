package tools.vitruv.compmodelcons.views.operations;

import java.util.Objects;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

@FunctionalInterface
public interface SourceObjectFactory {
  static SourceObjectFactory requireNonNullElseDefault(SourceObjectFactory factory,
                                                       EClass sourceClass) {
    return Objects.requireNonNullElseGet(factory, () -> ignored -> sourceClass
        .getEPackage()
        .getEFactoryInstance()
        .create(sourceClass));
  }

  EObject createOriginObject(EObject viewObject);
}
