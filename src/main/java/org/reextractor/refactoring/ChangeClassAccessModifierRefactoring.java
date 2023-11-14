package org.reextractor.refactoring;

import org.reextractor.dto.Visibility;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

public class ChangeClassAccessModifierRefactoring implements Refactoring {

    private Visibility originalAccessModifier;
    private Visibility changedAccessModifier;
    private DeclarationNodeTree classBefore;
    private DeclarationNodeTree classAfter;

    public ChangeClassAccessModifierRefactoring(Visibility originalAccessModifier, Visibility changedAccessModifier,
                                                DeclarationNodeTree classBefore, DeclarationNodeTree classAfter) {
        this.originalAccessModifier = originalAccessModifier;
        this.changedAccessModifier = changedAccessModifier;
        this.classBefore = classBefore;
        this.classAfter = classAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_CLASS_ACCESS_MODIFIER;
    }

    public LocationInfo leftSide() {
        return classBefore.getLocation();
    }

    public LocationInfo rightSide() {
        return classAfter.getLocation();
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append("\t");
        sb.append(originalAccessModifier);
        sb.append(" to ");
        sb.append(changedAccessModifier);
        sb.append(" in class ");
        sb.append(classAfter.getNamespace() + "." + classAfter.getName());
        return sb.toString();
    }

    public Visibility getOriginalAccessModifier() {
        return originalAccessModifier;
    }

    public Visibility getChangedAccessModifier() {
        return changedAccessModifier;
    }

    public DeclarationNodeTree getClassBefore() {
        return classBefore;
    }

    public DeclarationNodeTree getClassAfter() {
        return classAfter;
    }
}
