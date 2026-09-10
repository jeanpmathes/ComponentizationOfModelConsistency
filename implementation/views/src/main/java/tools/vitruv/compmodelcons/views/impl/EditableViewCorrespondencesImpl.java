package tools.vitruv.compmodelcons.views.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.compmodelcons.views.EditableViewCorrespondences;
import tools.vitruv.compmodelcons.views.ViewCorrespondences;

public class EditableViewCorrespondencesImpl implements EditableViewCorrespondences {
  private final BiMap<OriginKey, ViewKey> correspondences = HashBiMap.create();
  private final SetMultimap<PartialOriginKey, ViewKey> partialCorrespondences =
      HashMultimap.create();

  public EditableViewCorrespondencesImpl() {

  }

  @Override
  public EObject getCorrespondingViewObjectForOriginObjects(
      List<EObject> originObjects,
      EClass viewClass) {
    return correspondences
               .get(new OriginKey(originObjects, viewClass))
               .viewObject();
  }

  @Override
  public List<EObject> getCorrespondingOriginObjectsForViewObject(EObject viewObject) {
    OriginKey originKey = correspondences.inverse().get(new ViewKey(viewObject));
    return originKey == null ? null : originKey.originObjects();
  }

  @Override
  public Set<EObject> getCorrespondingViewObjectForPartialOriginObjects(
      EObject originObject,
      EClass viewClass) {
    return partialCorrespondences
               .get(new PartialOriginKey(originObject, viewClass))
               .stream()
               .map(ViewKey::viewObject)
               .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean correspond(List<EObject> originObjects, EObject viewObject) {
    return correspondences.containsKey(new OriginKey(originObjects, viewObject.eClass())) &&
               correspondences
                   .get(new OriginKey(originObjects, viewObject.eClass()))
                   .equals(new ViewKey(viewObject));
  }

  @Override
  public void forEach(BiConsumer<List<EObject>, EObject> action) {
    correspondences.forEach(
        (originKey, viewKey) -> action.accept(originKey.originObjects(), viewKey.viewObject()));
  }

  private record OriginKey(List<EObject> originObjects, EClass viewClass) {
  }

  private record ViewKey(EObject viewObject) {
  }

  private record PartialOriginKey(EObject originObject, EClass viewClass) {
  }

  @Override
  public void addCorrespondence(List<EObject> originObjects, EObject viewObject) {
    if (originObjects.isEmpty()) {
      return; // Handle the case of implicit root, which does not have an origin object.
    }

    var viewKey = new ViewKey(viewObject);

    OriginKey originKey = new OriginKey(originObjects, viewObject.eClass());
    ViewKey replacedViewKey = correspondences.put(originKey, viewKey);

    if (replacedViewKey != null && !replacedViewKey.equals(viewKey)) {
      for (var originObject : originObjects) {
        partialCorrespondences.remove(new PartialOriginKey(originObject, viewObject.eClass()),
                                      replacedViewKey);
      }
    }

    for (var originObject : originObjects) {
      partialCorrespondences.put(new PartialOriginKey(originObject, viewObject.eClass()), viewKey);
    }
  }

  @Override
  public void joinCorrespondence(
      List<EObject> currentOriginObjects,
      List<EObject> addedOriginObjects, EObject viewObject) {
    removeCorrespondence(currentOriginObjects, viewObject);

    List<EObject> newOriginObjects = new ArrayList<>(currentOriginObjects);
    newOriginObjects.addAll(addedOriginObjects);

    addCorrespondence(newOriginObjects, viewObject);
  }

  @Override
  public void removeCorrespondence(List<EObject> originObjects, EObject viewObject) {
    var viewKey = new ViewKey(viewObject);

    correspondences.remove(new OriginKey(originObjects, viewObject.eClass()), viewKey);

    for (var originObject : originObjects) {
      partialCorrespondences.remove(new PartialOriginKey(originObject, viewObject.eClass()),
                                    viewKey);
    }
  }

  @Override
  public void unjoinCorrespondence(
      List<EObject> currentOriginObjects,
      List<EObject> removedOriginObjects, EObject viewObject) {
    removeCorrespondence(currentOriginObjects, viewObject);

    List<EObject> newOriginObjects = new ArrayList<>(currentOriginObjects);
    newOriginObjects.removeAll(removedOriginObjects);

    addCorrespondence(newOriginObjects, viewObject);
  }

  @Override
  public void update(
      ViewCorrespondences newCorrespondences,
      Function<EObject, EObject> originObjectMapper,
      Function<EObject, EObject> viewObjectMapper) {
    HashBiMap.create(correspondences).forEach((key, value) -> {
      List<EObject> originalOriginObjects = key.originObjects();
      List<EObject> mappedOriginObjects =
          originalOriginObjects.stream().map(originObjectMapper).toList();
      if (!mappedOriginObjects.equals(originalOriginObjects)) {
        EObject viewObject = value.viewObject();
        removeCorrespondence(key.originObjects(), viewObject);
        addCorrespondence(mappedOriginObjects, viewObject);
      }
    });

    newCorrespondences.forEach(
        (originObjects, viewObject) -> {
          EObject mappedViewObject = viewObjectMapper.apply(viewObject);

          OriginKey previousOriginKey = correspondences.inverse().get(
              new ViewKey(mappedViewObject));
          if (previousOriginKey != null) {
            removeCorrespondence(previousOriginKey.originObjects(), mappedViewObject);
          }
          addCorrespondence(originObjects, mappedViewObject);
        });
  }
}
