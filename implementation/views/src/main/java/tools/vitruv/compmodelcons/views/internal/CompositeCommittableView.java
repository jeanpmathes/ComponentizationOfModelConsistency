package tools.vitruv.compmodelcons.views.internal;

import java.util.List;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;

public class CompositeCommittableView extends CompositeView implements CommittableView {
  private final List<CommittableView> committableViews;

  public CompositeCommittableView(List<CommittableView> views) {
    super(views
              .stream()
              .map(View.class::cast)
              .toList());
    this.committableViews = views;
  }

  @Override
  public void commitChanges() {
    committableViews
        .stream()
        .filter(View::isModified)
        .forEach(CommittableView::commitChanges);
  }

  @Override
  public void close() throws Exception {
    super.close();

    committableViews.clear();
  }
}
