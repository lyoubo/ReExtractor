package org.reextractor.refactoring;

import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class RenameAttributeRefactoring implements Refactoring {

    private DeclarationNodeTree originalAttribute;
    private DeclarationNodeTree renamedAttribute;

    public RenameAttributeRefactoring(DeclarationNodeTree originalAttribute, DeclarationNodeTree renamedAttribute) {
        this.originalAttribute = originalAttribute;
        this.renamedAttribute = renamedAttribute;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_ATTRIBUTE;
    }

    public LocationInfo leftSide() {
        return originalAttribute.getLocation();
    }

    public LocationInfo rightSide() {
        return renamedAttribute.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(AttributeUtils.getVariableDeclaration(originalAttribute));
        sb.append(" to ");
        sb.append(AttributeUtils.getVariableDeclaration(renamedAttribute));
        sb.append(" in class ").append(renamedAttribute.getNamespace());
        return sb.toString();
    }

    public DeclarationNodeTree getOriginalAttribute() {
        return originalAttribute;
    }

    public DeclarationNodeTree getRenamedAttribute() {
        return renamedAttribute;
    }
}
