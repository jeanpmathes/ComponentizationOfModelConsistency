package tools.vitruv.compmodelcons.views.impl;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.framework.views.ChangeableViewSource;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewSelection;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.impl.ViewCreatingViewType;

import java.util.Collection;
import java.util.List;

public class DefaultSelector implements ViewSelector {

    public DefaultSelector(ViewCreatingViewType<DefaultSelector, HierarchicalId> viewType, ChangeableViewSource viewSource) {
        this.viewType = viewType;
        this.viewSource = viewSource;
    }

    private final ViewCreatingViewType<DefaultSelector, HierarchicalId> viewType;

    private final ChangeableViewSource viewSource;

    public ChangeableViewSource getViewSource() {
        return viewSource;
    }

    @Override
    public Collection<EObject> getSelectableElements() {
        return List.of();
    }

    @Override
    public boolean isSelectable(EObject eObject) {
        return true;
    }

    @Override
    public boolean isSelected(EObject eObject) {
        return true;
    }

    @Override
    public void setSelected(EObject eObject, boolean selected) { }

    @Override
    public boolean isViewObjectSelected(EObject eObject) {
        return true;
    }

    @Override
    public View createView() {
        return viewType.createView(this);
    }

    @Override
    public ViewSelection getSelection() {
        return this;
    }

    @Override
    public boolean isValid() {
        return true;
    }
    
}
