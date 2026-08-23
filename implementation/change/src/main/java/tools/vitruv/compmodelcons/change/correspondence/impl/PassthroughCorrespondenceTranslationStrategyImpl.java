package tools.vitruv.compmodelcons.change.correspondence.impl;

import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.ViewChangePropagationContext;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslatorFactory;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceTranslationStrategy;

public class PassthroughCorrespondenceTranslationStrategyImpl implements
    CorrespondenceTranslationStrategy {
  @Override
  public CorrespondenceObjectViewObjectTranslatorFactory createCorrespondenceResolverFactory(
      ResourceAccess resourceAccess) {
    return (viewType, viewResourceAccess) ->
        new PassthroughCorrespondenceObjectViewObjectTranslatorImpl(
            MetamodelDescriptor.of(viewType.getMetamodel()));
  }

  @Override
  public EditableCorrespondenceModelView<Correspondence> createTranslatedCorrespondenceModelView(
      EditableCorrespondenceModelView<Correspondence> inner, ViewChangePropagationContext context) {
    return inner;
  }
}
