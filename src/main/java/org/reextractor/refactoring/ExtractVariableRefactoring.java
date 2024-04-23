package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.VariableUtils;
import org.remapper.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExtractVariableRefactoring implements Refactoring {

    private VariableDeclaration variableDeclaration;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;
    private Map<StatementNodeTree, StatementNodeTree> references;

    public ExtractVariableRefactoring(VariableDeclaration variableDeclaration, DeclarationNodeTree operationBefore,
                                      DeclarationNodeTree operationAfter, Map<StatementNodeTree, StatementNodeTree> references) {
        this.variableDeclaration = variableDeclaration;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
        this.references = references;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_VARIABLE;
    }

    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        for (StatementNodeTree mapping : references.keySet()) {
            ranges.add(mapping.codeRange()
                    .setDescription("statement with the initializer of the extracted variable")
                    .setCodeElement(mapping.getExpression()));
        }
        ranges.add(operationBefore.codeRange()
                .setDescription("original method declaration")
                .setCodeElement(MethodUtils.method2String(operationBefore)));
        return ranges;
    }

    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        LocationInfo variableLocation = new LocationInfo(
                (CompilationUnit) variableDeclaration.getRoot(), operationAfter.getFilePath(), variableDeclaration);
        ranges.add(variableLocation.codeRange()
                .setDescription("extracted variable declaration")
                .setCodeElement(VariableUtils.variable2String(variableDeclaration)));
        for (StatementNodeTree mapping : references.keySet()) {
            ranges.add(references.get(mapping).codeRange()
                    .setDescription("statement with the name of the extracted variable")
                    .setCodeElement(references.get(mapping).getExpression()));
        }
        ranges.add(operationAfter.codeRange()
                .setDescription("method declaration with extracted variable")
                .setCodeElement(MethodUtils.method2String(operationAfter)));
        return ranges;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(VariableUtils.variable2String(variableDeclaration));
        if (operationAfter.getType() == EntityType.INITIALIZER) {
            sb.append(" in initializer ");
        } else {
            sb.append(" in method ");
            sb.append(MethodUtils.method2String(operationAfter));
        }
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public VariableDeclaration getVariableDeclaration() {
        return variableDeclaration;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
