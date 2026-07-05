package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.jetbrains.annotations.UnknownNullability;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.compmodelcons.views.*;
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
                if (eClass.isSuperTypeOf(sourceClass)) {
                    continue;
                }
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
                    if (eReference.isContainment() && eReference.isMany() && eReference.getEReferenceType().isSuperTypeOf(sourceClass)) {
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
    public List<ObjectBinding> get(@UnknownNullability GetContext context) {
        return context.getOriginObjects(sourceClass).stream().map(ObjectBinding::ofOriginObject).toList();
    }

    @Override
    public ObjectBinding put(EChange<EObject> change, ObjectBinding target, PutContext context) {
        if (change instanceof CreateEObject<EObject> createEObject) {
            if (!target.originObjects().isEmpty()) {
                throw new IllegalArgumentException("Cannot create an origin object if there is already an origin object");
            }

            EObject created = sourceClass.getEPackage().getEFactoryInstance().create(sourceClass);

            context.getCorrespondences().addCorrespondence(List.of(created), createEObject.getAffectedElement());

            return ObjectBinding.ofOriginObject(created);
        }

        if (change instanceof DeleteEObject<EObject> deleteEObject) {
            if (target.originObjects().size() != 1) {
                throw new IllegalArgumentException("Cannot delete an origin object if that object is not singular");
            }

            EObject deleted = target.originObjects().get(0);
            context.getCorrespondences().removeCorrespondence(List.of(deleted), deleteEObject.getAffectedElement());

            return ObjectBinding.empty();
        }

        if (change instanceof InsertRootEObject<EObject> || change instanceof InsertNonRootEObject<EObject>) {
            if (target.originObjects().size() != 1) {
                throw new IllegalArgumentException("Cannot insert an origin object if that object is not singular");
            }

            EObject inserted = target.originObjects().get(0);

            if (isRoot) {
                context.addRootToOriginModel(sourceClass.getEPackage(), inserted);
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

            return ObjectBinding.ofOriginObject(inserted);
        }

        if (change instanceof RemoveRootEObject<EObject> || change instanceof RemoveNonRootEObject<EObject>) {
            if (target.originObjects().size() != 1) {
                throw new IllegalArgumentException("Cannot remove an origin object if that object is not singular");
            }

            EObject removed = target.originObjects().get(0);

            if (isRoot) {
                context.removeRootFromOriginModel(sourceClass.getEPackage(), removed);
            } else {
                if (container.isMany()) {
                    Utilities.getList(removed.eContainer(), container).remove(removed);
                } else {
                    removed.eContainer().eUnset(container);
                }
            }

            return ObjectBinding.ofOriginObject(removed);
        }

        throw new IllegalArgumentException("Inappropriate change type: " + change.getClass());
    }

    @Override
    public Optional<EChange<EObject>> getChange(EChange<EObject> change) {
        return Optional.empty();
    }
}
