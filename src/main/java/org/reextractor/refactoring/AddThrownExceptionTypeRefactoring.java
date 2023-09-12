package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.SimpleType;
import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class AddThrownExceptionTypeRefactoring implements Refactoring {

    private SimpleType exceptionType;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public AddThrownExceptionTypeRefactoring(SimpleType exceptionType,
                                             DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.exceptionType = exceptionType;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.ADD_THROWN_EXCEPTION_TYPE;
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
        sb.append(exceptionType.getName().getFullyQualifiedName());
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public SimpleType getExceptionType() {
        return exceptionType;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
