package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class RemoveParameterRefactoring implements Refactoring {

    private SingleVariableDeclaration parameter;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public RemoveParameterRefactoring(SingleVariableDeclaration parameter, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.parameter = parameter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_PARAMETER;
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
        sb.append(VariableUtils.getVariableDeclaration(parameter));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationBefore));
        sb.append(" from class ");
        sb.append(operationBefore.getNamespace());
        return sb.toString();
    }

    public SingleVariableDeclaration getParameter() {
        return parameter;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
