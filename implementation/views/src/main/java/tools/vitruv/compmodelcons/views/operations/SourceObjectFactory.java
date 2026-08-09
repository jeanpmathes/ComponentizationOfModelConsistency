package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import java.util.Objects;

@FunctionalInterface
public interface SourceObjectFactory {
    static SourceObjectFactory requireNonNullElseDefault(SourceObjectFactory factory, EClass sourceClass) {
        return Objects.requireNonNullElseGet(factory, () -> ignored -> sourceClass.getEPackage().getEFactoryInstance()
                .create(sourceClass));
    }
    EObject createOriginObject(EObject viewObject);
}
