package org.reextractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
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

public class ReExtractorTest {

    @Test
    public void detectAtCommit() throws Exception {
        String folder = "E:\\commons-math";
        String commitId = "482ebca8f54c6d1c6ef3d07710d0717334bc0eee";
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
        Path path = Paths.get("E:/results.json");
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
}
