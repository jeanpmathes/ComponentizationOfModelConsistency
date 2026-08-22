package tools.vitruv.compmodelcons.change.correspondence.impl;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceResolver;

public class TranslatingEditableCorrespondenceModelViewImpl<C extends Correspondence>
    extends TranslatingCorrespondenceModelViewImpl<C> implements
    EditableCorrespondenceModelView<C> {
  private final EditableCorrespondenceModelView<C> editableInner;

  public TranslatingEditableCorrespondenceModelViewImpl(EditableCorrespondenceModelView<C> inner,
                                                        CorrespondenceResolver sourceResolver,
                                                        CorrespondenceResolver targetResolver) {
    super(inner, sourceResolver, targetResolver);
    this.editableInner = inner;
  }

  @Override
  public C addCorrespondenceBetween(List<EObject> first, List<EObject> second, String tag) {
    return editableInner.addCorrespondenceBetween(getCorrespondenceEObjects(first, true),
                                                  getCorrespondenceEObjects(second, true), tag);
  }

  @Override
  public Set<C> removeCorrespondencesBetween(List<EObject> first, List<EObject> second,
                                             String tag) {
    List<EObject> firstCorrespondenceObjects = getCorrespondenceEObjects(first, false);
    List<EObject> secondCorrespondenceObjects = getCorrespondenceEObjects(second, false);

    if (firstCorrespondenceObjects == null || secondCorrespondenceObjects == null) {
      return Set.of();
    }

    return editableInner.removeCorrespondencesBetween(firstCorrespondenceObjects,
                                                      secondCorrespondenceObjects, tag);
  }

  @Override
  public <V extends C> EditableCorrespondenceModelView<V> getEditableView(
      Class<V> correspondenceType, Supplier<V> supplier) {
    return new TranslatingEditableCorrespondenceModelViewImpl<>(
        editableInner.getEditableView(correspondenceType, supplier), sourceResolver,
        targetResolver);
  }
}
