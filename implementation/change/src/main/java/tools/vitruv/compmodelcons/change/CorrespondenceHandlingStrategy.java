package tools.vitruv.compmodelcons.change;

import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;

public interface CorrespondenceHandlingStrategy {
    EditableCorrespondenceModelView<Correspondence> getLiftedCorrespondenceModel(EditableCorrespondenceModelView<Correspondence> correspondenceModel);
}
