package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ChangeVariableTypeRefactoring implements Refactoring {

    private VariableDeclaration originalVariable;
    private VariableDeclaration changedTypeVariable;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ChangeVariableTypeRefactoring(VariableDeclaration originalVariable, VariableDeclaration changedTypeVariable,
                                         DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalVariable = originalVariable;
        this.changedTypeVariable = changedTypeVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_VARIABLE_TYPE;
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
        sb.append(VariableUtils.getVariableDeclaration(originalVariable));
        sb.append(" to ");
        sb.append(VariableUtils.getVariableDeclaration(changedTypeVariable));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public VariableDeclaration getOriginalVariable() {
        return originalVariable;
    }

    public VariableDeclaration getChangedTypeVariable() {
        return changedTypeVariable;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
