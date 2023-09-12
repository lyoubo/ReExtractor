package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ReplaceIfWithTernaryRefactoring implements Refactoring {

    private String ifConditional;
    private String ternaryOperator;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ReplaceIfWithTernaryRefactoring(String ifConditional, String ternaryOperator, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.ifConditional = ifConditional;
        this.ternaryOperator = ternaryOperator;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_IF_WITH_TERNARY;
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
        sb.append(ifConditional);
        sb.append(" with ");
        sb.append(ternaryOperator.lastIndexOf(";\n") != -1 ? ternaryOperator.substring(0, ternaryOperator.lastIndexOf(";\n")) : ternaryOperator);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getIfConditional() {
        return ifConditional;
    }

    public String getTernaryOperator() {
        return ternaryOperator;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
