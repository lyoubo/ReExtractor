package org.reextractor.dto;


import java.util.ArrayList;
import java.util.List;

public class RefactoringDiscoveryJSON {

    private List<Result> results;

    public RefactoringDiscoveryJSON() {
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
        private List<CodeRange> leftSideLocation;
        private List<CodeRange> rightSideLocation;

        public Refactoring(org.reextractor.refactoring.Refactoring refactoring) {
            this.type = refactoring.getRefactoringType().toString();
            this.description = refactoring.toString();
            this.leftSideLocation = new ArrayList<>();
            for (org.remapper.dto.CodeRange codeRange : refactoring.leftSide()) {
                this.leftSideLocation.add(new CodeRange(codeRange));
            }
            this.rightSideLocation = new ArrayList<>();
            for (org.remapper.dto.CodeRange codeRange : refactoring.rightSide()) {
                this.rightSideLocation.add(new CodeRange(codeRange));
            }
        }
    }

    class CodeRange {
        private final String filePath;
        private final int startLine;
        private final int endLine;
        private final int startColumn;
        private final int endColumn;
        private final String codeElementType;
        private final String description;
        private final String codeElement;

        public CodeRange(org.remapper.dto.CodeRange codeRange) {
            this.filePath = codeRange.getFilePath();
            this.startLine = codeRange.getStartLine();
            this.endLine = codeRange.getEndLine();
            this.startColumn = codeRange.getStartColumn();
            this.endColumn = codeRange.getEndColumn();
            this.codeElementType = codeRange.getCodeElementType().name();
            this.description = codeRange.getDescription();
            this.codeElement = codeRange.getCodeElement();
        }
    }
}
