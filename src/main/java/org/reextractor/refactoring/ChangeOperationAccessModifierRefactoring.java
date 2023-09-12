package org.reextractor.refactoring;

import org.reextractor.dto.Visibility;
import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ChangeOperationAccessModifierRefactoring implements Refactoring {

    private Visibility originalAccessModifier;
    private Visibility changedAccessModifier;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ChangeOperationAccessModifierRefactoring(Visibility originalAccessModifier, Visibility changedAccessModifier,
                                                    DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalAccessModifier = originalAccessModifier;
        this.changedAccessModifier = changedAccessModifier;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_OPERATION_ACCESS_MODIFIER;
    }

    public LocationInfo leftSide() {
        return operationBefore.getLocation();
    }

    public LocationInfo rightSide() {
        return operationAfter.getLocation();
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
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public Visibility getOriginalAccessModifier() {
        return originalAccessModifier;
    }

    public Visibility getChangedAccessModifier() {
        return changedAccessModifier;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
