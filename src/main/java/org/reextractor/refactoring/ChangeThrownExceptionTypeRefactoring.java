package org.reextractor.refactoring;

import org.eclipse.jdt.core.dom.SimpleType;
import org.reextractor.util.MethodUtils;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.LocationInfo;

import java.util.Set;

public class ChangeThrownExceptionTypeRefactoring implements Refactoring {

    private Set<SimpleType> originalTypes;
    private Set<SimpleType> changedTypes;
    private DeclarationNodeTree operationBefore;
    private DeclarationNodeTree operationAfter;

    public ChangeThrownExceptionTypeRefactoring(Set<SimpleType> originalTypes, Set<SimpleType> changedTypes,
                                                DeclarationNodeTree operationBefore, DeclarationNodeTree operationAfter) {
        this.originalTypes = originalTypes;
        this.changedTypes = changedTypes;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return RefactoringType.CHANGE_THROWN_EXCEPTION_TYPE;
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
        if (originalTypes.size() == 1)
            sb.append(originalTypes.iterator().next().getName().getFullyQualifiedName());
        else {
            int len = 0;
            sb.append("[");
            for (SimpleType type : originalTypes) {
                sb.append(type.getName().getFullyQualifiedName());
                if (len < originalTypes.size() - 1)
                    sb.append(", ");
                len += 1;
            }
            sb.append("]");
        }
        sb.append(" to ");
        if (changedTypes.size() == 1)
            sb.append(changedTypes.iterator().next().getName().getFullyQualifiedName());
        else {
            int len = 0;
            sb.append("[");
            for (SimpleType type : changedTypes) {
                sb.append(type.getName().getFullyQualifiedName());
                if (len < changedTypes.size() - 1)
                    sb.append(", ");
                len += 1;
            }
            sb.append("]");
        }
        sb.append(" in method ");
        sb.append(MethodUtils.getMethodDeclaration(operationAfter));
        sb.append(" from class ");
        sb.append(operationAfter.getNamespace());
        return sb.toString();
    }

    public Set<SimpleType> getOriginalTypes() {
        return originalTypes;
    }

    public Set<SimpleType> getChangedTypes() {
        return changedTypes;
    }

    public DeclarationNodeTree getOperationBefore() {
        return operationBefore;
    }

    public DeclarationNodeTree getOperationAfter() {
        return operationAfter;
    }
}
