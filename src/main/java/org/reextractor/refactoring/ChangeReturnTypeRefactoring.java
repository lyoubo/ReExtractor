package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ChangeReturnTypeRefactoring implements Refactoring {

    private String originalType;
    private String changedType;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ChangeReturnTypeRefactoring(String originalType, String changedType,
                                       DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalType = originalType;
        this.changedType = changedType;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_RETURN_TYPE;
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
        sb.append(originalType);
        sb.append(" to ");
        sb.append(changedType);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getOriginalType() {
        return originalType;
    }

    public String getChangedType() {
        return changedType;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
