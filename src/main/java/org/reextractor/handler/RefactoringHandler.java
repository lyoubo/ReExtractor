package org.reextractor.handler;

import org.reextractor.refactoring.Refactoring;
import org.remapper.dto.MatchPair;

import java.util.List;

public abstract class RefactoringHandler {

    /**
     * This method is called after each commit is analyzed.
     * You should override this method to do your custom logic with the list of detected refactorings.
     *
     * @param commitId     The sha of the analyzed commit.
     * @param refactorings List of refactorings detected in the commit.
     */
    public void handle(String commitId, List<Refactoring> refactorings) {
    }

    /**
     * This method is called after each commit is analyzed.
     * You should override this method to do your custom logic with the list of detected refactorings.
     *
     * @param commitId     The sha of the analyzed commit.
     * @param matchPair    The matched entities and statements.
     * @param refactorings List of refactorings detected in the commit.
     */
    public void handle(String commitId, MatchPair matchPair, List<Refactoring> refactorings) {
    }

    /**
     * This method is called after each commit is analyzed.
     * You should override this method to do your custom logic with the list of detected refactorings.
     *
     * @param startCommitId The SHA key that identifies the commit to start the log lookup.
     * @param matchPair     The matched entities and statements.
     * @param endCommitId   The SHA key that identifies the commit to end the log lookup.
     * @param refactorings  List of refactorings detected in the commit.
     */
    public void handle(String startCommitId, String endCommitId, MatchPair matchPair, List<Refactoring> refactorings) {
    }

    /**
     * This method is called whenever an exception is thrown during the analysis of the given commit.
     * You should override this method to do your custom logic in the case of exceptions (e.g. skip or rethrow).
     *
     * @param commitId The SHA key that identifies the commit.
     * @param e        The exception thrown.
     */
    public void handleException(String commitId, Exception e) {
        throw new RuntimeException(e);
    }

    /**
     * This method is called whenever an exception is thrown during the analysis of the given commit.
     * You should override this method to do your custom logic in the case of exceptions (e.g. skip or rethrow).
     *
     * @param startCommitId The SHA key that identifies the commit to start the log lookup.
     * @param endCommitId   The SHA key that identifies the commit to end the log lookup.
     * @param e             The exception thrown.
     */
    public void handleException(String startCommitId, String endCommitId, Exception e) {
        throw new RuntimeException(e);
    }

    /**
     * This method is called after all commits are analyzed.
     * You may override this method to implement custom logic.
     *
     * @param refactoringsCount Total number of refactorings detected.
     * @param commitsCount      Total number of commits analyzed.
     * @param errorCommitsCount Total number of commits not analyzed due to errors.
     */
    public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
    }
}
