package tools.vitruv.compmodelcons.views.bindings;

public interface ValueUpdateBinding {
    record Unset() implements ValueUpdateBinding {

    }

    record Replace(Object newValue) implements ValueUpdateBinding {

    }

    record Insert(Object inserted, int index) implements ValueUpdateBinding {

    }

    record Remove(Object removed, int index) implements ValueUpdateBinding {

    }
}
