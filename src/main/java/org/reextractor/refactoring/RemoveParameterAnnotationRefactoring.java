package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class RemoveParameterAnnotationRefactoring implements Refactoring {

    private Annotation annotation;
    private SingleVariableDeclaration variableBefore;
    private SingleVariableDeclaration variableAfter;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public RemoveParameterAnnotationRefactoring(Annotation annotation, SingleVariableDeclaration variableBefore, SingleVariableDeclaration variableAfter,
                                                DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.annotation = annotation;
        this.variableBefore = variableBefore;
        this.variableAfter = variableAfter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REMOVE_PARAMETER_ANNOTATION;
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
        sb.append(annotation.toString());
        sb.append(" in parameter ");
        sb.append(VariableUtils.getVariableDeclaration(variableBefore));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationBefore));
        sb.append(" from class ");
        sb.append(operationBefore.getNamespace());
        return sb.toString();
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public SingleVariableDeclaration getVariableBefore() {
        return variableBefore;
    }

    public SingleVariableDeclaration getVariableAfter() {
        return variableAfter;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
