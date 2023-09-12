package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class InlineOperationRefactoring implements Refactoring {

    private DeclarationNodeTree inlinedOperation;
    private DeclarationNodeTree targetOperationBeforeInline;
    private DeclarationNodeTree targetOperationAfterInline;

    public InlineOperationRefactoring(DeclarationNodeTree targetOperationBeforeInline, DeclarationNodeTree targetOperationAfterInline,
                                      DeclarationNodeTree inlinedOperation) {
        this.targetOperationBeforeInline = targetOperationBeforeInline;
        this.targetOperationAfterInline = targetOperationAfterInline;
        this.inlinedOperation = inlinedOperation;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.INLINE_OPERATION;
    }

    public LocationInfo leftSide() {
        return inlinedOperation.getLocation();
    }

    public LocationInfo rightSide() {
        return targetOperationAfterInline.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(MethodUtils.getMethodDeclaration(inlinedOperation));
        sb.append(" inlined to ");
        sb.append(MethodUtils.getMethodDeclaration(targetOperationAfterInline));
        sb.append(" in class ");
        sb.append(targetOperationAfterInline.getNamespace());
        return sb.toString();
    }

    public DeclarationNodeTree getInlinedOperation() {
        return inlinedOperation;
    }

    public DeclarationNodeTree getTargetOperationBeforeInline() {
        return targetOperationBeforeInline;
    }

    public DeclarationNodeTree getTargetOperationAfterInline() {
        return targetOperationAfterInline;
    }
}
