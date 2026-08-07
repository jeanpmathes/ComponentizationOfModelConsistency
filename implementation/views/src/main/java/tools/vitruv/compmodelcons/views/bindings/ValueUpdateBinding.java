package tools.vitruv.compmodelcons.views.bindings;

import java.util.List;

public interface ValueUpdateBinding {
    record Unset() implements ValueUpdateBinding {

    }

    record Replace(Object newValue) implements ValueUpdateBinding {

    }

    record Insert(Object inserted, int index) implements ValueUpdateBinding {

    }

    record Remove(Object removed, int index) implements ValueUpdateBinding {

    }

    static <T> void insert(List<T> list, T inserted, int index) {
        if (index != -1) {
            if (index >= list.size() || list.get(index) != inserted) {
                list.add(index, inserted);
            }
        } else {
            list.add(inserted);
        }
    }

    static <T> void remove(List<T> list, T removed, int index) {
        if (index != -1 && list.get(index) == removed) {
            list.remove(index);
        } else {
            list.remove(removed);
        }
    }
}
