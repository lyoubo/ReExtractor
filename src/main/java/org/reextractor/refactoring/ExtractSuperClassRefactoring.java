package org.reextractor.refactoring;

import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ExtractSuperClassRefactoring implements Refactoring {

    private DeclarationNodeTree extractedClass;
    private DeclarationNodeTree originalClass;
    private DeclarationNodeTree nextClass;

    public ExtractSuperClassRefactoring(DeclarationNodeTree originalClass, DeclarationNodeTree nextClass,
                                        DeclarationNodeTree extractedClass) {
        this.originalClass = originalClass;
        this.nextClass = nextClass;
        this.extractedClass = extractedClass;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.EXTRACT_SUPERCLASS;
    }

    public LocationInfo leftSide() {
        return originalClass.getLocation();
    }

    public LocationInfo rightSide() {
        return extractedClass.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(extractedClass.getNamespace());
        sb.append(" from class ");
        sb.append(originalClass.getNamespace());
        return sb.toString();
    }

    public DeclarationNodeTree getExtractedClass() {
        return extractedClass;
    }

    public DeclarationNodeTree getOriginalClass() {
        return originalClass;
    }

    public DeclarationNodeTree getNextClass() {
        return nextClass;
    }
}
