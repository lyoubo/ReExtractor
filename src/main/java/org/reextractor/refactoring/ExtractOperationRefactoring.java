package org.reextractor.refactoring;

import org.reextractor.util.MethodUtils;
import org.remapper.dto.CodeRange;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.StatementNodeTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExtractOperationRefactoring implements Refactoring {

    private DeclarationNodeTree extractedOperation;
    private DeclarationNodeTree sourceOperationBeforeExtraction;
    private DeclarationNodeTree sourceOperationAfterExtraction;
    private List<StatementNodeTree> extractedCodeFragmentsFromSourceOperation;
    private List<StatementNodeTree> extractedCodeFragmentsToExtractedOperation;

    public ExtractOperationRefactoring(DeclarationNodeTree sourceOperationBeforeExtraction, DeclarationNodeTree sourceOperationAfterExtraction,
                                       DeclarationNodeTree extractedOperation) {
        this.sourceOperationBeforeExtraction = sourceOperationBeforeExtraction;
        this.sourceOperationAfterExtraction = sourceOperationAfterExtraction;
        this.extractedOperation = extractedOperation;
        this.extractedCodeFragmentsFromSourceOperation = sourceOperationBeforeExtraction.getMethodNode().getMatchedStatements();
        extractedCodeFragmentsFromSourceOperation.sort(Comparator.comparingInt(StatementNodeTree::getPosition));
        this.extractedCodeFragmentsToExtractedOperation = extractedOperation.getMethodNode().getMatchedStatements();
        extractedCodeFragmentsToExtractedOperation.sort(Comparator.comparingInt(StatementNodeTree::getPosition));
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_OPERATION;
    }

    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(sourceOperationBeforeExtraction.codeRange()
                .setDescription("source method declaration before extraction")
                .setCodeElement(MethodUtils.method2String(sourceOperationBeforeExtraction)));
        for(StatementNodeTree extractedCodeFragment : extractedCodeFragmentsFromSourceOperation) {
            ranges.add(extractedCodeFragment.codeRange()
                    .setDescription("extracted code from source method declaration")
                    .setCodeElement(extractedCodeFragment.getExpression()));
        }
        return ranges;
    }

    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(extractedOperation.codeRange()
                .setDescription("extracted method declaration")
                .setCodeElement(MethodUtils.method2String(extractedOperation)));
        for(StatementNodeTree extractedCodeFragment : extractedCodeFragmentsToExtractedOperation) {
            ranges.add(extractedCodeFragment.codeRange()
                    .setDescription("extracted code to extracted method declaration")
                    .setCodeElement(extractedCodeFragment.getExpression()));
        }
        ranges.add(sourceOperationAfterExtraction.codeRange()
                .setDescription("source method declaration after extraction")
                .setCodeElement(MethodUtils.method2String(sourceOperationAfterExtraction)));
        return ranges;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(MethodUtils.method2String(extractedOperation));
        sb.append(" extracted from ");
        sb.append(MethodUtils.method2String(sourceOperationBeforeExtraction));
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
