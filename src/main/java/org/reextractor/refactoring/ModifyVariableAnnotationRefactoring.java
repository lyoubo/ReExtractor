package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ModifyVariableAnnotationRefactoring implements Refactoring {

    private Annotation annotationBefore;
    private Annotation annotationAfter;
    private VariableDeclaration variableBefore;
    private VariableDeclaration variableAfter;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ModifyVariableAnnotationRefactoring(Annotation annotationBefore, Annotation annotationAfter, VariableDeclaration variableBefore,
                                               VariableDeclaration variableAfter, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.annotationBefore = annotationBefore;
        this.annotationAfter = annotationAfter;
        this.variableBefore = variableBefore;
        this.variableAfter = variableAfter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MODIFY_VARIABLE_ANNOTATION;
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
        sb.append(" in variable ");
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
