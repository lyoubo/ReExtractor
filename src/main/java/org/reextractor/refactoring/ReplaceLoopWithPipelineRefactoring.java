package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ReplaceLoopWithPipelineRefactoring implements Refactoring {

    private String loop;
    private String pipeline;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ReplaceLoopWithPipelineRefactoring(String loop, String pipeline, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.loop = loop;
        this.pipeline = pipeline;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_LOOP_WITH_PIPELINE;
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
        sb.append(loop);
        sb.append(" with ");
        sb.append(pipeline);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getLoop() {
        return loop;
    }

    public String getPipeline() {
        return pipeline;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
