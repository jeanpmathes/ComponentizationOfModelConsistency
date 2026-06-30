package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.compmodelcons.views.Context;
import tools.vitruv.compmodelcons.views.InsertNonRootEObject;
import tools.vitruv.compmodelcons.views.RemoveNonRootEObject;
import tools.vitruv.compmodelcons.views.Utilities;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Source implements Operation {
    private final EClass sourceClass;
    private final boolean isRoot;
    private final EReference container;

    public Source(EClass sourceClass) {
        this.sourceClass = sourceClass;
        this.isRoot = isRoot(sourceClass);
        this.container = isRoot ? null : getContainer(sourceClass);
    }

    private static boolean isRoot(EClass sourceClass) {
        for (EClassifier eClassifier : sourceClass.getEPackage().getEClassifiers()) {
            if (eClassifier instanceof EClass eClass) {
                for (EReference eReference : eClass.getEReferences()) {
                    if (eReference.isContainment() && eReference.getEReferenceType().isSuperTypeOf(sourceClass)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static EReference getContainer(EClass sourceClass) {
        Set<EReference> containers = new HashSet<>();

        for (EClassifier eClassifier : sourceClass.getEPackage().getEClassifiers()) {
            if (eClassifier instanceof EClass eClass) {
                for (EReference eReference : eClass.getEReferences()) {
                    if (eReference.isContainment() && eReference.getEReferenceType().isSuperTypeOf(sourceClass)) {
                        containers.add(eReference);
                    }
                }
            }
        }

        if (containers.isEmpty()) {
            throw new IllegalArgumentException("No container found for " + sourceClass);
        }

        if (containers.size() > 1) {
            throw new IllegalArgumentException("Multiple containers found for " + sourceClass);
        }

        return containers.iterator().next();
    }

    @Override
    public List<ObjectBinding> get(Context context) {
        return context.getOriginObjects(sourceClass).stream().map(ObjectBinding::ofOriginObject).toList();
    }

    @Override
    public Optional<ObjectBinding> put(EChange<EObject> eChange, ObjectBinding target, Context context) {
        if (eChange instanceof CreateEObject<EObject> createEObject) {
            if (!target.originObjects().isEmpty()) {
                throw new IllegalArgumentException("Cannot create an origin object if there is already an origin object");
            }

            EObject created = sourceClass.getEPackage().getEFactoryInstance().create(sourceClass);
            context.getCorrespondences().addCorrespondence(List.of(created), createEObject.getAffectedElement());

            return Optional.of(ObjectBinding.ofOriginObject(created));
        }

        if (eChange instanceof DeleteEObject<EObject> deleteEObject) {
            if (target.originObjects().size() != 1) {
                throw new IllegalArgumentException("Cannot delete an origin object if that object is not singular");
            }

            EObject deleted = target.originObjects().get(0);
            context.getCorrespondences().removeCorrespondence(List.of(deleted), deleteEObject.getAffectedElement());

            return Optional.empty();
        }

        if (eChange instanceof InsertRootEObject<EObject> || eChange instanceof InsertNonRootEObject<EObject>) {
            if (target.originObjects().size() != 1) {
                throw new IllegalArgumentException("Cannot insert an origin object if that object is not singular");
            }

            EObject inserted = target.originObjects().get(0);

            if (isRoot) {
                context.getOriginModel(sourceClass.getEPackage()).getContents().add(inserted);
            } else {
                List<EObject> candidates = context.getOriginObjects(container.getEContainingClass());

                if (candidates.size() != 1) {
                    throw new IllegalArgumentException("Could not find a singular container for insertion");
                }

                if (container.isMany()) {
                    Utilities.getList(candidates.get(0), container).add(inserted);
                } else {
                    candidates.get(0).eSet(container, inserted);
                }
            }

            return Optional.of(ObjectBinding.ofOriginObject(inserted));
        }

        if (eChange instanceof RemoveRootEObject<EObject> || eChange instanceof RemoveNonRootEObject<EObject>) {
            if (target.originObjects().size() != 1) {
                throw new IllegalArgumentException("Cannot remove an origin object if that object is not singular");
            }

            EObject removed = target.originObjects().get(0);

            if (isRoot) {
                context.getOriginModel(sourceClass.getEPackage()).getContents().remove(removed);
            } else {
                if (container.isMany()) {
                    Utilities.getList(removed.eContainer(), container).remove(removed);
                } else {
                    removed.eContainer().eUnset(container);
                }
            }

            return Optional.of(ObjectBinding.ofOriginObject(removed));
        }

        // todo: maybe it needs other events for insertion / removal of objects that are not root in view
        // todo: or maybe the delete / remove changes need to insert as well, but only if not root in view (but how to get whether the view element is root or not?)

        return Optional.empty();
    }

    @Override
    public Optional<EChange<EObject>> getChange(EChange<EObject> change) {
        return Optional.empty();
    }
}
