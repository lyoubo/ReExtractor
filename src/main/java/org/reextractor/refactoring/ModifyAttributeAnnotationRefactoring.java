package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.EntityType;
import org.remapper.dto.LocationInfo;

public class ModifyAttributeAnnotationRefactoring implements Refactoring {

    private Annotation annotationBefore;
    private Annotation annotationAfter;
    private DeclarationNodeTree attributeBefore;
    private DeclarationNodeTree attributeAfter;

    public ModifyAttributeAnnotationRefactoring(Annotation annotationBefore, Annotation annotationAfter, DeclarationNodeTree attributeBefore, DeclarationNodeTree attributeAfter) {
        this.annotationBefore = annotationBefore;
        this.annotationAfter = annotationAfter;
        this.attributeBefore = attributeBefore;
        this.attributeAfter = attributeAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MODIFY_ATTRIBUTE_ANNOTATION;
    }

    public LocationInfo leftSide() {
        return attributeBefore.getLocation();
    }

    public LocationInfo rightSide() {
        return attributeAfter.getLocation();
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
        if (attributeAfter.getType() == EntityType.ENUM_CONSTANT) {
            sb.append(" in enum constant ");
        } else {
            sb.append(" in attribute ");
        }
        sb.append(AttributeUtils.getVariableDeclarationWithVisibility(attributeAfter));
        sb.append(" from class ");
        sb.append(attributeAfter.getNamespace());
        return sb.toString();
    }

    public Annotation getAnnotationBefore() {
        return annotationBefore;
    }

    public Annotation getAnnotationAfter() {
        return annotationAfter;
    }

    public DeclarationNodeTree getAttributeBefore() {
        return attributeBefore;
    }

    public DeclarationNodeTree getAttributeAfter() {
        return attributeAfter;
    }
}
