package tools.vitruv.compmodelcons.views.expressions;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.compmodelcons.views.bindings.OriginBinding;

import java.util.List;
import java.util.Objects;

public class FeatureCondition implements Condition {
    private final int leftIndex;
    private final EStructuralFeature leftFeature;
    private final int rightIndex;
    private final EStructuralFeature rightFeature;

    public FeatureCondition(int leftIndex, EStructuralFeature leftFeature, int rightIndex, EStructuralFeature rightFeature) {
        this.leftIndex = leftIndex;
        this.leftFeature = leftFeature;
        this.rightIndex = rightIndex;
        this.rightFeature = rightFeature;
    }

    @Override
    public boolean evaluate(OriginBinding originBinding) {
        List<EObject> originObjects = originBinding.originObjects();

        EObject left = originObjects.get(leftIndex);
        EObject right = originObjects.get(rightIndex);

        if (left == null || right == null) {
            return false;
        }

        return Objects.equals(left.eGet(leftFeature), right.eGet(rightFeature));
    }
}
