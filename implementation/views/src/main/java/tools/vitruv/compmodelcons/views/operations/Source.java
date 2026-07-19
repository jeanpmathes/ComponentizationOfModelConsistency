package tools.vitruv.compmodelcons.views.operations;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.compmodelcons.views.DynamicModels;
import tools.vitruv.compmodelcons.views.GetContext;
import tools.vitruv.compmodelcons.views.PutContext;
import tools.vitruv.compmodelcons.views.bindings.ObjectBinding;

import java.util.List;

public class Source implements OriginOperation {
    private final EClass sourceClass;
    private final boolean isRoot;
    private final EReference container;

    public Source(EClass sourceClass) {
        this.sourceClass = sourceClass;
        this.isRoot = DynamicModels.isRoot(sourceClass);
        this.container = isRoot ? null : DynamicModels.getUnambiguousContainer(sourceClass);
    }

    public static void attachedCreatedOriginObject(EObject created, EClass sourceClass, boolean isRoot, EReference container, PutContext context) {
        if (isRoot) {
            context.addRootToDefaultOriginModel(sourceClass.getEPackage(), created);
        } else if (container != null) {
            List<EObject> candidates = context.getOriginObjects(container.getEContainingClass());

            if (candidates.size() == 1) {
                if (container.isMany()) {
                    DynamicModels.getList(candidates.getFirst(), container).add(created);
                } else {
                    candidates.getFirst().eSet(container, created);
                }
            } else {
                context.trackUnattachedCreatedOriginObject(created);
            }
        } else {
            context.trackUnattachedCreatedOriginObject(created);
        }
    }

    public static void detachDeletedOriginObject(EObject deleted, EClass sourceClass, boolean isRoot, EReference container, PutContext context) {
        if (isRoot) {
            if (deleted.eResource() != null) {
                context.removeRootFromDefaultOriginModel(sourceClass.getEPackage(), deleted);
            }
        } else if (container != null) {
            if (deleted.eContainer() != null) {
                if (container.isMany()) {
                    DynamicModels.getList(deleted.eContainer(), container).remove(deleted);
                } else {
                    deleted.eContainer().eUnset(container);
                }
            }
        } else {
            context.trackUndetachedDeletedOriginObject(deleted);
        }
    }

    @Override
    public List<ObjectBinding> doGet(GetContext context) {
        return context.getOriginObjects(sourceClass).stream().map(ObjectBinding::ofOriginObject).toList();
    }

    @Override
    public ObjectBinding doPut(EChange<EObject> viewChange, ObjectBinding target, PutContext context) {
        if (viewChange instanceof CreateEObject<EObject> createEObject) {
            if (!target.originObjects().isEmpty()) {
                throw new IllegalArgumentException("Cannot create an origin object if there is already an origin object");
            }

            EObject created = sourceClass.getEPackage().getEFactoryInstance().create(sourceClass);
            context.getCorrespondences().addCorrespondence(List.of(created), createEObject.getAffectedElement());

            attachedCreatedOriginObject(created, sourceClass, isRoot, container, context);

            return ObjectBinding.ofOriginObject(created);
        }

        if (viewChange instanceof DeleteEObject<EObject> deleteEObject) {
            EObject deleted = target.originObjects().getFirst();
            context.getCorrespondences().removeCorrespondence(List.of(deleted), deleteEObject.getAffectedElement());

            detachDeletedOriginObject(deleted, sourceClass, isRoot, container, context);

            return ObjectBinding.empty();
        }

        if (viewChange instanceof InsertRootEObject<EObject> insertRootEObject) {
            EObject inserted = target.originObjects().getFirst();

            if (isRoot) {
                context.moveRootToOtherOriginModel(sourceClass.getEPackage(), inserted, insertRootEObject.getResource().getURI());
            }

            return ObjectBinding.ofOriginObject(inserted);
        }

        if (viewChange instanceof RemoveRootEObject<EObject>) {
            EObject removed = target.originObjects().getFirst();

            if (isRoot) {
                context.moveRootToDefaultOriginModel(sourceClass.getEPackage(), removed);
            }

            return ObjectBinding.ofOriginObject(removed);
        }

        throw new IllegalArgumentException("Inappropriate change type: " + viewChange.getClass());
    }

    @Override
    public List<ObjectBinding> doUpdatingGet(List<ObjectBinding> previous, EChange<EObject> originChange, GetContext context) {
        return List.of();
    }
}
