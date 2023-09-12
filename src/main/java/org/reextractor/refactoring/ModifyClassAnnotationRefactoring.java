package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;
import org.remapper.util.StringUtils;

public class ModifyClassAnnotationRefactoring implements Refactoring {

    private Annotation annotationBefore;
    private Annotation annotationAfter;
    private DeclarationNodeTree classBefore;
    private DeclarationNodeTree classAfter;

    public ModifyClassAnnotationRefactoring(Annotation annotationBefore, Annotation annotationAfter, DeclarationNodeTree classBefore, DeclarationNodeTree classAfter) {
        this.annotationBefore = annotationBefore;
        this.annotationAfter = annotationAfter;
        this.classBefore = classBefore;
        this.classAfter = classAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MODIFY_CLASS_ANNOTATION;
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
        sb.append(annotationBefore.toString());
        sb.append(" to ");
        sb.append(annotationAfter.toString());
        sb.append(" in class ");
        if (StringUtils.isNotEmpty(classAfter.getNamespace()))
            sb.append(classAfter.getNamespace()).append(".").append(classAfter.getName());
        else
            sb.append(classAfter.getName());
        return sb.toString();
    }

    public Annotation getAnnotationBefore() {
        return annotationBefore;
    }

    public Annotation getAnnotationAfter() {
        return annotationAfter;
    }

    public DeclarationNodeTree getClassBefore() {
        return classBefore;
    }

    public DeclarationNodeTree getClassAfter() {
        return classAfter;
    }
}
