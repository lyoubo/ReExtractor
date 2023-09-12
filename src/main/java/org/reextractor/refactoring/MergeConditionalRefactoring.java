package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

import java.util.Set;

public class MergeConditionalRefactoring implements Refactoring {

    private Set<String> mergedConditionals;
    private String newConditional;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public MergeConditionalRefactoring(Set<String> mergedConditionals, String newConditional,
                                       DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.mergedConditionals = mergedConditionals;
        this.newConditional = newConditional;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MERGE_CONDITIONAL;
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
        sb.append("[");
        int len = 0;
        for (String mergedConditional : mergedConditionals) {
            sb.append(mergedConditional);
            if (len < mergedConditionals.size() - 1)
                sb.append(", ");
            len++;
        }
        sb.append("]");
        sb.append(" to ");
        sb.append(newConditional);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public Set<String> getMergedConditionals() {
        return mergedConditionals;
    }

    public String getNewConditional() {
        return newConditional;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
