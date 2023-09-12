package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class InvertConditionRefactoring implements Refactoring {

    private String originalConditional;
    private String invertedConditional;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public InvertConditionRefactoring(String originalConditional, String invertedConditional,
                                      DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalConditional = originalConditional;
        this.invertedConditional = invertedConditional;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.INVERT_CONDITION;
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
        sb.append(originalConditional);
        sb.append(" to ");
        sb.append(invertedConditional);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getOriginalConditional() {
        return originalConditional;
    }

    public String getInvertedConditional() {
        return invertedConditional;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
