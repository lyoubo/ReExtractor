package org.reextractor.dto;


import org.remapper.dto.LocationInfo;

import java.util.ArrayList;
import java.util.List;

public class RefactoringMiningJSON {

    private List<Result> results;

    public RefactoringMiningJSON() {
        results = new ArrayList<>();
    }

    public void populateJSON(String repository, String sha1, String url, List<org.reextractor.refactoring.Refactoring> refactorings) {
        Result result = new Result(repository, sha1, url, refactorings);
        results.add(result);
    }

    class Result {
        private String repository;
        private String sha1;
        private String url;
        private List<Refactoring> refactorings;

        public Result(String repository, String sha1, String url, List<org.reextractor.refactoring.Refactoring> refactorings) {
            this.repository = repository;
            this.sha1 = sha1;
            this.url = url;
            this.refactorings = new ArrayList<>();
            for (org.reextractor.refactoring.Refactoring refactoring : refactorings) {
                Refactoring ref = new Refactoring(refactoring);
                this.refactorings.add(ref);
            }
        }
    }

    class Refactoring {
        private String type;
        private String description;
        private Location leftSideLocation;
        private Location rightSideLocation;

        public Refactoring(org.reextractor.refactoring.Refactoring refactoring) {
            this.type = refactoring.getRefactoringType().toString();
            this.description = refactoring.toString();
            this.leftSideLocation = new Location(refactoring.leftSide());
            this.rightSideLocation = new Location(refactoring.rightSide());
        }
    }

    class Location {
        private final String filePath;
        private final int startLine;
        private final int endLine;
        private final int startColumn;
        private final int endColumn;

        public Location(LocationInfo location) {
            this.filePath = location.getFilePath();
            this.startLine = location.getStartLine();
            this.endLine = location.getEndLine();
            this.startColumn = location.getStartColumn();
            this.endColumn = location.getEndColumn();
        }
    }
}
