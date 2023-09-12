package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ReplaceAnonymousWithLambdaRefactoring implements Refactoring {

    private AnonymousClassDeclaration anonymous;
    private LambdaExpression lambda;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ReplaceAnonymousWithLambdaRefactoring(AnonymousClassDeclaration anonymous, LambdaExpression lambda, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.anonymous = anonymous;
        this.lambda = lambda;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_ANONYMOUS_WITH_LAMBDA;
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
        sb.append(MethodUtils.getAnonymousCodePath(anonymous));
        sb.append(" with ");
        sb.append(MethodUtils.getLambdaString(lambda));
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public AnonymousClassDeclaration getAnonymous() {
        return anonymous;
    }

    public LambdaExpression getLambda() {
        return lambda;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
