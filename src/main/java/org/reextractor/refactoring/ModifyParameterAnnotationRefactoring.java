package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ModifyParameterAnnotationRefactoring implements Refactoring {

    private Annotation annotationBefore;
    private Annotation annotationAfter;
    private SingleVariableDeclaration variableBefore;
    private SingleVariableDeclaration variableAfter;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ModifyParameterAnnotationRefactoring(Annotation annotationBefore, Annotation annotationAfter, SingleVariableDeclaration variableBefore, SingleVariableDeclaration variableAfter,
                                                DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.annotationBefore = annotationBefore;
        this.annotationAfter = annotationAfter;
        this.variableBefore = variableBefore;
        this.variableAfter = variableAfter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MODIFY_PARAMETER_ANNOTATION;
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
        sb.append(annotationBefore.toString());
        sb.append(" to ");
        sb.append(annotationAfter.toString());
        sb.append(" in parameter ");
        sb.append(VariableUtils.getVariableDeclaration(variableAfter));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public Annotation getAnnotationBefore() {
        return annotationBefore;
    }

    public Annotation getAnnotationAfter() {
        return annotationAfter;
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
