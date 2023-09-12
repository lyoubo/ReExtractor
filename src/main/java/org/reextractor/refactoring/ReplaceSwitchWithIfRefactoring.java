package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ReplaceSwitchWithIfRefactoring implements Refactoring {

    private String switchConditional;
    private String ifConditional;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ReplaceSwitchWithIfRefactoring(String switchConditional, String ifConditional, DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.switchConditional = switchConditional;
        this.ifConditional = ifConditional;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.REPLACE_SWITCH_WITH_IF;
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
        sb.append(switchConditional);
        sb.append(" with ");
        sb.append(ifConditional);
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public String getSwitchConditional() {
        return switchConditional;
    }

    public String getIfConditional() {
        return ifConditional;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
