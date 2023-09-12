package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ChangeLoopTypeRefactoring implements Refactoring {

    private String originalLoopType;
    private String changedLoopType;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ChangeLoopTypeRefactoring(String originalLoopType, String changedLoopType,
                                     DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalLoopType = originalLoopType;
        this.changedLoopType = changedLoopType;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_LOOP_TYPE;
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
        sb.append(originalLoopType);
        sb.append(" to ");
        sb.append(changedLoopType);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getOriginalLoopType() {
        return originalLoopType;
    }

    public String getChangedLoopType() {
        return changedLoopType;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
