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
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

import java.util.List;

public class Source implements OriginOperation {
    private final EClass              sourceClass;
    private final SourceObjectFactory sourceObjectFactory;
    private final boolean             isRoot;
    private final EReference          container;

    public Source(EClass sourceClass, SourceObjectFactory sourceObjectFactory) {
        this.sourceClass         = sourceClass;
        this.sourceObjectFactory = SourceObjectFactory.requireNonNullElseDefault(sourceObjectFactory, sourceClass);
        this.isRoot              = DynamicModels.isRoot(sourceClass);
        this.container           = isRoot ? null : DynamicModels.getUnambiguousContainer(sourceClass);
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
            }
        }

        context.notifyOriginObjectCreated(created);
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
        }

        context.notifyOriginObjectDeleted(deleted);
    }

    @Override public List<OriginBinding> doGet(GetContext context) {
        return context.getOriginObjects(sourceClass).stream().map(OriginBinding::of).toList();
    }

    @Override public OriginBinding doPut(EChange<EObject> viewChange, OriginBinding target, PutContext context) {
        if (viewChange instanceof CreateEObject<EObject> createEObject) {
            assert target.originObjects().isEmpty();

            EObject created = sourceObjectFactory.createOriginObject(createEObject.getAffectedElement());
            context.getCorrespondences().addCorrespondence(List.of(created), createEObject.getAffectedElement());

            attachedCreatedOriginObject(created, sourceClass, isRoot, container, context);

            return OriginBinding.of(created);
        }

        if (viewChange instanceof DeleteEObject<EObject> deleteEObject) {
            EObject deleted = target.originObjects().getFirst();
            context.getCorrespondences().removeCorrespondence(List.of(deleted), deleteEObject.getAffectedElement());

            detachDeletedOriginObject(deleted, sourceClass, isRoot, container, context);

            return OriginBinding.empty();
        }

        if (viewChange instanceof InsertRootEObject<EObject> insertRootEObject) {
            EObject inserted = target.originObjects().getFirst();

            if (isRoot) {
                context.moveRootToOtherOriginModel(sourceClass.getEPackage(), inserted,
                                                   insertRootEObject.getResource().getURI());
            }

            return OriginBinding.of(inserted);
        }

        if (viewChange instanceof RemoveRootEObject<EObject>) {
            EObject removed = target.originObjects().getFirst();

            if (isRoot) {
                context.moveRootToDefaultOriginModel(sourceClass.getEPackage(), removed);
            }

            return OriginBinding.of(removed);
        }

        throw new IllegalArgumentException("Inappropriate change type: " + viewChange.getClass());
    }

    @Override
    public List<OriginBinding> doUpdatingGet(List<OriginBinding> previous, EChange<EObject> originChange, GetContext context) {
        return List.of();
    }
}
