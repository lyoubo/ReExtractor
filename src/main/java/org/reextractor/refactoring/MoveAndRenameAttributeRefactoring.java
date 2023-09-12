package org.reextractor.refactoring;

import org.reextractor.util.AttributeUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class MoveAndRenameAttributeRefactoring implements Refactoring {

    private DeclarationNodeTree originalAttribute;
    private DeclarationNodeTree movedAttribute;

    public MoveAndRenameAttributeRefactoring(DeclarationNodeTree originalAttribute, DeclarationNodeTree movedAttribute) {
        this.originalAttribute = originalAttribute;
        this.movedAttribute = movedAttribute;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_RENAME_ATTRIBUTE;
    }

    public LocationInfo leftSide() {
        return originalAttribute.getLocation();
    }

    public LocationInfo rightSide() {
        return movedAttribute.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(AttributeUtils.getVariableDeclaration(originalAttribute));
        sb.append(" renamed to ");
        sb.append(AttributeUtils.getVariableDeclaration(movedAttribute));
        sb.append(" and moved from class ");
        sb.append(getSourceClassName());
        sb.append(" to class ");
        sb.append(getTargetClassName());
        return sb.toString();
    }

    public String getSourceClassName() {
        return originalAttribute.getNamespace();
    }

    public String getTargetClassName() {
        return movedAttribute.getNamespace();
    }

    public DeclarationNodeTree getOriginalAttribute() {
        return originalAttribute;
    }

    public DeclarationNodeTree getMovedAttribute() {
        return movedAttribute;
    }
}
