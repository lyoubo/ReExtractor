package org.reextractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jgit.lib.Repository;
import org.reextractor.dto.RefactoringDiscoveryJSON;
import org.reextractor.handler.RefactoringHandler;
import org.reextractor.refactoring.Refactoring;
import org.reextractor.service.RefactoringExtractorService;
import org.reextractor.service.RefactoringExtractorServiceImpl;
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

        if (option.equalsIgnoreCase("-c")) {
            detectAtCommit(args);
        } else {
            throw argumentException();
        }
    }

    private static void processJSONOption(String[] args) {
        if (args[args.length - 2].equalsIgnoreCase("-json")) {
            path = Paths.get(args[args.length - 1]);
        }
        if (Files.exists(path) && path.toFile().length() == 0) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void detectAtCommit(String[] args) throws Exception {
        processJSONOption(args);
        if (args.length != 5) {
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
                public void handle(String commitId, List<Refactoring> refactorings) {
                    commitJSON(gitURL, commitId, refactorings);
                }

                @Override
                public void handleException(String commit, Exception e) {
                    System.err.println("Error processing commit " + commit);
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    private static void commitJSON(String cloneURL, String currentCommitId, List<Refactoring> refactorings) {
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
                results.populateJSON(cloneURL, currentCommitId, url, refactorings);
                String jsonString = gson.toJson(results, RefactoringDiscoveryJSON.class).replace("\\t", "\t");
                out.write(jsonString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try (FileReader reader = new FileReader(path.toFile())) {
                RefactoringDiscoveryJSON results = gson.fromJson(reader, RefactoringDiscoveryJSON.class);
                results.populateJSON(cloneURL, currentCommitId, url, refactorings);
                String jsonString = gson.toJson(results, RefactoringDiscoveryJSON.class).replace("\\t", "\t");
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
                "-c <git-repo-folder> <commit-sha1> -json <path-to-json-file>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
    }

    private static IllegalArgumentException argumentException() {
        return new IllegalArgumentException("Type `ReExtractor -h` to show usage.");
    }
}
