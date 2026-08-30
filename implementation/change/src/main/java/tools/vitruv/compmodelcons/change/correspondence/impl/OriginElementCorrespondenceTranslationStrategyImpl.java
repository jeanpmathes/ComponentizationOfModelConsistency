package tools.vitruv.compmodelcons.change.correspondence.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.model.CorrespondenceModel;
import tools.vitruv.change.correspondence.view.CorrespondenceModelView;
import tools.vitruv.change.correspondence.view.CorrespondenceModelViewFactory;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.compmodelcons.change.ViewChangePropagationContext;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceObjectViewObjectTranslatorFactory;
import tools.vitruv.compmodelcons.change.correspondence.CorrespondenceTranslationStrategy;
import tools.vitruv.compmodelcons.change.correspondence.TranslatedCorrespondenceModelView;
import tools.vitruv.compmodelcons.change.correspondence.ViewCorrespondences;

public class OriginElementCorrespondenceTranslationStrategyImpl
    implements CorrespondenceTranslationStrategy {
  @Override
  public CorrespondenceObjectViewObjectTranslatorFactory createCorrespondenceResolverFactory(
      ResourceAccess resourceAccess) {
    return (viewType, viewResourceAccess) -> new PassthroughCorrespondenceObjectViewObjectTranslatorImpl(
        MetamodelDescriptor.of(viewType.getMetamodel()));
  }

  @Override
  public TranslatedCorrespondenceModelView createTranslatedCorrespondenceModelView(
      EditableCorrespondenceModelView<Correspondence> correspondenceModel,
      ViewChangePropagationContext context) {
    TranslatedCorrespondenceModel translatedCorrespondenceModel =
        new TranslatedCorrespondenceModel(correspondenceModel, context);

    return new TranslatedCorrespondenceModelView() {
      @Override
      public EditableCorrespondenceModelView<Correspondence> getCorrespondenceModelView() {
        return CorrespondenceModelViewFactory.createEditableCorrespondenceModelView(
            translatedCorrespondenceModel);
      }

      @Override
      public void close() {
        translatedCorrespondenceModel.close();
      }
    };
  }

  private class TranslatedCorrespondenceModel implements CorrespondenceModel {
    private final BiMap<String, EClass> viewClasses = HashBiMap.create();

    private final ViewCorrespondences sourceViewCorrespondences;
    private final ViewCorrespondences targetViewCorrespondences;

    private final EditableCorrespondenceModelView<Correspondence> innerCorrespondenceModel;

    private final Map<EObject, List<PendingCorrespondence>> pendingCorrespondences =
        new LinkedHashMap<>();

    public TranslatedCorrespondenceModel(
        EditableCorrespondenceModelView<Correspondence> innerCorrespondenceModel,
        ViewChangePropagationContext context) {
      this.innerCorrespondenceModel = innerCorrespondenceModel;

      this.sourceViewCorrespondences = context.sourceView().getCorrespondences();
      this.targetViewCorrespondences = context.targetView().getCorrespondences();

      addViewClasses(context.sourceViewType().getMetamodel());
      addViewClasses(context.targetViewType().getMetamodel());
    }

    private void addViewClasses(EPackage ePackage) {
      if (ePackage == null) {
        return;
      }

      ePackage.getEClassifiers()
          .stream()
          .filter(EClass.class::isInstance)
          .map(EClass.class::cast)
          .forEach(eClass -> viewClasses.put(EcoreUtil.getURI(eClass).toString(), eClass));
    }

    @Override
    public <C extends Correspondence> C addCorrespondenceBetween(
        List<EObject> firstEObjects, List<EObject> secondEObjects, String tag,
        Supplier<C> correspondenceCreator) {
      ensureSingularViewObject(firstEObjects);
      ensureSingularViewObject(secondEObjects);
      EObject leftViewObject = firstEObjects.getFirst();
      EObject rightViewObject = secondEObjects.getFirst();

      EClass leftViewClass = leftViewObject.eClass();
      EClass rightViewClass = rightViewObject.eClass();
      Tag completeTag = new Tag(leftViewClass, rightViewClass, tag);

      C correspondence = correspondenceCreator.get();
      PendingCorrespondence pendingCorrespondence =
          new PendingCorrespondence(leftViewObject, rightViewObject, tag, correspondence,
                                    () -> innerCorrespondenceModel.getEditableView(
                                            Correspondence.class, () -> correspondence)
                                              .addCorrespondenceBetween(
                                                  translateViewObjectToOriginObjects(
                                                      leftViewObject),
                                                  translateViewObjectToOriginObjects(
                                                      rightViewObject), completeTag.toString()));
      addPendingCorrespondence(pendingCorrespondence);

      return correspondence;
    }

    private static void ensureSingularViewObject(List<EObject> viewObjects) {
      if (viewObjects.size() != 1) {
        throw new UnsupportedOperationException("Only supports translating singular view objects.");
      }
    }

    @Override
    public boolean hasCorrespondences(List<EObject> sourceEObjects) {
      ensureSingularViewObject(sourceEObjects);
      EObject viewObject = sourceEObjects.getFirst();

      if (!getPendingCorrespondences(viewObject).isEmpty()) {
        return true;
      }

      List<EObject> originObjects = translateViewObjectToOriginObjects(viewObject);
      return originObjects != null && innerCorrespondenceModel.hasCorrespondences(originObjects);
    }

    @Override
    public Set<EObject> getAllEObjectsInACorrespondence() {
      HashSet<EObject> result = new HashSet<>();

      pendingCorrespondences.values().forEach(pendingCorrespondences -> {
        for (PendingCorrespondence pendingCorrespondence : pendingCorrespondences) {
          result.add(pendingCorrespondence.leftViewObject);
          result.add(pendingCorrespondence.rightViewObject);
        }
      });

      result.addAll(innerCorrespondenceModel.getAllEObjectsInACorrespondence());

      return result;
    }

    @Override
    public Set<String> getAllTags() {
      HashSet<String> tags = new HashSet<>();

      pendingCorrespondences.values().forEach(pendingCorrespondences -> {
        for (PendingCorrespondence pendingCorrespondence : pendingCorrespondences) {
          tags.add(pendingCorrespondence.tag);
        }
      });
      tags.addAll(innerCorrespondenceModel.getAllTags());

      return tags;
    }

    @Override
    public Map<String, Set<EObject>> getCorrespondingEObjectsWithTag(
        List<EObject> sourceEObjects,
        Class<? extends Correspondence> correspondenceType) {
      ensureSingularViewObject(sourceEObjects);
      EObject viewObject = sourceEObjects.getFirst();

      Map<String, Set<EObject>> result = new HashMap<>();

      filterByTypeAndTag(getPendingCorrespondences(viewObject).stream(),
                         correspondenceType, null)
          .forEach(pendingCorrespondence -> {
            String tag = pendingCorrespondence.tag;
            EObject correspondingViewObject = pendingCorrespondence.getCorresponding(viewObject);
            result.computeIfAbsent(tag, k -> new HashSet<>()).add(correspondingViewObject);
          });

      List<EObject> originObjects = translateViewObjectToOriginObjects(viewObject);
      if (originObjects == null) {
        return result;
      }

      CorrespondenceModelView<? extends Correspondence> typedCorrespondenceModel =
          innerCorrespondenceModel.getView(correspondenceType);

      for (String existingTag : typedCorrespondenceModel.getAllTags()) {
        Optional<Tag> parsedTag = parse(existingTag);
        if (parsedTag.isPresent()) {
          String tag = parsedTag.get().tag;
          EClass otherViewClass = parsedTag.get().getOtherViewClass(viewObject);

          Set<List<EObject>> allCorrespondingOriginObjects =
              typedCorrespondenceModel.getCorrespondingEObjects(originObjects, existingTag);

          for (List<EObject> correspondingOriginObjects : allCorrespondingOriginObjects) {
            EObject correspondingViewObject =
                translateOriginObjectsToViewObject(correspondingOriginObjects, otherViewClass);
            result.computeIfAbsent(tag, k -> new HashSet<>()).add(correspondingViewObject);
          }
        }
      }

      return result;
    }

    @Override
    public Set<List<EObject>> getCorrespondingEObjects(
        Class<? extends Correspondence> correspondenceType, List<EObject> sourceEObjects,
        String tag) {
      ensureSingularViewObject(sourceEObjects);
      EObject viewObject = sourceEObjects.getFirst();

      Set<List<EObject>> result = new HashSet<>();

      filterByTypeAndTag(getPendingCorrespondences(viewObject).stream(),
                         correspondenceType, tag).map(
              pendingCorrespondence -> pendingCorrespondence.getCorresponding(viewObject))
          .map(List::of)
          .forEach(result::add);

      List<EObject> originObjects = translateViewObjectToOriginObjects(viewObject);
      if (originObjects == null) {
        return result;
      }

      CorrespondenceModelView<? extends Correspondence> typedCorrespondenceModel =
          innerCorrespondenceModel.getView(correspondenceType);

      for (String existingTag : typedCorrespondenceModel.getAllTags()) {
        Optional<Tag> parsedTag = parse(existingTag);
        if (parsedTag.isPresent()
                && (tag == null || Objects.equals(parsedTag.get().tag, tag))) {
          EClass otherViewClass = parsedTag.get().getOtherViewClass(viewObject);

          Set<List<EObject>> allCorrespondingOriginObjects =
              typedCorrespondenceModel.getCorrespondingEObjects(originObjects, existingTag);
          for (List<EObject> correspondingOriginObjects : allCorrespondingOriginObjects) {
            EObject correspondingViewObject =
                translateOriginObjectsToViewObject(correspondingOriginObjects, otherViewClass);
            result.add(List.of(correspondingViewObject));
          }
        }
      }

      return result;
    }

    @Override
    public <C extends Correspondence> Set<C> removeCorrespondencesBetween(
        Class<C> correspondenceType, List<EObject> firstEObjects, List<EObject> secondEObjects,
        String tag) {
      ensureSingularViewObject(firstEObjects);
      ensureSingularViewObject(secondEObjects);
      EObject leftViewObject = firstEObjects.getFirst();
      EObject rightViewObject = secondEObjects.getFirst();

      Set<PendingCorrespondence> toRemove = new HashSet<>();
      filterByTypeAndTag(getPendingCorrespondences(leftViewObject).stream(), correspondenceType,
                         tag).filter(
              pendingCorrespondence -> pendingCorrespondence.isOnEitherSide(rightViewObject))
          .forEach(toRemove::add);
      toRemove.forEach(this::removePendingCorrespondence);

      HashSet<C> result = toRemove.stream()
                              .map(pendingCorrespondence -> correspondenceType.cast(
                                  pendingCorrespondence.correspondence))
                              .collect(Collectors.toCollection(HashSet::new));

      List<EObject> leftOriginObjects = translateViewObjectToOriginObjects(leftViewObject);
      List<EObject> rightOriginObjects = translateViewObjectToOriginObjects(rightViewObject);
      if (leftOriginObjects == null || rightOriginObjects == null) {
        return result;
      }

      EditableCorrespondenceModelView<C> typedCorrespondenceModel =
          innerCorrespondenceModel.getEditableView(correspondenceType, () -> null);

      if (tag != null) {
        Tag completeTag = new Tag(leftViewObject.eClass(), rightViewObject.eClass(), tag);
        result.addAll(typedCorrespondenceModel.removeCorrespondencesBetween(leftOriginObjects,
                                                                            rightOriginObjects,
                                                                            completeTag.toString()));
      } else {
        for (String existingTag : Set.copyOf(typedCorrespondenceModel.getAllTags())) {
          Optional<Tag> parsedTag = parse(existingTag);
          if (parsedTag.isPresent() && parsedTag.get()
                                           .hasViewClasses(leftViewObject.eClass(),
                                                           rightViewObject.eClass())) {
            result.addAll(typedCorrespondenceModel.removeCorrespondencesBetween(leftOriginObjects,
                                                                                rightOriginObjects,
                                                                                existingTag));
          }
        }
      }

      return result;
    }

    private Stream<PendingCorrespondence> filterByTypeAndTag(
        Stream<PendingCorrespondence> stream, Class<? extends Correspondence> correspondenceType,
        String tag) {
      return stream.filter(pendingCorrespondence -> correspondenceType.isInstance(
              pendingCorrespondence.correspondence))
                 .filter(
                     pendingCorrespondence -> tag == null || tag.equals(pendingCorrespondence.tag));
    }

    public Optional<Tag> parse(String tag) {
      if (tag == null || !tag.startsWith("origin")) {
        return Optional.empty();
      }

      int leftOpeningBracketIndex = tag.indexOf("<");
      int leftClosingBracketIndex = tag.indexOf(">");

      if (leftOpeningBracketIndex == -1 || leftClosingBracketIndex == -1) {
        return Optional.empty();
      }

      int rightOpeningBracketIndex = tag.indexOf("<", leftClosingBracketIndex);
      int rightClosingBracketIndex = tag.indexOf(">", rightOpeningBracketIndex);

      if (rightOpeningBracketIndex == -1 || rightClosingBracketIndex == -1) {
        return Optional.empty();
      }

      String leftClassString = tag.substring(leftOpeningBracketIndex + 1, leftClosingBracketIndex);
      String rightClassString =
          tag.substring(rightOpeningBracketIndex + 1, rightClosingBracketIndex);

      EClass leftViewClass = viewClasses.get(leftClassString);
      EClass rightViewClass = viewClasses.get(rightClassString);

      int remainingLength = tag.length() - (rightClosingBracketIndex + 1);
      String tagString = null;
      if (remainingLength > 0 && tag.charAt(rightClosingBracketIndex + 1) == ' ') {
        tagString = tag.substring(rightClosingBracketIndex + 2);
      }

      return Optional.of(new Tag(leftViewClass, rightViewClass, tagString));
    }

    private EObject translateOriginObjectsToViewObject(
        List<EObject> originObjects,
        EClass viewClass) {
      if (sourceViewCorrespondences.canProvideViewObjectForOriginObjects(originObjects,
                                                                         viewClass)) {
        return sourceViewCorrespondences.getCorrespondingViewObjectForOriginObjects(originObjects,
                                                                                    viewClass);
      }
      if (targetViewCorrespondences.canProvideViewObjectForOriginObjects(originObjects,
                                                                         viewClass)) {
        return targetViewCorrespondences.getCorrespondingViewObjectForOriginObjects(originObjects,
                                                                                    viewClass);
      }
      throw new IllegalStateException("No translation found for origin objects " + originObjects);
    }

    private void removePendingCorrespondence(PendingCorrespondence pendingCorrespondence) {
      removePendingCorrespondenceFromIndex(pendingCorrespondence,
                                           pendingCorrespondence.leftViewObject);
      removePendingCorrespondenceFromIndex(pendingCorrespondence,
                                           pendingCorrespondence.rightViewObject);
    }

    private void removePendingCorrespondenceFromIndex(
        PendingCorrespondence pendingCorrespondence, EObject viewObject) {
      pendingCorrespondences.get(viewObject).remove(pendingCorrespondence);
      if (pendingCorrespondences.get(viewObject).isEmpty()) {
        pendingCorrespondences.remove(viewObject);
      }
    }

    private List<PendingCorrespondence> getPendingCorrespondences(EObject viewObject) {
      return pendingCorrespondences.getOrDefault(viewObject, List.of());
    }

    private void addPendingCorrespondence(PendingCorrespondence pendingCorrespondence) {
      addPendingCorrespondence(pendingCorrespondence, pendingCorrespondence.leftViewObject);
      addPendingCorrespondence(pendingCorrespondence, pendingCorrespondence.rightViewObject);
    }

    private void addPendingCorrespondence(
        PendingCorrespondence pendingCorrespondence, EObject viewObject) {
      pendingCorrespondences.computeIfAbsent(viewObject, ignored -> new ArrayList<>())
          .add(pendingCorrespondence);
    }

    private List<EObject> translateViewObjectToOriginObjects(EObject viewObject) {
      if (sourceViewCorrespondences.canProvideOriginObjectsForViewObject(viewObject)) {
        return sourceViewCorrespondences.getCorrespondingOriginObjectsForViewObject(viewObject);
      }
      if (targetViewCorrespondences.canProvideOriginObjectsForViewObject(viewObject)) {
        return targetViewCorrespondences.getCorrespondingOriginObjectsForViewObject(viewObject);
      }
      throw new IllegalStateException("No translation found for view objects " + viewObject);
    }

    @Override
    public void close() {
      Set<PendingCorrespondence> allPendingCorrespondences = new LinkedHashSet<>();
      pendingCorrespondences.values().forEach(allPendingCorrespondences::addAll);
      allPendingCorrespondences.forEach(
          pendingCorrespondence -> pendingCorrespondence.runnable.run());

      pendingCorrespondences.clear();
    }

    private record PendingCorrespondence(EObject leftViewObject, EObject rightViewObject,
                                         String tag, Correspondence correspondence,
                                         Runnable runnable) {
      public boolean isOnEitherSide(EObject viewObject) {
        return leftViewObject.equals(viewObject) || rightViewObject.equals(viewObject);
      }

      public EObject getCorresponding(EObject viewObject) {
        return leftViewObject.equals(viewObject) ? rightViewObject : leftViewObject;
      }
    }

    private class Tag {
      private final EClass leftViewClass;
      private final EClass rightViewClass;
      private final String tag;

      public Tag(EClass leftViewClass, EClass rightViewClass, String tag) {
        this.leftViewClass = leftViewClass;
        this.rightViewClass = rightViewClass;
        this.tag = tag;
      }

      public EClass getOtherViewClass(EObject viewObject) {
        EClass viewClass = viewObject.eClass();
        if (Objects.equals(leftViewClass, viewClass) || (leftViewClass == null
                                                             && sourceViewCorrespondences.canProvideOriginObjectsForViewObject(
            viewObject))) {
          return rightViewClass;
        }
        if (Objects.equals(rightViewClass, viewClass) || (rightViewClass == null
                                                              && targetViewCorrespondences.canProvideOriginObjectsForViewObject(
            viewObject))) {
          return leftViewClass;
        }
        return null;
      }

      @Override
      public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("origin<");
        String leftClassString = viewClasses.inverse().get(leftViewClass);
        if (leftClassString != null) {
          result.append(leftClassString);
        }
        result.append("><");
        String rightClassString = viewClasses.inverse().get(rightViewClass);
        if (rightClassString != null) {
          result.append(rightClassString);
        }
        result.append(">");
        if (tag != null) {
          result.append(" ");
          result.append(tag);
        }

        return result.toString();
      }

      public boolean hasViewClasses(EClass otherLeftViewClass, EClass otherRightViewClass) {
        EClass effectiveLeftViewClass =
            viewClasses.inverse().containsKey(otherLeftViewClass) ? otherLeftViewClass : null;
        EClass effectiveRightViewClass =
            viewClasses.inverse().containsKey(otherRightViewClass) ? otherRightViewClass : null;

        if (Objects.equals(leftViewClass, effectiveLeftViewClass)
                && Objects.equals(rightViewClass, effectiveRightViewClass)) {
          return true;
        }
        return Objects.equals(leftViewClass, effectiveRightViewClass)
                   && Objects.equals(rightViewClass, effectiveLeftViewClass);
      }
    }
  }
}
