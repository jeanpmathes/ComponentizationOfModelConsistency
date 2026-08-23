package tools.vitruv.compmodelcons.views.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewSelection;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.views.changederivation.StateBasedChangeResolutionStrategy;

public class CompositeView implements View {
  private final List<View> views;
  private final Map<EPackage, View> metamodelToView;

  public CompositeView(List<View> views) {
    this.views = views;

    this.metamodelToView = new HashMap<>();
    for (View view : views) {
      metamodelToView.put(view
                              .getViewType()
                              .getMetamodel(), view);
    }
  }

  @Override
  public Collection<EObject> getRootObjects() {
    return views
        .stream()
        .flatMap(view -> view
            .getRootObjects()
            .stream())
        .toList();
  }

  @Override
  public boolean isModified() {
    return views
        .stream()
        .anyMatch(View::isModified);
  }

  @Override
  public boolean isOutdated() {
    return views
        .stream()
        .anyMatch(View::isOutdated);
  }

  @Override
  public void update() {
    views.forEach(View::update);
  }

  @Override
  public boolean isClosed() {
    return views
        .stream()
        .allMatch(View::isClosed);
  }

  @Override
  public void registerRoot(EObject eObject, URI uri) {
    View view = metamodelToView.get(eObject
                                        .eClass()
                                        .getEPackage());
    if (view != null) {
      view.registerRoot(eObject, uri);
    }
  }

  @Override
  public void moveRoot(EObject eObject, URI uri) {
    View view = metamodelToView.get(eObject
                                        .eClass()
                                        .getEPackage());
    if (view != null) {
      view.moveRoot(eObject, uri);
    }
  }

  @Override
  public ViewSelection getSelection() {
    return eObject -> views
        .stream()
        .anyMatch(view -> view
            .getSelection()
            .isViewObjectSelected(eObject));
  }

  @Override
  public ViewType<? extends ViewSelector> getViewType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CommittableView withChangeRecordingTrait() {
    return new CompositeCommittableView(views
                                            .stream()
                                            .map(View::withChangeRecordingTrait)
                                            .toList());
  }

  @Override
  public CommittableView withChangeDerivingTrait(
      StateBasedChangeResolutionStrategy stateBasedChangeResolutionStrategy) {
    return new CompositeCommittableView(views
                                            .stream()
                                            .map(view -> view.withChangeDerivingTrait(
                                                stateBasedChangeResolutionStrategy))
                                            .toList());
  }

  @Override
  public void close() throws Exception {
    for (View view : views) {
      view.close();
    }

    views.clear();
    metamodelToView.clear();
  }
}
