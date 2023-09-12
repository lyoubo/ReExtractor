package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ReplacePipelineWithLoopRefactoring implements Refactoring {

    private String pipeline;
    private String loop;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ReplacePipelineWithLoopRefactoring(String pipeline, String loop, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.pipeline = pipeline;
        this.loop = loop;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_PIPELINE_WITH_LOOP;
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
        sb.append(pipeline);
        sb.append(" with ");
        sb.append(loop);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getPipeline() {
        return pipeline;
    }

    public String getLoop() {
        return loop;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
