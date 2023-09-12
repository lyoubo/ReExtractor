package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class LoopInterchangeRefactoring implements Refactoring {

    private String originalLoop;
    private String interchangedLoop;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public LoopInterchangeRefactoring(String originalLoop, String interchangedLoop,
                                      DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalLoop = originalLoop;
        this.interchangedLoop = interchangedLoop;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.LOOP_INTERCHANGE;
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
        sb.append(originalLoop);
        sb.append(" to ");
        sb.append(interchangedLoop);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getOriginalLoop() {
        return originalLoop;
    }

    public String getInterchangedLoop() {
        return interchangedLoop;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
