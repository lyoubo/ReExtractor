package org.reextractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jgit.lib.Repository;
import org.reextractor.dto.RefactoringDiscoveryJSON;
import org.reextractor.handler.RefactoringHandler;
import org.reextractor.refactoring.Refactoring;
import org.reextractor.service.RefactoringExtractorService;
import org.reextractor.service.RefactoringExtractorServiceImpl;
import org.remapper.dto.MatchPair;
import org.remapper.service.GitService;
import org.remapper.util.GitServiceImpl;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReExtractor {

    private static Path path = null;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw argumentException();
        }

        final String option = args[0];
        if (option.equalsIgnoreCase("-h") || option.equalsIgnoreCase("--h") || option.equalsIgnoreCase("-help")
                || option.equalsIgnoreCase("--help")) {
            printTips();
            return;
        }

        if (option.equalsIgnoreCase("-a")) {
            detectAll(args);
        } else if (option.equalsIgnoreCase("-ac")) {
            detectAllBetweenCommits(args);
        } else if (option.equalsIgnoreCase("-at")) {
            detectAllBetweenTags(args);
        } else if (option.equalsIgnoreCase("-bc")) {
            detectBetweenCommits(args);
        } else if (option.equalsIgnoreCase("-bt")) {
            detectBetweenTags(args);
        } else if (option.equalsIgnoreCase("-c")) {
            detectAtCommit(args);
        } else {
            throw argumentException();
        }
    }

    public static void detectAll(String[] args) throws Exception {
        int maxArgLength = processJSONoption(args, 3);
        if (args.length > maxArgLength) {
            throw argumentException();
        }
        String folder = args[1];
        String branch = null;
        if (containsBranchArgument(args)) {
            branch = args[2];
        }
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = GitServiceImpl.getRemoteUrl(folder);
            RefactoringExtractorService service = new RefactoringExtractorServiceImpl();
            service.detectAll(repo, branch, new RefactoringHandler() {
                @Override
                public void handle(String commitId, MatchPair matchPair, List<Refactoring> refactorings) {
                    commitJSON(gitURL, commitId, matchPair, refactorings);
                }

                @Override
                public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
                    System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
                            commitsCount, errorCommitsCount, refactoringsCount));
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    private static boolean containsBranchArgument(String[] args) {
        return args.length == 3 || (args.length > 3 && args[3].equalsIgnoreCase("-json"));
    }

    private static int processJSONoption(String[] args, int maxArgLength) {
        if (args[args.length - 2].equalsIgnoreCase("-json")) {
            path = Paths.get(args[args.length - 1]);
            maxArgLength = maxArgLength + 2;
        }
        return maxArgLength;
    }

    public static void detectBetweenCommits(String[] args) throws Exception {
        int maxArgLength = processJSONoption(args, 4);
        if (!(args.length == maxArgLength - 1 || args.length == maxArgLength)) {
            throw argumentException();
        }
        String folder = args[1];
        String startCommit = args[2];
        String endCommit = containsEndArgument(args) ? args[3] : null;
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = GitServiceImpl.getRemoteUrl(folder);
            RefactoringExtractorService service = new RefactoringExtractorServiceImpl();
            service.detectBetweenCommits(repo, startCommit, endCommit, new RefactoringHandler() {
                @Override
                public void handle(String startCommitId, String endCommitId, MatchPair matchPair, List<Refactoring> refactorings) {
                    commitJSON(gitURL, endCommitId, matchPair, refactorings);
                }

                @Override
                public void handleException(String startCommitId, String endCommitId, Exception e) {
                    System.err.println("Error processing commit " + endCommitId);
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    public static void detectBetweenTags(String[] args) throws Exception {
        int maxArgLength = processJSONoption(args, 4);
        if (!(args.length == maxArgLength - 1 || args.length == maxArgLength)) {
            throw argumentException();
        }
        String folder = args[1];
        String startTag = args[2];
        String endTag = containsEndArgument(args) ? args[3] : null;
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = GitServiceImpl.getRemoteUrl(folder);
            RefactoringExtractorService service = new RefactoringExtractorServiceImpl();
            service.detectBetweenTags(repo, startTag, endTag, new RefactoringHandler() {
                @Override
                public void handle(String startTag, String endTag, MatchPair matchPair, List<Refactoring> refactorings) {
                    commitJSON(gitURL, endTag, matchPair, refactorings);
                }

                @Override
                public void handleException(String startTag, String endTag, Exception e) {
                    System.err.println("Error processing tag " + endTag);
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    public static void detectAllBetweenCommits(String[] args) throws Exception {
        int maxArgLength = processJSONoption(args, 4);
        if (!(args.length == maxArgLength - 1 || args.length == maxArgLength)) {
            throw argumentException();
        }
        String folder = args[1];
        String startCommit = args[2];
        String endCommit = containsEndArgument(args) ? args[3] : null;
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = GitServiceImpl.getRemoteUrl(folder);
            RefactoringExtractorService service = new RefactoringExtractorServiceImpl();
            service.detectAllBetweenCommits(repo, startCommit, endCommit, new RefactoringHandler() {
                @Override
                public void handle(String commitId, MatchPair matchPair, List<Refactoring> refactorings) {
                    commitJSON(gitURL, commitId, matchPair, refactorings);
                }

                @Override
                public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
                    System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
                            commitsCount, errorCommitsCount, refactoringsCount));
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    public static void detectAllBetweenTags(String[] args) throws Exception {
        int maxArgLength = processJSONoption(args, 4);
        if (!(args.length == maxArgLength - 1 || args.length == maxArgLength)) {
            throw argumentException();
        }
        String folder = args[1];
        String startTag = args[2];
        String endTag = containsEndArgument(args) ? args[3] : null;
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = GitServiceImpl.getRemoteUrl(folder);
            RefactoringExtractorService service = new RefactoringExtractorServiceImpl();
            service.detectAllBetweenTags(repo, startTag, endTag, new RefactoringHandler() {
                @Override
                public void handle(String commitId, MatchPair matchPair, List<Refactoring> refactorings) {
                    commitJSON(gitURL, commitId, matchPair, refactorings);
                }

                @Override
                public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
                    System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
                            commitsCount, errorCommitsCount, refactoringsCount));
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    private static boolean containsEndArgument(String[] args) {
        return args.length == 4 || (args.length > 4 && args[4].equalsIgnoreCase("-json"));
    }

    public static void detectAtCommit(String[] args) throws Exception {
        int maxArgLength = processJSONoption(args, 3);
        if (args.length != maxArgLength) {
            throw argumentException();
        }
        String folder = args[1];
        String commitId = args[2];
        GitService gitService = new GitServiceImpl();
        try (Repository repo = gitService.openRepository(folder)) {
            String gitURL = GitServiceImpl.getRemoteUrl(folder);
            RefactoringExtractorService service = new RefactoringExtractorServiceImpl();
            service.detectAtCommit(repo, commitId, new RefactoringHandler() {
                @Override
                public void handle(String commitId, MatchPair matchPair, List<Refactoring> refactorings) {
                    commitJSON(gitURL, commitId, matchPair, refactorings);
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    private static void commitJSON(String cloneURL, String currentCommitId, MatchPair matchPair, List<Refactoring> refactorings) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        String url = cloneURL.replace(".git", "/commit/") + currentCommitId;
        if (Files.notExists(path)) {
            Path parent = path.getParent();
            try {
                if (Files.notExists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (BufferedWriter out = new BufferedWriter(new FileWriter(path.toFile()))) {
                RefactoringDiscoveryJSON results = new RefactoringDiscoveryJSON();
                results.populateJSON(cloneURL, currentCommitId, url, matchPair, refactorings);
                String jsonString = gson.toJson(results, RefactoringDiscoveryJSON.class);
                out.write(jsonString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try (FileReader reader = new FileReader(path.toFile())) {
                RefactoringDiscoveryJSON results = gson.fromJson(reader, RefactoringDiscoveryJSON.class);
                results.populateJSON(cloneURL, currentCommitId, url, matchPair, refactorings);
                String jsonString = gson.toJson(results, RefactoringDiscoveryJSON.class);
                BufferedWriter out = new BufferedWriter(new FileWriter(path.toFile()));
                out.write(jsonString);
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void printTips() {
        System.out.println("-h\t\t\t\t\t\t\t\t\t\t\tShow options");
        System.out.println(
                "-a <git-repo-folder> <branch> -json <path-to-json-file>\t\t\t\t\tDetect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.");
        System.out.println(
                "-ac <git-repo-folder> <start-commit-sha1> <end-commit-sha1> -json <path-to-json-file>\tDetect refactorings from all commits between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
        System.out.println(
                "-at <git-repo-folder> <start-tag> <end-tag> -json <path-to-json-file>\t\t\tDetect refactorings from all commits between <start-tag> and <end-tag> for project <git-repo-folder>");
        System.out.println(
                "-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1> -json <path-to-json-file>\tDetect refactorings between <start-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
        System.out.println(
                "-bt <git-repo-folder> <start-tag> <end-tag> -json <path-to-json-file>\t\t\tDetect refactorings between <start-tag> and <end-tag> for project <git-repo-folder>");
        System.out.println(
                "-c <git-repo-folder> <commit-sha1> -json <path-to-json-file>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
    }

    private static IllegalArgumentException argumentException() {
        return new IllegalArgumentException("Type `ReExtractor -h` to show usage.");
    }
}
