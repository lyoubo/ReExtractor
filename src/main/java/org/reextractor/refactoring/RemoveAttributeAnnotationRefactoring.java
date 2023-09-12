package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.EntityType;
import org.remapper.dto.LocationInfo;

public class RemoveAttributeAnnotationRefactoring implements Refactoring {

    private Annotation annotation;
    private DeclarationNodeTree attributeBefore;
    private DeclarationNodeTree attributeAfter;

    public RemoveAttributeAnnotationRefactoring(Annotation annotation, DeclarationNodeTree attributeBefore, DeclarationNodeTree attributeAfter) {
        this.annotation = annotation;
        this.attributeBefore = attributeBefore;
        this.attributeAfter = attributeAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_ATTRIBUTE_ANNOTATION;
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
        sb.append(annotation.toString());
        if (attributeBefore.getType() == EntityType.ENUM_CONSTANT) {
            sb.append(" in enum constant ");
        } else {
            sb.append(" in attribute ");
        }
        sb.append(AttributeUtils.getVariableDeclaration(attributeBefore));
        sb.append(" from class ");
        sb.append(attributeBefore.getNamespace());
        return sb.toString();
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public DeclarationNodeTree getAttributeBefore() {
        return attributeBefore;
    }

    public DeclarationNodeTree getAttributeAfter() {
        return attributeAfter;
    }
}