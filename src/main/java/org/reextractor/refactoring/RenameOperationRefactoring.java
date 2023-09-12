package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.reextractor.util.StringUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class RenameOperationRefactoring implements Refactoring {

    private DeclarationNodeTree originalOperation;
    private DeclarationNodeTree renamedOperation;

    public RenameOperationRefactoring(DeclarationNodeTree originalOperation, DeclarationNodeTree renamedOperation) {
        this.originalOperation = originalOperation;
        this.renamedOperation = renamedOperation;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_METHOD;
    }

    public LocationInfo leftSide() {
        return originalOperation.getLocation();
    }

    public LocationInfo rightSide() {
        return renamedOperation.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(MethodUtils.getMethodDeclaration(originalOperation));
        sb.append(" renamed to ");
        sb.append(MethodUtils.getMethodDeclaration(renamedOperation));
        sb.append(" in class ").append(getClassName());
        return sb.toString();
    }

    private String getClassName() {
        String sourceClassName = originalOperation.getNamespace();
        String targetClassName = renamedOperation.getNamespace();
        boolean targetIsAnonymousInsideSource = false;
        if (targetClassName.startsWith(sourceClassName + ".")) {
            String targetClassNameSuffix = targetClassName.substring(sourceClassName.length() + 1, targetClassName.length());
            targetIsAnonymousInsideSource = StringUtils.isNumeric(targetClassNameSuffix);
        }
        return sourceClassName.equals(targetClassName) || targetIsAnonymousInsideSource ? sourceClassName : targetClassName;
    }

    public DeclarationNodeTree getOriginalOperation() {
        return originalOperation;
    }

    public DeclarationNodeTree getRenamedOperation() {
        return renamedOperation;
    }
}
