package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;
import org.remapper.util.StringUtils;

public class RemoveClassAnnotationRefactoring implements Refactoring {

    private Annotation annotation;
    private DeclarationNodeTree classBefore;
    private DeclarationNodeTree classAfter;

    public RemoveClassAnnotationRefactoring(Annotation annotation, DeclarationNodeTree classBefore, DeclarationNodeTree classAfter) {
        this.annotation = annotation;
        this.classBefore = classBefore;
        this.classAfter = classAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_CLASS_ANNOTATION;
    }

    public LocationInfo leftSide() {
        return classBefore.getLocation();
    }

    public LocationInfo rightSide() {
        return classAfter.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(annotation.toString());
        sb.append(" in class ");
        if (StringUtils.isNotEmpty(classBefore.getNamespace()))
            sb.append(classBefore.getNamespace()).append(".").append(classBefore.getName());
        else
            sb.append(classBefore.getName());
        return sb.toString();
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public DeclarationNodeTree getClassBefore() {
        return classBefore;
    }

    public DeclarationNodeTree getClassAfter() {
        return classAfter;
    }
}
