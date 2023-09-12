package org.reextractor.refactoring;

import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;
import org.remapper.util.StringUtils;

public class ChangeTypeDeclarationKindRefactoring implements Refactoring {

    private DeclarationNodeTree classBefore;
    private DeclarationNodeTree classAfter;

    public ChangeTypeDeclarationKindRefactoring(DeclarationNodeTree classBefore, DeclarationNodeTree classAfter) {
        this.classBefore = classBefore;
        this.classAfter = classAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_TYPE_DECLARATION_KIND;
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
        sb.append(classBefore.getType().getName());
        sb.append(" to ");
        sb.append(classAfter.getType().getName());
        sb.append(" in type ");
        if (StringUtils.isNotEmpty(classAfter.getNamespace()))
            sb.append(classAfter.getNamespace()).append(".").append(classAfter.getName());
        else
            sb.append(classAfter.getName());
        return sb.toString();
    }

    public DeclarationNodeTree getClassBefore() {
        return classBefore;
    }

    public DeclarationNodeTree getClassAfter() {
        return classAfter;
    }
}
