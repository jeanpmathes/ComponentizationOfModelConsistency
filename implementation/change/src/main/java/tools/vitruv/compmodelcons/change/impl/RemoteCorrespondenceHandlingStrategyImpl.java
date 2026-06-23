package tools.vitruv.compmodelcons.change.impl;

import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.compmodelcons.change.CorrespondenceHandlingStrategy;

public class RemoteCorrespondenceHandlingStrategyImpl implements CorrespondenceHandlingStrategy {
    @Override
    public EditableCorrespondenceModelView<Correspondence> getLiftedCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        return correspondenceModel;
    }
}
