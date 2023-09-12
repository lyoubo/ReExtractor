package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class RemoveMethodModifierRefactoring implements Refactoring {

    private String modifier;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public RemoveMethodModifierRefactoring(String modifier, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.modifier = modifier;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_METHOD_MODIFIER;
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
        sb.append(modifier);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationBefore));
        sb.append(" from class ");
        sb.append(operationBefore.getNamespace());
        return sb.toString();
    }

    public String getModifier() {
        return modifier;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
