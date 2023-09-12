package org.reextractor.refactoring;

import org.reextractor.dto.Visibility;
import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ChangeAttributeAccessModifierRefactoring implements Refactoring {

    private Visibility originalAccessModifier;
    private Visibility changedAccessModifier;
    private DeclarationNodeTree attributeBefore;
    private DeclarationNodeTree attributeAfter;

    public ChangeAttributeAccessModifierRefactoring(Visibility originalAccessModifier, Visibility changedAccessModifier,
                                                    DeclarationNodeTree attributeBefore, DeclarationNodeTree attributeAfter) {
        this.originalAccessModifier = originalAccessModifier;
        this.changedAccessModifier = changedAccessModifier;
        this.attributeBefore = attributeBefore;
        this.attributeAfter = attributeAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_ATTRIBUTE_ACCESS_MODIFIER;
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
        sb.append(originalAccessModifier);
        sb.append(" to ");
        sb.append(changedAccessModifier);
        sb.append(" in attribute ");
        sb.append(AttributeUtils.getVariableDeclaration(attributeAfter));
        sb.append(" from class ");
        sb.append(attributeAfter.getNamespace());
        return sb.toString();
    }

    public Visibility getOriginalAccessModifier() {
        return originalAccessModifier;
    }

    public Visibility getChangedAccessModifier() {
        return changedAccessModifier;
    }

    public DeclarationNodeTree getAttributeBefore() {
        return attributeBefore;
    }

    public DeclarationNodeTree getAttributeAfter() {
        return attributeAfter;
    }
}
