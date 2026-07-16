package tools.vitruv.compmodelcons.views.internal.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.EChangeUtil;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.eobject.EObjectSubtractedEChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.recording.ChangeRecorder;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.impl.EditableViewCorrespondencesImpl;
import tools.vitruv.compmodelcons.views.impl.GetContextImpl;
import tools.vitruv.compmodelcons.views.impl.PutContextImpl;
import tools.vitruv.compmodelcons.views.internal.OriginResourceAccess;
import tools.vitruv.compmodelcons.views.internal.ViewResourceAccess;
import tools.vitruv.compmodelcons.views.operations.Root;

import java.util.ArrayList;
import java.util.List;

public class InternalViewImpl implements AutoCloseable {
    private final EditableViewCorrespondences correspondences = new EditableViewCorrespondencesImpl();
    private final Root structure;
    private final ViewResourceAccess viewResourceAccess;
    private final OriginResourceAccess originResourceAccess;

    private ChangeRecorder changeRecorder;

    private Root.ViewBinding viewBinding;

    public InternalViewImpl(Root structure, ViewResourceAccess viewResourceAccess, OriginResourceAccess originResourceAccess) {
        this.structure = structure;
        this.viewResourceAccess = viewResourceAccess;
        this.originResourceAccess = originResourceAccess;

        setupChangeRecorderAndBeginRecording();
    }

    private static List<EChange<EObject>> reorderChanges(List<EChange<EObject>> changes) {
        // While the documentation of ChangeRecorder.endRecording() states that the deletions are inserted
        // right after the change causing the removal, that is not the case.
        // See ChangeRecorder.postprocessRemovals() and its documentation.
        // However, I would really like it to be the case.

        List<EChange<EObject>> reorderedChanges = new ArrayList<>(changes);

        int numberOfDeletionsToReorder = 0;
        for (int index = reorderedChanges.size() - 1; index >= 0; index--) {
            if (reorderedChanges.get(index) instanceof DeleteEObject<EObject>) {
                numberOfDeletionsToReorder += 1;
            } else {
                break;
            }
        }

        while (numberOfDeletionsToReorder > 0) {
            DeleteEObject<EObject> deletion = (DeleteEObject<EObject>) reorderedChanges.getLast();
            reorderedChanges.removeLast();

            List<EChange<EObject>> associatedDeletions = new ArrayList<>();
            for (int index = reorderedChanges.size() - 1; index >= 0; index--) {
                EChange<EObject> eChange = reorderedChanges.get(index);
                if (eChange instanceof DeleteEObject<EObject> subtractedEChange
                        && EChangeUtil.isContainmentRemoval(subtractedEChange)
                        && subtractedEChange.getAffectedElement() == deletion.getAffectedElement()) {
                    associatedDeletions.addFirst(eChange);
                    reorderedChanges.remove(index);
                } else {
                    break;
                }
            }

            int indexOfCause = -1;

            for (int index = reorderedChanges.size() - 1; index >= 0; index--) {
                EChange<EObject> eChange = reorderedChanges.get(index);
                if (eChange instanceof EObjectSubtractedEChange<EObject> subtractedEChange && EChangeUtil.isContainmentRemoval(subtractedEChange) && subtractedEChange.getOldValue() == deletion.getAffectedElement()) {
                    indexOfCause = index;
                    break;
                }
            }

            if (indexOfCause == -1) {
                throw new IllegalStateException("Could not find the cause of the deletion");
            }

            int indexRightAfterCause = indexOfCause + 1;
            reorderedChanges.add(indexRightAfterCause, deletion);
            reorderedChanges.addAll(indexRightAfterCause, associatedDeletions);

            numberOfDeletionsToReorder -= associatedDeletions.size() + 1;
        }

        return reorderedChanges;
    }

    public EditableViewCorrespondences getCorrespondences() {
        return correspondences;
    }

    public void update() {
        endRecordingAndClose();
        viewResourceAccess.reset();
        doGet();
        setupChangeRecorderAndBeginRecording();
    }

    private void doGet() {
        viewBinding = structure.doGet(new GetContextImpl(originResourceAccess, viewResourceAccess, correspondences));
    }

    public void commit() {
        VitruviusChange<EObject> change = changeRecorder.endRecording();
        doPut(change);
    }

    private void doPut(VitruviusChange<EObject> change) {
        var context = new PutContextImpl(originResourceAccess, viewResourceAccess, correspondences);
        reorderChanges(change.getEChanges()).forEach(eChange -> viewBinding = structure.doPut(eChange, viewBinding, context));
        context.validateAttachmentState();
    }

    public List<EChange<EObject>> doGetChange(EChange<EObject> originChange) {
        return structure.doGetChange(originChange);
    }

    private void setupChangeRecorderAndBeginRecording() {
        changeRecorder = new ChangeRecorder(viewResourceAccess.getResourceSet());
        changeRecorder.addToRecording(viewResourceAccess.getResourceSet());
        changeRecorder.beginRecording();
    }

    private void endRecordingAndClose() {
        if (changeRecorder.isRecording()) {
            changeRecorder.endRecording();
        }
        changeRecorder.close();
        changeRecorder = null;
    }

    @Override
    public void close() {
        endRecordingAndClose();
    }
}
