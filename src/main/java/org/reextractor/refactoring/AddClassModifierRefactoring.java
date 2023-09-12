package org.reextractor.refactoring;

import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;
import org.remapper.util.StringUtils;

public class AddClassModifierRefactoring implements Refactoring {

    private String modifier;
    private DeclarationNodeTree classBefore;
    private DeclarationNodeTree classAfter;

    public AddClassModifierRefactoring(String modifier, DeclarationNodeTree classBefore, DeclarationNodeTree classAfter) {
        this.modifier = modifier;
        this.classBefore = classBefore;
        this.classAfter = classAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.ADD_CLASS_MODIFIER;
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
        sb.append(modifier);
        sb.append(" in class ");
        if (StringUtils.isNotEmpty(classAfter.getNamespace()))
            sb.append(classAfter.getNamespace()).append(".").append(classAfter.getName());
        else
            sb.append(classAfter.getName());
        return sb.toString();
    }

    public String getModifier() {
        return modifier;
    }

    public DeclarationNodeTree getClassBefore() {
        return classBefore;
    }

    public DeclarationNodeTree getClassAfter() {
        return classAfter;
    }
}
