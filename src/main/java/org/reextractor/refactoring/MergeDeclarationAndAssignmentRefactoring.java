package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class MergeDeclarationAndAssignmentRefactoring implements Refactoring {

    private String originalDeclaration;
    private String originalAssignment;
    private String mergedDeclaration;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public MergeDeclarationAndAssignmentRefactoring(String originalDeclaration, String originalAssignment, String mergedDeclaration,
                                                    DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalDeclaration = originalDeclaration;
        this.originalAssignment = originalAssignment;
        this.mergedDeclaration = mergedDeclaration;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MERGE_DECLARATION_AND_ASSIGNMENT;
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
        sb.append("[");
        sb.append(originalDeclaration.contains(";\n") ? originalDeclaration.substring(0, originalDeclaration.indexOf(";\n")) : originalDeclaration);
        sb.append(", ");
        sb.append(originalAssignment.contains(";\n") ? originalAssignment.substring(0, originalAssignment.indexOf(";\n")) : originalAssignment);
        sb.append("]");
        sb.append(" to ");
        sb.append(mergedDeclaration.contains(";\n") ? mergedDeclaration.substring(0, mergedDeclaration.indexOf(";\n")) : mergedDeclaration);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getOriginalDeclaration() {
        return originalDeclaration;
    }

    public String getOriginalAssignment() {
        return originalAssignment;
    }

    public String getMergedDeclaration() {
        return mergedDeclaration;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
