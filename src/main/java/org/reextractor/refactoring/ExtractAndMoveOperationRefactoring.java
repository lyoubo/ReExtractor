package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ExtractAndMoveOperationRefactoring implements Refactoring {

    private DeclarationNodeTree extractedOperation;
    private DeclarationNodeTree sourceOperationBeforeExtraction;
    private DeclarationNodeTree sourceOperationAfterExtraction;

    public ExtractAndMoveOperationRefactoring(DeclarationNodeTree sourceOperationBeforeExtraction, DeclarationNodeTree sourceOperationAfterExtraction,
                                              DeclarationNodeTree extractedOperation) {
        this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
        this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
        this.extractedOperation = extractedOperation;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_AND_MOVE_OPERATION;
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
        sb.append(sourceOperationBeforeExtraction.getNamespace());
        sb.append(" & moved to class ");
        sb.append(extractedOperation.getNamespace());
        return sb.toString();
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
