package org.reextractor.refactoring;

import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;
import org.remapper.util.StringUtils;

public class RenameClassRefactoring implements Refactoring {

    private DeclarationNodeTree originalClass;
    private DeclarationNodeTree renamedClass;

    public RenameClassRefactoring(DeclarationNodeTree originalClass, DeclarationNodeTree renamedClass) {
        this.originalClass = originalClass;
        this.renamedClass = renamedClass;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.RENAME_CLASS;
    }

    public LocationInfo leftSide() {
        return originalClass.getLocation();
    }

    public LocationInfo rightSide() {
        return renamedClass.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        if (StringUtils.isNotEmpty(originalClass.getNamespace()))
            sb.append(originalClass.getNamespace()).append(".").append(originalClass.getName());
        else
            sb.append(originalClass.getName());
        sb.append(" renamed to ");
        if (StringUtils.isNotEmpty(renamedClass.getNamespace()))
            sb.append(renamedClass.getNamespace()).append(".").append(renamedClass.getName());
        else
            sb.append(renamedClass.getName());
        return sb.toString();
    }

    public DeclarationNodeTree getOriginalClass() {
        return originalClass;
    }

    public DeclarationNodeTree getRenamedClass() {
        return renamedClass;
    }
}
