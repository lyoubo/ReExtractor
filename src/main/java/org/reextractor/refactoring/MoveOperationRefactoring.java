package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class MoveOperationRefactoring implements Refactoring {

    private DeclarationNodeTree originalOperation;
    private DeclarationNodeTree movedOperation;

    public MoveOperationRefactoring(DeclarationNodeTree originalOperation, DeclarationNodeTree movedOperation) {
        this.originalOperation = originalOperation;
        this.movedOperation = movedOperation;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_OPERATION;
    }

    public LocationInfo leftSide() {
        return originalOperation.getLocation();
    }

    public LocationInfo rightSide() {
        return movedOperation.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(MethodUtils.getMethodDeclaration(originalOperation));
        sb.append(" from class ");
        sb.append(getSourceClassName());
        sb.append(" to ");
        sb.append(MethodUtils.getMethodDeclaration(movedOperation));
        sb.append(" from class ");
        sb.append(getTargetClassName());
        return sb.toString();
    }

    public String getSourceClassName() {
        return originalOperation.getNamespace();
    }

    public String getTargetClassName() {
        return movedOperation.getNamespace();
    }

    public DeclarationNodeTree getOriginalOperation() {
        return originalOperation;
    }

    public DeclarationNodeTree getMovedOperation() {
        return movedOperation;
    }
}
