package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ExtractOperationRefactoring implements Refactoring {

    private DeclarationNodeTree extractedOperation;
    private DeclarationNodeTree sourceOperationBeforeExtraction;
    private DeclarationNodeTree sourceOperationAfterExtraction;

    public ExtractOperationRefactoring(DeclarationNodeTree sourceOperationBeforeExtraction, DeclarationNodeTree sourceOperationAfterExtraction,
                                       DeclarationNodeTree extractedOperation) {
        this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
        this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
        this.extractedOperation = extractedOperation;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_OPERATION;
    }

    public LocationInfo leftSide() {
        return sourceOperationBeforeExtraction.getLocation();
    }

    public LocationInfo rightSide() {
        return extractedOperation.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(MethodUtils.getMethodDeclaration(extractedOperation));
        sb.append(" extracted from ");
        sb.append(MethodUtils.getMethodDeclaration(sourceOperationBeforeExtraction));
        sb.append(" in class ");
        sb.append(getClassName());
        return sb.toString();
    }

    private String getClassName() {
        String sourceClassName = sourceOperationBeforeExtraction.getNamespace();
        String targetClassName = sourceOperationAfterExtraction.getNamespace();
        return sourceClassName.equals(targetClassName) ? sourceClassName : targetClassName;
    }

    public DeclarationNodeTree getExtractedOperation() {
        return extractedOperation;
    }

    public DeclarationNodeTree getSourceOperationBeforeExtraction() {
        return sourceOperationBeforeExtraction;
    }

    public DeclarationNodeTree getSourceOperationAfterExtraction() {
        return sourceOperationAfterExtraction;
    }
}
