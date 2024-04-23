package org.reextractor.dto;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IExtendedModifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AnnotationListDiff {

    Set<Annotation> removedAnnotations;
    Set<Annotation> addedAnnotations;
    Set<Pair<Annotation, Annotation>> annotationDiffs;
    Set<Pair<Annotation, Annotation>> commonAnnotations;


    public AnnotationListDiff(List<IExtendedModifier> modifiers1, List<IExtendedModifier> modifiers2) {
        removedAnnotations = new LinkedHashSet<>();
        addedAnnotations = new LinkedHashSet<>();
        annotationDiffs = new LinkedHashSet<>();
        commonAnnotations = new LinkedHashSet<>();
        List<Annotation> annotations1 = new ArrayList<>();
        List<Annotation> annotations2 = new ArrayList<>();
        for (IExtendedModifier modifier : modifiers1) {
            if (modifier.isAnnotation())
                annotations1.add((Annotation) modifier);
        }
        for (IExtendedModifier modifier : modifiers2) {
            if (modifier.isAnnotation())
                annotations2.add((Annotation) modifier);
        }

        Set<Pair<Annotation, Annotation>> matchedAnnotations = new LinkedHashSet<>();
        for (Annotation annotation1 : annotations1) {
            boolean found = false;
            for (Annotation annotation2 : annotations2) {
                if (annotation1.toString().equals(annotation2.toString())) {
                    matchedAnnotations.add(Pair.of(annotation1, annotation2));
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (Annotation annotation2 : annotations2) {
                    if (annotation1.getTypeName().getFullyQualifiedName().equals(annotation2.getTypeName().getFullyQualifiedName())) {
                        matchedAnnotations.add(Pair.of(annotation1, annotation2));
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                removedAnnotations.add(annotation1);
            }
        }
        for (Annotation annotation2 : annotations2) {
            boolean found = false;
            for (Annotation annotation1 : annotations1) {
                if (annotation1.toString().equals(annotation2.toString())) {
                    matchedAnnotations.add(Pair.of(annotation1, annotation2));
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (Annotation annotation1 : annotations1) {
                    if (annotation1.getTypeName().getFullyQualifiedName().equals(annotation2.getTypeName().getFullyQualifiedName())) {
                        matchedAnnotations.add(Pair.of(annotation1, annotation2));
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                addedAnnotations.add(annotation2);
            }
        }
        for (Pair<Annotation, Annotation> pair : matchedAnnotations) {
            Annotation left = pair.getLeft();
            Annotation right = pair.getRight();
            if (!left.toString().equals(right.toString()) && !annotationDiffs.contains(pair)) {
                annotationDiffs.add(pair);
            } else if (!commonAnnotations.contains(pair)) {
                commonAnnotations.add(pair);
            }
        }
    }

    public Set<Annotation> getRemovedAnnotations() {
        return removedAnnotations;
    }

    public Set<Annotation> getAddedAnnotations() {
        return addedAnnotations;
    }

    public Set<Pair<Annotation, Annotation>> getAnnotationDiffs() {
        return annotationDiffs;
    }
}
