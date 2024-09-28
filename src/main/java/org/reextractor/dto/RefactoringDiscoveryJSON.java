package org.reextractor.dto;


import org.remapper.dto.MatchPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RefactoringDiscoveryJSON {

    private List<Result> results;

    public RefactoringDiscoveryJSON() {
        results = new ArrayList<>();
    }

    public void populateJSON(String repository, String sha1, String url, MatchPair matchPair, List<org.reextractor.refactoring.Refactoring> refactorings) {
        Result result = new Result(repository, sha1, url, matchPair, refactorings);
        results.add(result);
    }

    class Result {
        private String repository;
        private String sha1;
        private String url;
        private List<FileContent> files;
        private List<Refactoring> refactorings;

        public Result(String repository, String sha1, String url, MatchPair matchPair, List<org.reextractor.refactoring.Refactoring> refactorings) {
            this.repository = repository;
            this.sha1 = sha1;
            this.url = url;
            this.files = new ArrayList<>();
            Map<String, String> fileContentsBefore = matchPair.getFileContentsBefore();
            Map<String, String> fileContentsCurrent = matchPair.getFileContentsCurrent();
            Set<String> modifiedFiles = matchPair.getModifiedFiles();
            Map<String, String> renamedFiles = matchPair.getRenamedFiles();
            Set<String> deletedFiles = matchPair.getDeletedFiles();
            Set<String> addedFiles = matchPair.getAddedFiles();
            for (String name : modifiedFiles) {
                FileContent fileContent = new FileContent(name, fileContentsBefore.get(name), fileContentsCurrent.get(name));
                files.add(fileContent);
            }
            for (String oldName : renamedFiles.keySet()) {
                String newName = renamedFiles.get(oldName);
                FileContent fileContent = new FileContent(oldName + " --> " + newName, fileContentsBefore.get(oldName), fileContentsCurrent.get(newName));
                files.add(fileContent);
            }
            for (String name : deletedFiles) {
                FileContent fileContent = new FileContent(name, fileContentsBefore.get(name), "");
                files.add(fileContent);
            }
            for (String name : addedFiles) {
                FileContent fileContent = new FileContent(name, "", fileContentsCurrent.get(name));
                files.add(fileContent);
            }
            this.refactorings = new ArrayList<>();
            for (org.reextractor.refactoring.Refactoring refactoring : refactorings) {
                Refactoring ref = new Refactoring(refactoring);
                this.refactorings.add(ref);
            }
        }
    }

    class FileContent {
        private String name;
        private String oldCode;
        private String newCode;

        public FileContent(String name, String oldCode, String newCode) {
            this.name = name;
            this.oldCode = oldCode;
            this.newCode = newCode;
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
