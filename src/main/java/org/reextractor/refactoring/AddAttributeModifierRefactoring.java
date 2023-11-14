package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.EntityType;
import org.remapper.dto.LocationInfo;

public class AddAttributeModifierRefactoring implements Refactoring {

    private String modifier;
    private DeclarationNodeTree attributeBefore;
    private DeclarationNodeTree attributeAfter;

    public AddAttributeModifierRefactoring(String modifier, DeclarationNodeTree attributeBefore, DeclarationNodeTree attributeAfter) {
        this.modifier = modifier;
        this.attributeBefore = attributeBefore;
        this.attributeAfter = attributeAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.ADD_ATTRIBUTE_MODIFIER;
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
