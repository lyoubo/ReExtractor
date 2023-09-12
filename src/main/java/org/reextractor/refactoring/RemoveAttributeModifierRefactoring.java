package org.reextractor.refactoring;

import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.EntityType;
import org.remapper.dto.LocationInfo;

public class RemoveAttributeModifierRefactoring implements Refactoring {

    private String modifier;
    private DeclarationNodeTree attributeBefore;
    private DeclarationNodeTree attributeAfter;

    public RemoveAttributeModifierRefactoring(String modifier, DeclarationNodeTree attributeBefore, DeclarationNodeTree attributeAfter) {
        this.modifier = modifier;
        this.attributeBefore = attributeBefore;
        this.attributeAfter = attributeAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_ATTRIBUTE_MODIFIER;
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
        sb.append(modifier);
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

    public String getModifier() {
        return modifier;
    }

    public DeclarationNodeTree getAttributeBefore() {
        return attributeBefore;
    }

    public DeclarationNodeTree getAttributeAfter() {
        return attributeAfter;
    }
}
