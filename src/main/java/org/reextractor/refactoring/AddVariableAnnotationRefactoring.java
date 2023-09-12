package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class AddVariableAnnotationRefactoring implements Refactoring {

    private Annotation annotation;
    private VariableDeclaration variableBefore;
    private VariableDeclaration variableAfter;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public AddVariableAnnotationRefactoring(Annotation annotation, VariableDeclaration variableBefore, VariableDeclaration variableAfter,
                                            DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.annotation = annotation;
        this.variableBefore = variableBefore;
        this.variableAfter = variableAfter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.ADD_VARIABLE_ANNOTATION;
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
        sb.append(" in variable ");
        sb.append(VariableUtils.getVariableDeclaration(variableAfter));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public VariableDeclaration getVariableBefore() {
        return variableBefore;
    }

    public VariableDeclaration getVariableAfter() {
        return variableAfter;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
