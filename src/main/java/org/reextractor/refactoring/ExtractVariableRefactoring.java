package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ExtractVariableRefactoring implements Refactoring {

    private VariableDeclaration variableDeclaration;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ExtractVariableRefactoring(VariableDeclaration variableDeclaration, DeclarationNodeTree operationBefore,
                                      DeclarationNodeTree operationAfter) {
        this.variableDeclaration = variableDeclaration;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_VARIABLE;
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
        sb.append(VariableUtils.getVariableDeclaration(variableDeclaration));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public VariableDeclaration getVariableDeclaration() {
        return variableDeclaration;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
