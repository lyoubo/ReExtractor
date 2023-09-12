package org.reextractor.refactoring;

import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;
import org.remapper.util.StringUtils;

public class MoveClassRefactoring implements Refactoring {

    private DeclarationNodeTree originalClass;
    private DeclarationNodeTree movedClass;

    public MoveClassRefactoring(DeclarationNodeTree originalClass, DeclarationNodeTree movedClass) {
        this.originalClass = originalClass;
        this.movedClass = movedClass;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.MOVE_CLASS;
    }

    public LocationInfo leftSide() {
        return originalClass.getLocation();
    }

    public LocationInfo rightSide() {
        return movedClass.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        if (org.remapper.util.StringUtils.isNotEmpty(originalClass.getNamespace()))
            sb.append(originalClass.getNamespace()).append(".").append(originalClass.getName());
        else
            sb.append(originalClass.getName());
        sb.append(" moved to ");
        if (StringUtils.isNotEmpty(movedClass.getNamespace()))
            sb.append(movedClass.getNamespace()).append(".").append(movedClass.getName());
        else
            sb.append(movedClass.getName());
        return sb.toString();
    }

    public DeclarationNodeTree getOriginalClass() {
        return originalClass;
    }

    public DeclarationNodeTree getMovedClass() {
        return movedClass;
    }
}
