package org.reextractor.refactoring;

import org.remapper.dto.LocationInfo;

public interface Refactoring {

    RefactoringType getRefactoringType();

    LocationInfo leftSide();

    LocationInfo rightSide();

    public String getName();

    public String toString();
}
