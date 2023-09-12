package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class RenameParameterRefactoring implements Refactoring {

    private VariableDeclaration originalVariable;
    private VariableDeclaration renamedVariable;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public RenameParameterRefactoring(VariableDeclaration originalVariable, VariableDeclaration renamedVariable,
                                      DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalVariable = originalVariable;
        this.renamedVariable = renamedVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_PARAMETER;
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
        sb.append(VariableUtils.getVariableDeclaration(renamedVariable));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public VariableDeclaration getOriginalVariable() {
        return originalVariable;
    }

    public VariableDeclaration getRenamedVariable() {
        return renamedVariable;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
