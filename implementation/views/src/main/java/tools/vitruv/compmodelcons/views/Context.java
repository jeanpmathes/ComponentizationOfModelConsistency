package tools.vitruv.compmodelcons.views;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;

public interface Context {
    List<EObject> getOriginObjects(EClass eClass);

    Resource getOriginModel(EPackage ePackage);

    Resource getViewModel();

    EditableViewCorrespondences getCorrespondences();

    default ObjectBinding getBindingForViewObject(EObject eObject) {
        return ObjectBinding.ofViewObject(eObject, getCorrespondences());
    }

    default ObjectBinding getBindingForOriginObjects(List<EObject> eObject) {
        return ObjectBinding.ofOriginObjects(eObject, getCorrespondences());
    }
}
