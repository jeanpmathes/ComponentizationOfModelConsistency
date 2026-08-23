package tools.vitruv.compmodelcons.change.correspondence;

import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.ViewChangePropagationContext;

public interface CorrespondenceTranslationStrategy {
  CorrespondenceObjectViewObjectTranslatorFactory createCorrespondenceResolverFactory(
      ResourceAccess resourceAccess);

  EditableCorrespondenceModelView<Correspondence> createTranslatedCorrespondenceModelView(
      EditableCorrespondenceModelView<Correspondence> inner, ViewChangePropagationContext context);
}
