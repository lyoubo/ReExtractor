package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.CodeRange;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.EntityType;
import org.remapper.dto.LocationInfo;

import java.util.ArrayList;
import java.util.List;

public class AddVariableModifierRefactoring implements Refactoring {

    private String modifier;
    private VariableDeclaration variableBefore;
    private VariableDeclaration variableAfter;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public AddVariableModifierRefactoring(String modifier, VariableDeclaration variableBefore, VariableDeclaration variableAfter,
                                          DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.modifier = modifier;
        this.variableBefore = variableBefore;
        this.variableAfter = variableAfter;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.ADD_VARIABLE_MODIFIER;
    }

    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        LocationInfo variableLocation = new LocationInfo(
                (CompilationUnit) variableBefore.getRoot(), operationBefore.getFilePath(), variableBefore);
        ranges.add(variableLocation.codeRange()
                .setDescription("original variable declaration")
                .setCodeElement(VariableUtils.variable2String(variableBefore)));
        ranges.add(operationBefore.codeRange()
                .setDescription("original method declaration")
                .setCodeElement(MethodUtils.method2String(operationBefore)));
        return ranges;
    }

    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        LocationInfo variableLocation = new LocationInfo(
                (CompilationUnit) variableAfter.getRoot(), operationAfter.getFilePath(), variableAfter);
        ranges.add(variableLocation.codeRange()
                .setDescription("variable declaration with added modifier")
                .setCodeElement(VariableUtils.variable2String(variableAfter)));
        ranges.add(operationAfter.codeRange().
                setDescription("method declaration with added variable modifier")
                .setCodeElement(MethodUtils.method2String(operationAfter)));
        return ranges;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(modifier);
        sb.append(" in variable ");
        sb.append(VariableUtils.variable2String(variableAfter));
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

    public String getModifier() {
        return modifier;
    }

    public VariableDeclaration getVariableBefore() {
        return variableBefore;
    }

    public VariableDeclaration getVariableAfter() {
        return variableAfter;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
