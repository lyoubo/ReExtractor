package org.reextractor.refactoring;

import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ChangeAttributeTypeRefactoring implements Refactoring {

    private DeclarationNodeTree originalAttribute;
    private DeclarationNodeTree changedTypeAttribute;

    public ChangeAttributeTypeRefactoring(DeclarationNodeTree originalAttribute, DeclarationNodeTree changedTypeAttribute) {
        this.originalAttribute = originalAttribute;
        this.changedTypeAttribute = changedTypeAttribute;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_ATTRIBUTE_TYPE;
    }

    public LocationInfo leftSide() {
        return originalAttribute.getLocation();
    }

    public LocationInfo rightSide() {
        return changedTypeAttribute.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(AttributeUtils.getVariableDeclaration(originalAttribute));
        sb.append(" to ");
        sb.append(AttributeUtils.getVariableDeclaration(changedTypeAttribute));
        sb.append(" in class ").append(changedTypeAttribute.getNamespace());
        return sb.toString();
    }

    public DeclarationNodeTree getOriginalAttribute() {
        return originalAttribute;
    }

    public DeclarationNodeTree getChangedTypeAttribute() {
        return changedTypeAttribute;
    }
}
