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

import java.util.ArrayList;
import java.util.List;

public class Join implements OriginOperation {
    private final EClass sourceClass;
    private final boolean isRoot;
    private final EReference container;
    private final OriginOperation origin;

    public Join(EClass sourceClass, OriginOperation origin) {
        this.sourceClass = sourceClass;
        this.isRoot = DynamicModels.isRoot(sourceClass);
        this.container = isRoot ? null : DynamicModels.getUnambiguousContainer(sourceClass);
        this.origin = origin;
    }

    @Override
    public List<ObjectBinding> doGet(GetContext context) {
        return origin.doGet(context).stream()
                .flatMap(originBinding -> context.getOriginObjects(sourceClass).stream()
                        .map(joinedBinding -> (ObjectBinding) new JoinObjectBindingImpl(originBinding, joinedBinding)))
                .toList();
    }

    @Override
    public ObjectBinding doPut(EChange<EObject> viewChange, ObjectBinding target, PutContext context) {
        if (viewChange instanceof CreateEObject<EObject> createEObject) {
            if (!target.originObjects().isEmpty()) {
                throw new IllegalArgumentException("Cannot create an origin object if there is already an origin object");
            }

            ObjectBinding originBinding = origin.doPut(viewChange, target, context);

            EObject created = sourceClass.getEPackage().getEFactoryInstance().create(sourceClass);
            context.getCorrespondences().joinCorrespondence(originBinding.originObjects(), List.of(created), createEObject.getAffectedElement());
            Source.attachedCreatedOriginObject(created, sourceClass, isRoot, container, context);

            return new JoinObjectBindingImpl(originBinding, created);
        }

        if (viewChange instanceof DeleteEObject<EObject> deleteEObject) {
            JoinObjectBindingImpl binding = (JoinObjectBindingImpl) target;

            EObject deleted = binding.originObject();
            context.getCorrespondences().unjoinCorrespondence(binding.originObjects(), List.of(deleted), deleteEObject.getAffectedElement());
            Source.detachDeletedOriginObject(deleted, sourceClass, isRoot, container, context);

            origin.doPut(viewChange, binding.originBinding(), context);

            return ObjectBinding.empty();
        }

        if (viewChange instanceof InsertRootEObject<EObject> insertRootEObject) {
            JoinObjectBindingImpl binding = (JoinObjectBindingImpl) target;
            EObject inserted = binding.originObject();

            if (isRoot) {
                context.moveRootToOtherOriginModel(sourceClass.getEPackage(), inserted, insertRootEObject.getResource().getURI());
            }

            return ObjectBinding.ofOriginObject(inserted);
        }

        if (viewChange instanceof RemoveRootEObject<EObject> removeRootEObject) {
            JoinObjectBindingImpl binding = (JoinObjectBindingImpl) target;
            EObject removed = binding.originObject();

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

    private static final class JoinObjectBindingImpl implements ObjectBinding {
        private final ObjectBinding originBinding;
        private final EObject originObject;
        private final List<EObject> originObjects = new ArrayList<>();

        private JoinObjectBindingImpl(ObjectBinding originBinding, EObject originObject) {
            this.originBinding = originBinding;
            this.originObject = originObject;

            this.originObjects.addAll(originBinding.originObjects());
            this.originObjects.add(originObject);
        }

        @Override
        public List<EObject> originObjects() {
            return originObjects;
        }

        @Override
        public EObject viewObject() {
            throw new UnsupportedOperationException();
        }

        public ObjectBinding originBinding() {
            return originBinding;
        }

        public EObject originObject() {
            return originObject;
        }
    }
}
