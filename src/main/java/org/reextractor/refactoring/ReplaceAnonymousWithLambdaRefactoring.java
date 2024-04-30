package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.*;
import org.reextractor.util.MethodUtils;
import org.remapper.dto.CodeRange;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.EntityType;
import org.remapper.dto.LocationInfo;

import java.util.ArrayList;
import java.util.List;

public class ReplaceAnonymousWithLambdaRefactoring implements Refactoring {

    private AnonymousClassDeclaration anonymous;
    private Expression lambda;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ReplaceAnonymousWithLambdaRefactoring(AnonymousClassDeclaration anonymous, LambdaExpression lambda, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.anonymous = anonymous;
        this.lambda = lambda;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public ReplaceAnonymousWithLambdaRefactoring(AnonymousClassDeclaration anonymous, MethodReference lambda, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.anonymous = anonymous;
        this.lambda = lambda;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_ANONYMOUS_WITH_LAMBDA;
    }

    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        LocationInfo anonymousLocation = new LocationInfo(
                (CompilationUnit) anonymous.getRoot(), operationBefore.getFilePath(), anonymous);
        ranges.add(anonymousLocation.codeRange()
                .setDescription("anonymous class declaration")
                .setCodeElement(MethodUtils.getAnonymousCodePath(anonymous)));
        ranges.add(operationBefore.codeRange()
                .setDescription("original method declaration")
                .setCodeElement(MethodUtils.method2String(operationBefore)));
        return ranges;
    }

    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        LocationInfo lambdaLocation = new LocationInfo(
                (CompilationUnit) lambda.getRoot(), operationAfter.getFilePath(), lambda);
        ranges.add(lambdaLocation.codeRange()
                .setDescription("lambda expression")
                .setCodeElement(MethodUtils.getLambdaString(lambda)));
        ranges.add(operationAfter.codeRange()
                .setDescription("method declaration with introduced lambda")
                .setCodeElement(MethodUtils.method2String(operationAfter)));
        return ranges;
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
        if (operationAfter.getType() == EntityType.INITIALIZER) {
            sb.append(" in initializer " + operationAfter.getParent().getName());
        } else {
            sb.append(" in method ");
            sb.append(MethodUtils.method2String(operationAfter));
        }
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public AnonymousClassDeclaration getAnonymous() {
        return anonymous;
    }

    public Expression getLambda() {
        return lambda;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
