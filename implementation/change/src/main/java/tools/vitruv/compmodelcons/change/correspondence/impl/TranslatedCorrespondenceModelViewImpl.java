package tools.vitruv.compmodelcons.change.correspondence.impl;

import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.compmodelcons.change.correspondence.TranslatedCorrespondenceModelView;

public class TranslatedCorrespondenceModelViewImpl implements
    TranslatedCorrespondenceModelView {
  private final EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView;

  public TranslatedCorrespondenceModelViewImpl(
      EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
    this.editableCorrespondenceModelView = editableCorrespondenceModelView;
  }

  @Override
  public EditableCorrespondenceModelView<Correspondence> getCorrespondenceModelView() {
    return this.editableCorrespondenceModelView;
  }

  @Override
  public void close() {

  }
}
