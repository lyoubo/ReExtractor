package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

import java.util.Set;

public class SplitConditionalRefactoring implements Refactoring {

    private String originalConditional;
    private Set<String> splitConditionals;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public SplitConditionalRefactoring(String originalConditional, Set<String> splitConditionals,
                                       DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalConditional = originalConditional;
        this.splitConditionals = splitConditionals;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.SPLIT_CONDITIONAL;
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
        sb.append("[");
        int len = 0;
        for (String splitConditional : splitConditionals) {
            sb.append(splitConditional);
            if (len < splitConditionals.size() - 1)
                sb.append(", ");
            len++;
        }
        sb.append("]");
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getOriginalConditional() {
        return originalConditional;
    }

    public Set<String> getSplitConditionals() {
        return splitConditionals;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
