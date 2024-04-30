package org.reextractor.service;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.reextractor.dto.AnnotationListDiff;
import org.reextractor.dto.Visibility;
import org.reextractor.handler.RefactoringHandler;
import org.reextractor.refactoring.*;
import org.reextractor.util.MethodUtils;
import org.reextractor.util.StringUtils;
import org.remapper.dto.*;
import org.remapper.handler.MatchingHandler;
import org.remapper.service.EntityMatcherService;
import org.remapper.service.EntityMatcherServiceImpl;
import org.remapper.util.DiceFunction;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RefactoringExtractorServiceImpl implements RefactoringExtractorService {

    @Override
    public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler) {
        RevWalk walk = new RevWalk(repository);
        try {
            RevCommit commit = walk.parseCommit(repository.resolve(commitId));
            if (commit.getParentCount() > 0) {
                walk.parseCommit(commit.getParent(0));
                this.detectRefactorings(repository, handler, commit);
            }
        } catch (MissingObjectException ignored) {
        } catch (Exception e) {
            handler.handleException(commitId, e);
        } finally {
            walk.close();
            walk.dispose();
        }
    }

    @Override
    public void detectAtCommit(Repository repository, String commitId, RefactoringHandler handler, int timeout) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> f = null;
        try {
            Runnable r = () -> detectAtCommit(repository, commitId, handler);
            f = service.submit(r);
            f.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }

    protected void detectRefactorings(Repository repository, final RefactoringHandler handler, RevCommit currentCommit) throws Exception {
        List<Refactoring> refactoringsAtRevision;
        EntityMatcherService service = new EntityMatcherServiceImpl();
        String commitId = currentCommit.getId().getName();
        if (currentCommit.getParentCount() > 0) {
            MatchPair matchPair = service.matchEntities(repository, currentCommit, new MatchingHandler() {
            });
            refactoringsAtRevision = detectRefactorings(matchPair);
        } else {
            refactoringsAtRevision = Collections.emptyList();
        }
        handler.handle(commitId, refactoringsAtRevision);
    }

    public void detectAtFiles(File previousFile, File nextFile, RefactoringHandler handler) {
        List<Refactoring> refactoringsAtRevision = Collections.emptyList();
        EntityMatcherService service = new EntityMatcherServiceImpl();
        String id = previousFile.getName() + " -> " + nextFile.getName();
        try {
            MatchPair matchPair = service.matchEntities(previousFile, nextFile, new MatchingHandler() {
            });
            refactoringsAtRevision = detectRefactorings(matchPair);
        } catch (Exception e) {
            handler.handleException(id, e);
        }
        handler.handle(id, refactoringsAtRevision);
    }

    protected List<Refactoring> detectRefactorings(MatchPair matchPair) {
        List<Refactoring> refactorings = new ArrayList<>();
        Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities = matchPair.getMatchedEntities();
        Set<DeclarationNodeTree> inlinedEntities = matchPair.getInlinedEntities();
        Set<DeclarationNodeTree> extractedEntities = matchPair.getExtractedEntities();
        Set<DeclarationNodeTree> addedEntities = matchPair.getAddedEntities();
        Set<DeclarationNodeTree> deletedEntities = matchPair.getDeletedEntities();
        Set<Pair<MethodNode, MethodNode>> methodNodePairs = mapMethodNodePairs(matchedEntities, inlinedEntities, extractedEntities);
        Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements = matchPair.getMatchedStatements();
        Set<StatementNodeTree> deletedStatements = matchPair.getDeletedStatements();
        Set<StatementNodeTree> addedStatements = matchPair.getAddedStatements();

        detectRefactoringsInMatchedEntities(matchedEntities, refactorings);
        detectRefactoringsBetweenMatchedAndAddedEntities(matchPair, matchedEntities, addedEntities, matchedStatements, refactorings);
        detectRefactoringsBetweenMatchedDeletedEntities(matchedEntities, deletedEntities, matchedStatements, matchPair, refactorings);

        detectRefactoringsInMatchedStatements(matchedStatements, refactorings);
        detectRefactoringsBetweenMatchedAndAddedStatements(methodNodePairs, matchedStatements, addedStatements, refactorings);
        detectRefactoringsBetweenMatchedAndDeletedStatements(methodNodePairs, matchedStatements, deletedStatements, refactorings);
        detectRefactoringsBetweenAddedAndDeletedStatements(methodNodePairs, addedStatements, deletedStatements, matchPair, refactorings);
        return refactorings;
    }

    private Set<Pair<MethodNode, MethodNode>> mapMethodNodePairs(Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities,
                                                                 Set<DeclarationNodeTree> inlinedEntities,
                                                                 Set<DeclarationNodeTree> extractedEntities) {
        Set<Pair<MethodNode, MethodNode>> methodNodePairs = new LinkedHashSet<>();
        for (Pair<DeclarationNodeTree, DeclarationNodeTree> matchedEntity : matchedEntities) {
            DeclarationNodeTree left = matchedEntity.getLeft();
            DeclarationNodeTree right = matchedEntity.getRight();
            if (left.getType() == EntityType.METHOD && right.getType() == EntityType.METHOD)
                methodNodePairs.add(Pair.of(left.getMethodNode(), right.getMethodNode()));
        }
        for (DeclarationNodeTree inlinedEntity : inlinedEntities) {
            if (inlinedEntity.getType() == EntityType.METHOD) {
                List<EntityInfo> dependencies = inlinedEntity.getDependencies();
                for (Pair<DeclarationNodeTree, DeclarationNodeTree> matchedEntity : matchedEntities) {
                    DeclarationNodeTree left = matchedEntity.getLeft();
                    DeclarationNodeTree right = matchedEntity.getRight();
                    if (left.getType() == EntityType.METHOD && right.getType() == EntityType.METHOD) {
                        if (dependencies.contains(left.getEntity())) {
                            methodNodePairs.add(Pair.of(inlinedEntity.getMethodNode(), right.getMethodNode()));
                        }
                    }
                }
            }
        }
        for (DeclarationNodeTree extractedEntity : extractedEntities) {
            if (extractedEntity.getType() == EntityType.METHOD) {
                List<EntityInfo> dependencies = extractedEntity.getDependencies();
                for (Pair<DeclarationNodeTree, DeclarationNodeTree> matchedEntity : matchedEntities) {
                    DeclarationNodeTree left = matchedEntity.getLeft();
                    DeclarationNodeTree right = matchedEntity.getRight();
                    if (left.getType() == EntityType.METHOD && right.getType() == EntityType.METHOD) {
                        if (dependencies.contains(right.getEntity())) {
                            methodNodePairs.add(Pair.of(left.getMethodNode(), extractedEntity.getMethodNode()));
                        }
                    }
                }
            }
        }
        return methodNodePairs;
    }

    private void detectRefactoringsInMatchedEntities(Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities,
                                                     List<Refactoring> refactorings) {
        for (Pair<DeclarationNodeTree, DeclarationNodeTree> pair : matchedEntities) {
            DeclarationNodeTree oldEntity = pair.getLeft();
            DeclarationNodeTree newEntity = pair.getRight();
            boolean isMove = !oldEntity.getNamespace().equals(newEntity.getNamespace()) &&
                    !matchedEntities.contains(Pair.of(oldEntity.getParent(), newEntity.getParent()));
            if (oldEntity.getType() == EntityType.METHOD && newEntity.getType() == EntityType.METHOD) {
                processOperations(isMove, matchedEntities, oldEntity, newEntity, refactorings);
            } else if (oldEntity.getType() == EntityType.FIELD && newEntity.getType() == EntityType.FIELD) {
                processAttributes(isMove, matchedEntities, oldEntity, newEntity, refactorings);
            } else if (oldEntity.getType() == EntityType.ENUM_CONSTANT && newEntity.getType() == EntityType.ENUM_CONSTANT) {
                processEnumConstants(isMove, oldEntity, newEntity, refactorings);
            } else if ((oldEntity.getType() == EntityType.CLASS || oldEntity.getType() == EntityType.INTERFACE ||
                    oldEntity.getType() == EntityType.ENUM || oldEntity.getType() == EntityType.ANNOTATION_TYPE) ||
                    (newEntity.getType() == EntityType.CLASS || newEntity.getType() == EntityType.INTERFACE ||
                            newEntity.getType() == EntityType.ENUM || newEntity.getType() == EntityType.ANNOTATION_TYPE)) {
                processClasses(isMove, oldEntity, newEntity, refactorings);
            }
        }
    }

    private void processOperations(boolean isMove, Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities,
                                   DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        MethodDeclaration removedOperation = (MethodDeclaration) oldEntity.getDeclaration();
        MethodDeclaration addedOperation = (MethodDeclaration) newEntity.getDeclaration();
        if (isMove) {
            if (isSubTypeOf(matchedEntities, oldEntity, newEntity, 0)) {
                PullUpOperationRefactoring refactoring = new PullUpOperationRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (isSubTypeOf(matchedEntities, newEntity, oldEntity, 1)) {
                PushDownOperationRefactoring refactoring = new PushDownOperationRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (!isSubTypeOf(matchedEntities, oldEntity, newEntity, 0) && !isSubTypeOf(matchedEntities, newEntity, oldEntity, 1)) {
                if (!oldEntity.getName().equals(newEntity.getName()) &&
                        !(removedOperation.isConstructor() && addedOperation.isConstructor())) {
                    MoveAndRenameOperationRefactoring refactoring = new MoveAndRenameOperationRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                } else {
                    MoveOperationRefactoring refactoring = new MoveOperationRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
        } else {
            if (!oldEntity.getName().equals(newEntity.getName()) &&
                    !(removedOperation.isConstructor() && addedOperation.isConstructor())) {
                RenameOperationRefactoring refactoring = new RenameOperationRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
        String originalType = removedOperation.getReturnType2() == null ? "" : removedOperation.getReturnType2().toString();
        String changedType = addedOperation.getReturnType2() == null ? "" : addedOperation.getReturnType2().toString();
        if (!StringUtils.equals(originalType, changedType)) {
            ChangeReturnTypeRefactoring refactoring = new ChangeReturnTypeRefactoring(removedOperation.getReturnType2(), addedOperation.getReturnType2(), oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        checkForOperationAnnotationChanges(oldEntity, newEntity, refactorings);
        checkForOperationModifierChanges(oldEntity, newEntity, refactorings);
        checkForThrownExceptionTypeChanges(oldEntity, newEntity, refactorings);
        checkForOperationParameterChanges(oldEntity, newEntity, refactorings);
    }

    private void processAttributes(boolean isMove, Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities, DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity,
                                   List<Refactoring> refactorings) {
        FieldDeclaration removedAttribute = (FieldDeclaration) oldEntity.getDeclaration();
        FieldDeclaration addedAttribute = (FieldDeclaration) newEntity.getDeclaration();
        if (isMove) {
            if (isSubTypeOf(matchedEntities, oldEntity, newEntity, 0)) {
                if (oldEntity.getName().equals(newEntity.getName())) {
                    PullUpAttributeRefactoring refactoring = new PullUpAttributeRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                } else {
                    MoveAndRenameAttributeRefactoring refactoring = new MoveAndRenameAttributeRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
            if (isSubTypeOf(matchedEntities, newEntity, oldEntity, 1)) {
                if (oldEntity.getName().equals(newEntity.getName())) {
                    PushDownAttributeRefactoring refactoring = new PushDownAttributeRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                } else {
                    MoveAndRenameAttributeRefactoring refactoring = new MoveAndRenameAttributeRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
            if (!isSubTypeOf(matchedEntities, oldEntity, newEntity, 0) && !isSubTypeOf(matchedEntities, newEntity, oldEntity, 1)) {
                if (!oldEntity.getName().equals(newEntity.getName())) {
                    MoveAndRenameAttributeRefactoring refactoring = new MoveAndRenameAttributeRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                } else {
                    MoveAttributeRefactoring refactoring = new MoveAttributeRefactoring(oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
        } else {
            if (!oldEntity.getName().equals(newEntity.getName())) {
                RenameAttributeRefactoring refactoring = new RenameAttributeRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
        String originalType = removedAttribute.getType().toString();
        String changedType = addedAttribute.getType().toString();
        if (!StringUtils.equals(originalType, changedType)) {
            ChangeAttributeTypeRefactoring refactoring = new ChangeAttributeTypeRefactoring(oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        checkForAttributeAnnotationChanges(oldEntity, newEntity, refactorings);
        checkForAttributeModifierChanges(oldEntity, newEntity, refactorings);
        List<MethodDeclaration> oldMethods = new ArrayList<>();
        List<MethodDeclaration> newMethods = new ArrayList<>();
        oldEntity.getDeclaration().accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                oldMethods.add(node);
                return true;
            }
        });
        newEntity.getDeclaration().accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                newMethods.add(node);
                return true;
            }
        });
        for (MethodDeclaration oldMethod : oldMethods) {
            for (MethodDeclaration newMethod : newMethods) {
                originalType = oldMethod.getReturnType2() == null ? "" : oldMethod.getReturnType2().toString();
                changedType = newMethod.getReturnType2() == null ? "" : newMethod.getReturnType2().toString();
                if (oldMethod.getName().getIdentifier().equals(newMethod.getName().getIdentifier()) &&
                        StringUtils.equals(originalType, changedType) &&
                        oldMethod.parameters().size() == newMethod.parameters().size() && equalsParameters(oldMethod, newMethod)) {
                    DeclarationNodeTree leftEntity = new LeafNode((CompilationUnit) oldMethod.getRoot(), oldEntity.getLocationInfo().getFilePath(), oldMethod);
                    DeclarationNodeTree rightEntity = new LeafNode((CompilationUnit) newMethod.getRoot(), newEntity.getLocationInfo().getFilePath(), newMethod);
                    leftEntity.setDeclaration(oldMethod);
                    leftEntity.setType(EntityType.METHOD);
                    leftEntity.setParent(new InternalNode((CompilationUnit) oldMethod.getRoot(), oldEntity.getLocationInfo().getFilePath(), oldMethod));
                    leftEntity.getParent().setType(EntityType.CLASS);
                    if (oldMethod.getParent() instanceof AnonymousClassDeclaration && oldMethod.getParent().getParent() instanceof ClassInstanceCreation)
                        leftEntity.setNamespace(oldEntity.getNamespace() + "." + oldEntity.getName() + ".new " +
                                ((ClassInstanceCreation) oldMethod.getParent().getParent()).getType().toString());
                    rightEntity.setDeclaration(newMethod);
                    rightEntity.setType(EntityType.METHOD);
                    rightEntity.setParent(new InternalNode((CompilationUnit) oldMethod.getRoot(), oldEntity.getLocationInfo().getFilePath(), oldMethod));
                    rightEntity.getParent().setType(EntityType.CLASS);
                    if (newMethod.getParent() instanceof AnonymousClassDeclaration && newMethod.getParent().getParent() instanceof ClassInstanceCreation)
                        rightEntity.setNamespace(newEntity.getNamespace() + "." + newEntity.getName() + ".new " +
                                ((ClassInstanceCreation) newMethod.getParent().getParent()).getType().toString());
                    checkForOperationAnnotationChanges(leftEntity, rightEntity, refactorings);
                    checkForOperationModifierChanges(leftEntity, rightEntity, refactorings);
                    checkForThrownExceptionTypeChanges(leftEntity, rightEntity, refactorings);
                }
            }
        }
    }

    private void processEnumConstants(boolean isMove, DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity,
                                      List<Refactoring> refactorings) {
        if (isMove) {
            if (!oldEntity.getName().equals(newEntity.getName())) {
                MoveAndRenameAttributeRefactoring refactoring = new MoveAndRenameAttributeRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            } else {
                MoveAttributeRefactoring refactoring = new MoveAttributeRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        } else {
            if (!oldEntity.getName().equals(newEntity.getName())) {
                RenameAttributeRefactoring refactoring = new RenameAttributeRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
        checkForEnumConstantAnnotationChanges(oldEntity, newEntity, refactorings);
    }

    private void processClasses(boolean isMove, DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity,
                                List<Refactoring> refactorings) {
        if (isMove) {
            if (!oldEntity.getName().equals(newEntity.getName())) {
                MoveAndRenameClassRefactoring refactoring = new MoveAndRenameClassRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            } else {
                MoveClassRefactoring refactoring = new MoveClassRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        } else {
            if (!oldEntity.getName().equals(newEntity.getName())) {
                RenameClassRefactoring refactoring = new RenameClassRefactoring(oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
        if (oldEntity.getType() != newEntity.getType()) {
            ChangeTypeDeclarationKindRefactoring refactoring = new ChangeTypeDeclarationKindRefactoring(oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        checkForClassAnnotationChanges(oldEntity, newEntity, refactorings);
        checkForClassModifierChanges(oldEntity, newEntity, refactorings);
    }

    private void checkForOperationAnnotationChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        MethodDeclaration removedOperation = (MethodDeclaration) oldEntity.getDeclaration();
        MethodDeclaration addedOperation = (MethodDeclaration) newEntity.getDeclaration();
        List<IExtendedModifier> modifiers1 = removedOperation.modifiers();
        List<IExtendedModifier> modifiers2 = addedOperation.modifiers();
        AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
        for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddMethodAnnotationRefactoring refactoring = new AddMethodAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveMethodAnnotationRefactoring refactoring = new RemoveMethodAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
            ModifyMethodAnnotationRefactoring refactoring = new ModifyMethodAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void checkForOperationModifierChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        MethodDeclaration removedOperation = (MethodDeclaration) oldEntity.getDeclaration();
        MethodDeclaration addedOperation = (MethodDeclaration) newEntity.getDeclaration();
        int methodModifiers1 = removedOperation.getModifiers();
        int methodModifiers2 = addedOperation.getModifiers();
        if (!Flags.isFinal(methodModifiers1) && Flags.isFinal(methodModifiers2)) {
            AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("final", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isFinal(methodModifiers1) && !Flags.isFinal(methodModifiers2)) {
            RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("final", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isAbstract(methodModifiers1) && Flags.isAbstract(methodModifiers2)) {
            AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("abstract", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isAbstract(methodModifiers1) && !Flags.isAbstract(methodModifiers2)) {
            RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("abstract", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isStatic(methodModifiers1) && Flags.isStatic(methodModifiers2)) {
            AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("static", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isStatic(methodModifiers1) && !Flags.isStatic(methodModifiers2)) {
            RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("static", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isSynchronized(methodModifiers1) && Flags.isSynchronized(methodModifiers2)) {
            AddMethodModifierRefactoring refactoring = new AddMethodModifierRefactoring("synchronized", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isSynchronized(methodModifiers1) && !Flags.isSynchronized(methodModifiers2)) {
            RemoveMethodModifierRefactoring refactoring = new RemoveMethodModifierRefactoring("synchronized", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        Visibility originalAccessModifier = getVisibility(methodModifiers1, oldEntity);
        Visibility changedAccessModifier = getVisibility(methodModifiers2, newEntity);
        if (originalAccessModifier != changedAccessModifier) {
            ChangeOperationAccessModifierRefactoring refactoring = new ChangeOperationAccessModifierRefactoring(originalAccessModifier, changedAccessModifier, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private Visibility getVisibility(int modifiers, DeclarationNodeTree entity) {
        Visibility visibility;
        if ((modifiers & Modifier.PUBLIC) != 0)
            visibility = Visibility.PUBLIC;
        else if ((modifiers & Modifier.PROTECTED) != 0)
            visibility = Visibility.PROTECTED;
        else if ((modifiers & Modifier.PRIVATE) != 0)
            visibility = Visibility.PRIVATE;
        else {
            if (entity instanceof LeafNode && entity.getParent().getType() == EntityType.INTERFACE)
                visibility = Visibility.PUBLIC;
            else
                visibility = Visibility.PACKAGE;
        }
        return visibility;
    }

    private void checkForThrownExceptionTypeChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        MethodDeclaration removedOperation = (MethodDeclaration) oldEntity.getDeclaration();
        MethodDeclaration addedOperation = (MethodDeclaration) newEntity.getDeclaration();
        List<Type> exceptionTypes1 = removedOperation.thrownExceptionTypes();
        List<Type> exceptionTypes2 = addedOperation.thrownExceptionTypes();
        Set<Type> addedExceptionTypes = new LinkedHashSet<>();
        Set<Type> removedExceptionTypes = new LinkedHashSet<>();
        for (Type exceptionType1 : exceptionTypes1) {
            boolean found = false;
            for (Type exceptionType2 : exceptionTypes2) {
                if (exceptionType1.toString().equals(exceptionType2.toString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                removedExceptionTypes.add(exceptionType1);
            }
        }
        for (Type exceptionType2 : exceptionTypes2) {
            boolean found = false;
            for (Type exceptionType1 : exceptionTypes1) {
                if (exceptionType1.toString().equals(exceptionType2.toString())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                addedExceptionTypes.add(exceptionType2);
            }
        }
        if (!removedExceptionTypes.isEmpty() && addedExceptionTypes.isEmpty()) {
            for (Type exceptionType : removedExceptionTypes) {
                RemoveThrownExceptionTypeRefactoring refactoring = new RemoveThrownExceptionTypeRefactoring(exceptionType, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
        if (!addedExceptionTypes.isEmpty() && removedExceptionTypes.isEmpty()) {
            for (Type exceptionType : addedExceptionTypes) {
                AddThrownExceptionTypeRefactoring refactoring = new AddThrownExceptionTypeRefactoring(exceptionType, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
        if (!removedExceptionTypes.isEmpty() && !addedExceptionTypes.isEmpty()) {
            ChangeThrownExceptionTypeRefactoring refactoring = new ChangeThrownExceptionTypeRefactoring(removedExceptionTypes, addedExceptionTypes, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void checkForOperationParameterChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity,
                                                   List<Refactoring> refactorings) {
        MethodDeclaration removedOperation = (MethodDeclaration) oldEntity.getDeclaration();
        MethodDeclaration addedOperation = (MethodDeclaration) newEntity.getDeclaration();
        List<SingleVariableDeclaration> addedParameters = new ArrayList<>();
        List<SingleVariableDeclaration> removedParameters = new ArrayList<>();
        List<AbstractMap.SimpleEntry<SingleVariableDeclaration, SingleVariableDeclaration>> matchedParameters = new ArrayList<>();
        List<Pair<SingleVariableDeclaration, SingleVariableDeclaration>> parameterDiffList = new ArrayList<>();
        for (Object obj1 : removedOperation.parameters()) {
            SingleVariableDeclaration parameter1 = (SingleVariableDeclaration) obj1;
            boolean found = false;
            for (Object obj2 : addedOperation.parameters()) {
                SingleVariableDeclaration parameter2 = (SingleVariableDeclaration) obj2;
                if (equalsIncludingName(parameter1, parameter2)) {
                    matchedParameters.add(new AbstractMap.SimpleEntry<>(parameter1, parameter2));
                    found = true;
                    break;
                }
            }
            if (!found) {
                removedParameters.add(parameter1);
            }
        }
        for (Object obj1 : addedOperation.parameters()) {
            SingleVariableDeclaration parameter1 = (SingleVariableDeclaration) obj1;
            boolean found = false;
            for (Object obj2 : removedOperation.parameters()) {
                SingleVariableDeclaration parameter2 = (SingleVariableDeclaration) obj2;
                if (equalsIncludingName(parameter1, parameter2)) {
                    matchedParameters.add(new AbstractMap.SimpleEntry<>(parameter2, parameter1));
                    found = true;
                    break;
                }
            }
            if (!found) {
                addedParameters.add(parameter1);
            }
        }
        for (AbstractMap.SimpleEntry<SingleVariableDeclaration, SingleVariableDeclaration> matchedParameter : matchedParameters) {
            SingleVariableDeclaration parameter1 = matchedParameter.getKey();
            SingleVariableDeclaration parameter2 = matchedParameter.getValue();
            parameterDiffList.add(Pair.of(parameter1, parameter2));
        }
        int matchedParameterCount = matchedParameters.size() / 2;
        List<String> parameterNames1 = new ArrayList<>();
        List<SingleVariableDeclaration> parameters1 = removedOperation.parameters();
        for (SingleVariableDeclaration singleVariableDeclaration : parameters1) {
            String identifier = singleVariableDeclaration.getName().getIdentifier();
            parameterNames1.add(identifier);
        }
        for (SingleVariableDeclaration removedParameter : removedParameters) {
            parameterNames1.remove(removedParameter.getName().getIdentifier());
        }
        List<String> parameterNames2 = new ArrayList<>();
        List<SingleVariableDeclaration> parameters2 = addedOperation.parameters();
        for (SingleVariableDeclaration parameter : parameters2) {
            String identifier = parameter.getName().getIdentifier();
            parameterNames2.add(identifier);
        }
        for (SingleVariableDeclaration addedParameter : addedParameters) {
            parameterNames2.remove(addedParameter.getName().getIdentifier());
        }
        if (matchedParameterCount == parameterNames1.size() && matchedParameterCount == parameterNames2.size() &&
                parameterNames1.size() > 1 && !parameterNames1.equals(parameterNames2)) {
            ReorderParameterRefactoring refactoring = new ReorderParameterRefactoring(oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        //first round match parameters with the same name
        for (Iterator<SingleVariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext(); ) {
            SingleVariableDeclaration removedParameter = removedParameterIterator.next();
            for (Iterator<SingleVariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext(); ) {
                SingleVariableDeclaration addedParameter = addedParameterIterator.next();
                if (removedParameter.getName().getIdentifier().equals(addedParameter.getName().getIdentifier())) {
                    parameterDiffList.add(Pair.of(removedParameter, addedParameter));
                    addedParameterIterator.remove();
                    removedParameterIterator.remove();
                    break;
                }
            }
        }
        //second round match parameters with the same type
        for (Iterator<SingleVariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext(); ) {
            SingleVariableDeclaration removedParameter = removedParameterIterator.next();
            for (Iterator<SingleVariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext(); ) {
                SingleVariableDeclaration addedParameter = addedParameterIterator.next();
                if (removedParameter.getType().toString().equals(addedParameter.getType().toString()) &&
                        !existsAnotherAddedParameterWithTheSameType(removedOperation, addedOperation, addedParameters, addedParameter)) {
                    parameterDiffList.add(Pair.of(removedParameter, addedParameter));
                    addedParameterIterator.remove();
                    removedParameterIterator.remove();
                    break;
                }
            }
        }
        //third round match parameters with different type and name
        List<SingleVariableDeclaration> removedParametersWithoutReturnType = removedOperation.parameters();
        List<SingleVariableDeclaration> addedParametersWithoutReturnType = addedOperation.parameters();
        if (matchedParameterCount == removedParametersWithoutReturnType.size() - 1 && matchedParameterCount == addedParametersWithoutReturnType.size() - 1) {
            for (Iterator<SingleVariableDeclaration> removedParameterIterator = removedParameters.iterator(); removedParameterIterator.hasNext(); ) {
                SingleVariableDeclaration removedParameter = removedParameterIterator.next();
                int indexOfRemovedParameter = indexOfParameter(removedParametersWithoutReturnType, removedParameter);
                for (Iterator<SingleVariableDeclaration> addedParameterIterator = addedParameters.iterator(); addedParameterIterator.hasNext(); ) {
                    SingleVariableDeclaration addedParameter = addedParameterIterator.next();
                    int indexOfAddedParameter = indexOfParameter(addedParametersWithoutReturnType, addedParameter);
                    if (indexOfRemovedParameter == indexOfAddedParameter &&
                            usedParameters(removedOperation, addedOperation, removedParameter, addedParameter)) {
                        parameterDiffList.add(Pair.of(removedParameter, addedParameter));
                        addedParameterIterator.remove();
                        removedParameterIterator.remove();
                        break;
                    }
                }
            }
        }

        //fourth round match parameters with the same type
        if (removedParameters.size() == addedParameters.size()) {
            boolean sameType = true;
            for (int i = 0; i < removedParameters.size(); i++) {
                SingleVariableDeclaration removedParameter = removedParameters.get(i);
                SingleVariableDeclaration addedParameter = addedParameters.get(i);
                if (!removedParameter.getType().toString().equals(addedParameter.getType().toString())) {
                    sameType = false;
                    break;
                }
            }
            if (sameType) {
                for (int i = 0; i < removedParameters.size(); i++) {
                    SingleVariableDeclaration removedParameter = removedParameters.get(i);
                    SingleVariableDeclaration addedParameter = addedParameters.get(i);
                    parameterDiffList.add(Pair.of(removedParameter, addedParameter));
                }
                removedParameters.clear();
                addedParameters.clear();
            }
        }
        getParameterRefactorings(parameterDiffList, addedParameters, removedParameters, oldEntity, newEntity, refactorings);
    }

    private void getParameterRefactorings(List<Pair<SingleVariableDeclaration, SingleVariableDeclaration>> parameterDiffList,
                                          List<SingleVariableDeclaration> addedParameters, List<SingleVariableDeclaration> removedParameters,
                                          DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        for (Pair<SingleVariableDeclaration, SingleVariableDeclaration> parameterDiff : parameterDiffList) {
            SingleVariableDeclaration parameter1 = parameterDiff.getLeft();
            SingleVariableDeclaration parameter2 = parameterDiff.getRight();
            if (!parameter1.getName().getIdentifier().equals(parameter2.getName().getIdentifier())) {
                RenameParameterRefactoring refactoring = new RenameParameterRefactoring(parameter1, parameter2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (!parameter1.getType().toString().equals(parameter2.getType().toString())) {
                ChangeParameterTypeRefactoring refactoring = new ChangeParameterTypeRefactoring(parameter1, parameter2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            int parameterModifiers1 = parameter1.getModifiers();
            int parameterModifiers2 = parameter2.getModifiers();
            List<IExtendedModifier> modifiers1 = parameter1.modifiers();
            List<IExtendedModifier> modifiers2 = parameter2.modifiers();
            AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
            for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
                AddParameterAnnotationRefactoring refactoring = new AddParameterAnnotationRefactoring(annotation, parameter1, parameter2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
                RemoveParameterAnnotationRefactoring refactoring = new RemoveParameterAnnotationRefactoring(annotation, parameter1, parameter2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
                ModifyParameterAnnotationRefactoring refactoring = new ModifyParameterAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), parameter1, parameter2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (!Flags.isFinal(parameterModifiers1) && Flags.isFinal(parameterModifiers2)) {
                AddParameterModifierRefactoring refactoring = new AddParameterModifierRefactoring("final", parameter1, parameter2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (Flags.isFinal(parameterModifiers1) && !Flags.isFinal(parameterModifiers2)) {
                RemoveParameterModifierRefactoring refactoring = new RemoveParameterModifierRefactoring("final", parameter1, parameter2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
        for (SingleVariableDeclaration parameter : addedParameters) {
            AddParameterRefactoring refactoring = new AddParameterRefactoring(parameter, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (SingleVariableDeclaration parameter : removedParameters) {
            RemoveParameterRefactoring refactoring = new RemoveParameterRefactoring(parameter, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private boolean usedParameters(MethodDeclaration removedOperation, MethodDeclaration addedOperation,
                                   SingleVariableDeclaration removedParameter, SingleVariableDeclaration addedParameter) {
        List<String> removedOperationVariables = getAllVariables(removedOperation);
        List<String> addedOperationVariables = getAllVariables(addedOperation);
        if (removedOperationVariables.contains(removedParameter.getName().getIdentifier()) ==
                addedOperationVariables.contains(addedParameter.getName().getIdentifier())) {
            if (!removedOperation.isConstructor() && !addedOperation.isConstructor()) {
                return !removedOperationVariables.contains(addedParameter.getName().getIdentifier()) &&
                        !addedOperationVariables.contains(removedParameter.getName().getIdentifier());
            } else {
                return true;
            }
        }
        return false;
    }

    private List<String> getAllVariables(MethodDeclaration methodDeclaration) {
        List<String> operationVariables = new ArrayList<>();
        methodDeclaration.accept(new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationFragment node) {
                operationVariables.add(node.getName().getIdentifier());
                return true;
            }
        });
        return operationVariables;
    }

    private int indexOfParameter(List<SingleVariableDeclaration> parameters, SingleVariableDeclaration parameter) {
        int index = 0;
        for (SingleVariableDeclaration p : parameters) {
            if (equalsIncludingName(p, parameter)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private boolean existsAnotherAddedParameterWithTheSameType(MethodDeclaration removedOperation, MethodDeclaration addedOperation,
                                                               List<SingleVariableDeclaration> addedParameters, SingleVariableDeclaration parameter) {
        if (hasTwoParametersWithTheSameType(removedOperation) && hasTwoParametersWithTheSameType(addedOperation)) {
            return false;
        }
        for (SingleVariableDeclaration addedParameter : addedParameters) {
            if (!addedParameter.getName().equals(parameter.getName()) &&
                    addedParameter.getType().toString().equals(parameter.getType().toString())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTwoParametersWithTheSameType(MethodDeclaration methodDeclaration) {
        List<SingleVariableDeclaration> parameterTypes = methodDeclaration.parameters();
        if (parameterTypes.size() == 2) {
            if (parameterTypes.get(0).getType().toString().equals(parameterTypes.get(1).getType().toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean equalsIncludingName(SingleVariableDeclaration parameter1, SingleVariableDeclaration parameter2) {
        return parameter1.getName().getIdentifier().equals(parameter2.getName().getIdentifier()) &&
                parameter1.getType().toString().equals(parameter2.getType().toString()) &&
                parameter1.isVarargs() == parameter2.isVarargs();
    }

    private void checkForAttributeAnnotationChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        FieldDeclaration removedAttribute = (FieldDeclaration) oldEntity.getDeclaration();
        FieldDeclaration addedAttribute = (FieldDeclaration) newEntity.getDeclaration();
        List<IExtendedModifier> modifiers1 = removedAttribute.modifiers();
        List<IExtendedModifier> modifiers2 = addedAttribute.modifiers();
        AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
        for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddAttributeAnnotationRefactoring refactoring = new AddAttributeAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveAttributeAnnotationRefactoring refactoring = new RemoveAttributeAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
            ModifyAttributeAnnotationRefactoring refactoring = new ModifyAttributeAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void checkForAttributeModifierChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        FieldDeclaration removedAttribute = (FieldDeclaration) oldEntity.getDeclaration();
        FieldDeclaration addedAttribute = (FieldDeclaration) newEntity.getDeclaration();
        int attributeModifiers1 = removedAttribute.getModifiers();
        int attributeModifiers2 = addedAttribute.getModifiers();
        if (!Flags.isFinal(attributeModifiers1) && Flags.isFinal(attributeModifiers2)) {
            AddAttributeModifierRefactoring refactoring = new AddAttributeModifierRefactoring("final", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isFinal(attributeModifiers1) && !Flags.isFinal(attributeModifiers2)) {
            RemoveAttributeModifierRefactoring refactoring = new RemoveAttributeModifierRefactoring("final", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isStatic(attributeModifiers1) && Flags.isStatic(attributeModifiers2)) {
            AddAttributeModifierRefactoring refactoring = new AddAttributeModifierRefactoring("static", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isStatic(attributeModifiers1) && !Flags.isStatic(attributeModifiers2)) {
            RemoveAttributeModifierRefactoring refactoring = new RemoveAttributeModifierRefactoring("static", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isTransient(attributeModifiers1) && Flags.isTransient(attributeModifiers2)) {
            AddAttributeModifierRefactoring refactoring = new AddAttributeModifierRefactoring("transient", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isTransient(attributeModifiers1) && !Flags.isTransient(attributeModifiers2)) {
            RemoveAttributeModifierRefactoring refactoring = new RemoveAttributeModifierRefactoring("transient", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isVolatile(attributeModifiers1) && Flags.isVolatile(attributeModifiers2)) {
            AddAttributeModifierRefactoring refactoring = new AddAttributeModifierRefactoring("volatile", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isVolatile(attributeModifiers1) && !Flags.isVolatile(attributeModifiers2)) {
            RemoveAttributeModifierRefactoring refactoring = new RemoveAttributeModifierRefactoring("volatile", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        Visibility originalAccessModifier = getVisibility(attributeModifiers1, oldEntity);
        Visibility changedAccessModifier = getVisibility(attributeModifiers2, newEntity);
        if (originalAccessModifier != changedAccessModifier) {
            ChangeAttributeAccessModifierRefactoring refactoring = new ChangeAttributeAccessModifierRefactoring(originalAccessModifier, changedAccessModifier, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void checkForEnumConstantAnnotationChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        EnumConstantDeclaration removedAttribute = (EnumConstantDeclaration) oldEntity.getDeclaration();
        EnumConstantDeclaration addedAttribute = (EnumConstantDeclaration) newEntity.getDeclaration();
        List<IExtendedModifier> modifiers1 = removedAttribute.modifiers();
        List<IExtendedModifier> modifiers2 = addedAttribute.modifiers();
        AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
        for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddAttributeAnnotationRefactoring refactoring = new AddAttributeAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveAttributeAnnotationRefactoring refactoring = new RemoveAttributeAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
            ModifyAttributeAnnotationRefactoring refactoring = new ModifyAttributeAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void checkForClassAnnotationChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        AbstractTypeDeclaration removedClass = (AbstractTypeDeclaration) oldEntity.getDeclaration();
        AbstractTypeDeclaration addedClass = (AbstractTypeDeclaration) newEntity.getDeclaration();
        List<IExtendedModifier> modifiers1 = removedClass.modifiers();
        List<IExtendedModifier> modifiers2 = addedClass.modifiers();
        AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
        for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddClassAnnotationRefactoring refactoring = new AddClassAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveClassAnnotationRefactoring refactoring = new RemoveClassAnnotationRefactoring(annotation, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
            ModifyClassAnnotationRefactoring refactoring = new ModifyClassAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void checkForClassModifierChanges(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        AbstractTypeDeclaration removedClass = (AbstractTypeDeclaration) oldEntity.getDeclaration();
        AbstractTypeDeclaration addedClass = (AbstractTypeDeclaration) newEntity.getDeclaration();
        int methodModifiers1 = removedClass.getModifiers();
        int methodModifiers2 = addedClass.getModifiers();
        if (!Flags.isAbstract(methodModifiers1) && Flags.isAbstract(methodModifiers2)) {
            AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("abstract", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isAbstract(methodModifiers1) && !Flags.isAbstract(methodModifiers2)) {
            RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("abstract", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isFinal(methodModifiers1) && Flags.isFinal(methodModifiers2)) {
            AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("final", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isFinal(methodModifiers1) && !Flags.isFinal(methodModifiers2)) {
            RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("final", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!Flags.isStatic(methodModifiers1) && Flags.isStatic(methodModifiers2)) {
            AddClassModifierRefactoring refactoring = new AddClassModifierRefactoring("static", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isStatic(methodModifiers1) && !Flags.isStatic(methodModifiers2)) {
            RemoveClassModifierRefactoring refactoring = new RemoveClassModifierRefactoring("static", oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        Visibility originalAccessModifier = getVisibility(methodModifiers1, oldEntity);
        Visibility changedAccessModifier = getVisibility(methodModifiers2, newEntity);
        if (originalAccessModifier != changedAccessModifier) {
            ChangeClassAccessModifierRefactoring refactoring = new ChangeClassAccessModifierRefactoring(originalAccessModifier, changedAccessModifier, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void detectRefactoringsBetweenMatchedAndAddedEntities(MatchPair matchPair,
                                                                  Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities,
                                                                  Set<DeclarationNodeTree> addedEntities,
                                                                  Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                                                  List<Refactoring> refactorings) {
        for (DeclarationNodeTree extractedEntity : addedEntities) {
            if (extractedEntity.getType() != EntityType.METHOD)
                continue;
            List<EntityInfo> dependencies = extractedEntity.getDependencies();
            for (Pair<DeclarationNodeTree, DeclarationNodeTree> pair : matchedEntities) {
                DeclarationNodeTree oldEntity = pair.getLeft();
                DeclarationNodeTree newEntity = pair.getRight();
                if (oldEntity.getType() == EntityType.METHOD && newEntity.getType() == EntityType.METHOD) {
                    if (!dependencies.contains(newEntity.getEntity()) && !hasMethodInvocation(newEntity, extractedEntity))
                        continue;
                    if (MethodUtils.isNewFunction(oldEntity.getDeclaration(), newEntity.getDeclaration()))
                        continue;
                    if (MethodUtils.isGetter((MethodDeclaration) extractedEntity.getDeclaration()) || MethodUtils.isSetter((MethodDeclaration) extractedEntity.getDeclaration()))
                        continue;
                    boolean isExtracted = isExtractedFromStatement(oldEntity, newEntity, extractedEntity, matchedStatements, refactorings);
                    double dice = DiceFunction.calculateBodyDice((LeafNode) oldEntity, (LeafNode) newEntity, (LeafNode) extractedEntity);
                    if (dice >= 0.15 || isExtracted ||
                            matchedLOCAreGreaterThanUnmatchedLOC(oldEntity, newEntity, extractedEntity, true, matchedStatements)) {
                        boolean isMove = !oldEntity.getNamespace().equals(extractedEntity.getNamespace()) &&
                                !matchedEntities.contains(Pair.of(oldEntity.getParent(), extractedEntity.getParent()));
                        double ref1 = DiceFunction.calculateReferenceSimilarity(matchPair, oldEntity, newEntity);
                        double ref2 = DiceFunction.calculateReferenceSimilarity(matchPair, oldEntity, extractedEntity);
                        double dice1 = DiceFunction.calculateDiceSimilarity((LeafNode) oldEntity, (LeafNode) newEntity);
                        double dice2 = DiceFunction.calculateDiceSimilarity((LeafNode) oldEntity, (LeafNode) extractedEntity);
                        MethodDeclaration declaration = (MethodDeclaration) newEntity.getDeclaration();
                        boolean isDeprecated = false;
                        List<IExtendedModifier> modifiers = declaration.modifiers();
                        for (IExtendedModifier modifier : modifiers) {
                            if (modifier.isAnnotation() && modifier.toString().equals("@Deprecated")) {
                                isDeprecated = true;
                                break;
                            }
                        }
                        if (isMove) {
                            if (ref2 > ref1 && dice2 > dice1 && isDeprecated) {
                                MoveAndRenameOperationRefactoring refactoring = new MoveAndRenameOperationRefactoring(oldEntity, newEntity);
                                refactorings.add(refactoring);
                            } else {
                                ExtractAndMoveOperationRefactoring refactoring = new ExtractAndMoveOperationRefactoring(oldEntity, newEntity, extractedEntity, matchedStatements);
                                refactorings.add(refactoring);
                            }
                        } else {
                            if (ref2 > ref1 && dice2 > dice1 && isDeprecated) {
                                RenameOperationRefactoring refactoring = new RenameOperationRefactoring(oldEntity, newEntity);
                                refactorings.add(refactoring);
                            } else {
                                ExtractOperationRefactoring refactoring = new ExtractOperationRefactoring(oldEntity, newEntity, extractedEntity, matchedStatements);
                                refactorings.add(refactoring);
                            }
                        }
                    } else {
                        MethodNode extractedMethodNode = extractedEntity.getMethodNode();
                        List<StatementNodeTree> allOperations = extractedMethodNode.getAllOperations();
                        List<StatementNodeTree> allControls = extractedMethodNode.getAllControls();
                        List<StatementNodeTree> allBlocks = extractedMethodNode.getAllBlocks();
                        if (allOperations.size() == 1 && allControls.isEmpty() && allBlocks.size() == 1 &&
                                allOperations.get(0).getType() == StatementType.RETURN_STATEMENT && allOperations.get(0).getExpression().startsWith("return ")) {
                            isExtracted = false;
                            for (Pair<StatementNodeTree, StatementNodeTree> pair2 : matchedStatements) {
                                StatementNodeTree oldStatement = pair2.getLeft();
                                StatementNodeTree newStatement = pair2.getRight();
                                if (oldStatement.getRoot().getMethodEntity() == oldEntity && newStatement.getRoot().getMethodEntity() == newEntity) {
                                    MethodNode newMethodNode = newEntity.getMethodNode();
                                    List<StatementNodeTree> operations = newMethodNode.getAllOperations();
                                    List<StatementNodeTree> controls = newMethodNode.getAllControls();
                                    List<StatementNodeTree> locations = new ArrayList<>();
                                    findMethodInvocation(operations, controls, extractedEntity, locations);
                                    List<MethodInvocation> invocations = new ArrayList<>();
                                    for (StatementNodeTree location : locations) {
                                        location.getStatement().accept(new ASTVisitor() {
                                            @Override
                                            public boolean visit(MethodInvocation node) {
                                                if (node.getName().getIdentifier().equals(extractedEntity.getName())) {
                                                    invocations.add(node);
                                                }
                                                return true;
                                            }
                                        });
                                    }
                                    for (MethodInvocation invocation : invocations) {
                                        List<Expression> arguments = invocation.arguments();
                                        MethodDeclaration declaration = (MethodDeclaration) extractedEntity.getDeclaration();
                                        List<SingleVariableDeclaration> parameters = declaration.parameters();
                                        String operation = allOperations.get(0).getExpression().substring("return ".length());
                                        if (operation.endsWith(";\n"))
                                            operation = operation.substring(0, operation.length() - 2);
                                        if (arguments.size() == parameters.size()) {
                                            for (int i = 0; i < arguments.size(); i++) {
                                                String argument = arguments.get(i).toString();
                                                String parameter = parameters.get(i).getName().getIdentifier();
                                                operation = operation.replace(parameter, argument);
                                            }
                                        }
                                        if (newStatement.getExpression().contains(invocation.toString()) && !oldStatement.getExpression().equals(newStatement.getExpression()) &&
                                                oldStatement.getExpression().equals(newStatement.getExpression().replace(invocation.toString(), operation))) {
                                            isExtracted = true;
                                            break;
                                        }
                                        ReturnStatement statement = (ReturnStatement) allOperations.get(0).getStatement();
                                        if (statement.getExpression() instanceof ClassInstanceCreation) {
                                            ClassInstanceCreation creation = (ClassInstanceCreation) statement.getExpression();
                                            if (!creation.getType().toString().equals(declaration.getReturnType2().toString())) {
                                                operation = creation.toString().replace(creation.getType().toString(), declaration.getReturnType2().toString());
                                                Map<String, String> replacements = new HashMap<>();
                                                for (Pair<DeclarationNodeTree, DeclarationNodeTree> pair3 : matchedEntities) {
                                                    DeclarationNodeTree left = pair3.getLeft();
                                                    DeclarationNodeTree right = pair3.getRight();
                                                    if (left.getType() == EntityType.CLASS && right.getType() == EntityType.CLASS)
                                                        if (!left.getName().equals(right.getName()))
                                                            replacements.put(left.getName(), right.getName());
                                                }
                                                boolean isSame = false;
                                                for (String key : replacements.keySet()) {
                                                    if (newStatement.getExpression().contains(invocation.toString()) && !oldStatement.getExpression().equals(newStatement.getExpression())) {
                                                        String replace1 = oldStatement.getExpression().replace(key, replacements.get(key));
                                                        String replace2 = newStatement.getExpression().replace(invocation.toString(), operation);
                                                        if (replace1.equals(replace2)) {
                                                            isExtracted = true;
                                                            isSame = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (isSame)
                                                    break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (!isExtracted)
                                continue;
                            boolean isMove = !oldEntity.getNamespace().equals(extractedEntity.getNamespace()) &&
                                    !matchedEntities.contains(Pair.of(oldEntity.getParent(), extractedEntity.getParent()));
                            if (isMove) {
                                ExtractAndMoveOperationRefactoring refactoring = new ExtractAndMoveOperationRefactoring(oldEntity, newEntity, extractedEntity, matchedStatements);
                                refactorings.add(refactoring);
                            } else {
                                ExtractOperationRefactoring refactoring = new ExtractOperationRefactoring(oldEntity, newEntity, extractedEntity, matchedStatements);
                                refactorings.add(refactoring);
                            }
                        }
                    }
                }
            }
        }
        for (DeclarationNodeTree addedEntity : addedEntities) {
            if (addedEntity.getType() == EntityType.CLASS || addedEntity.getType() == EntityType.INTERFACE) {
                Set<DeclarationNodeTree> mapping = new HashSet<>();
                Map<DeclarationNodeTree, DeclarationNodeTree> extractedOperations = new TreeMap<>(Comparator.comparingInt(startLine -> startLine.getLocationInfo().getStartLine()));
                Map<DeclarationNodeTree, DeclarationNodeTree> extractedAttributes = new TreeMap<>(Comparator.comparingInt(startLine -> startLine.getLocationInfo().getStartLine()));
                for (Pair<DeclarationNodeTree, DeclarationNodeTree> pair : matchedEntities) {
                    DeclarationNodeTree oldEntity = pair.getLeft();
                    DeclarationNodeTree newEntity = pair.getRight();
                    if (oldEntity.getType() == EntityType.METHOD && newEntity.getType() == EntityType.METHOD) {
                        if (newEntity.getParent() == addedEntity) {
                            MethodDeclaration declaration1 = (MethodDeclaration) oldEntity.getDeclaration();
                            MethodDeclaration declaration2 = (MethodDeclaration) newEntity.getDeclaration();
                            if (!declaration1.isConstructor() && !declaration2.isConstructor()) {
                                mapping.add(oldEntity.getParent());
                                extractedOperations.put(oldEntity, newEntity);
                            }
                        } else {
                            if (newEntity.getParent().getType() == EntityType.CLASS || newEntity.getParent().getType() == EntityType.INTERFACE) {
                                TypeDeclaration newClass = (TypeDeclaration) newEntity.getParent().getDeclaration();
                                TypeDeclaration addedClass = (TypeDeclaration) addedEntity.getDeclaration();
                                if (isSubTypeOf(newClass, addedClass)) {
                                    List<DeclarationNodeTree> children = addedEntity.getChildren();
                                    for (DeclarationNodeTree child : children) {
                                        if (child.getType() == newEntity.getType() && child.getName().equals(newEntity.getName()) &&
                                                isSameSignature(child, newEntity)) {
                                            mapping.add(oldEntity.getParent());
                                            extractedOperations.put(oldEntity, child);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (oldEntity.getType() == EntityType.FIELD && newEntity.getType() == EntityType.FIELD && newEntity.getParent() == addedEntity) {
                        mapping.add(oldEntity.getParent());
                        extractedAttributes.put(oldEntity, newEntity);
                    }
                }
                Set<DeclarationNodeTree> subclassSetBefore = new LinkedHashSet<>();
                Set<DeclarationNodeTree> subclassSetAfter = new LinkedHashSet<>();
                for (Pair<DeclarationNodeTree, DeclarationNodeTree> pair : matchedEntities) {
                    DeclarationNodeTree oldEntity = pair.getLeft();
                    if (mapping.contains(oldEntity)) {
                        DeclarationNodeTree newEntity = pair.getRight();
                        if (newEntity.getType() == EntityType.CLASS || newEntity.getType() == EntityType.INTERFACE) {
                            TypeDeclaration newClass = (TypeDeclaration) newEntity.getDeclaration();
                            TypeDeclaration addedClass = (TypeDeclaration) addedEntity.getDeclaration();
                            if (!isSubTypeOf(newClass, addedClass) && !isSubTypeOf(addedClass, newClass)) {
                                ExtractClassRefactoring refactoring = new ExtractClassRefactoring(oldEntity, newEntity, addedEntity, extractedOperations, extractedAttributes);
                                refactorings.add(refactoring);
                            }
                            if (isSubTypeOf(newClass, addedClass)) {
                                subclassSetBefore.add(oldEntity);
                                subclassSetAfter.add(newEntity);
                            } else if (isSubTypeOf(addedClass, newClass)) {
                                ExtractSubClassRefactoring refactoring = new ExtractSubClassRefactoring(oldEntity, newEntity, addedEntity, extractedOperations, extractedAttributes);
                                refactorings.add(refactoring);
                            }
                        }
                    }
                }
                if (!subclassSetBefore.isEmpty()) {
                    if (addedEntity.getType() == EntityType.INTERFACE) {
                        ExtractInterfaceRefactoring refactoring = new ExtractInterfaceRefactoring(addedEntity, subclassSetBefore, subclassSetAfter);
                        refactorings.add(refactoring);
                    } else {
                        ExtractSuperClassRefactoring refactoring = new ExtractSuperClassRefactoring(addedEntity, subclassSetBefore, subclassSetAfter);
                        refactorings.add(refactoring);
                    }
                }
            }
        }
    }

    private boolean isSameSignature(DeclarationNodeTree dntBefore, DeclarationNodeTree dntCurrent) {
        if (dntBefore.getType() == EntityType.METHOD && dntCurrent.getType() == EntityType.METHOD) {
            MethodDeclaration md1 = ((MethodDeclaration) dntBefore.getDeclaration());
            MethodDeclaration md2 = ((MethodDeclaration) dntCurrent.getDeclaration());
            String pl1 = ((List<SingleVariableDeclaration>) md1.parameters()).stream().
                    map(declaration -> declaration.isVarargs() ? declaration.getType().toString() + "[]" : declaration.getType().toString()).
                    collect(Collectors.joining(","));
            String pl2 = ((List<SingleVariableDeclaration>) md2.parameters()).stream().
                    map(declaration -> declaration.isVarargs() ? declaration.getType().toString() + "[]" : declaration.getType().toString()).
                    collect(Collectors.joining(","));
            String tp1 = ((List<TypeParameter>) md1.typeParameters()).stream().
                    map(TypeParameter::toString).
                    collect(Collectors.joining(","));
            String tp2 = ((List<TypeParameter>) md2.typeParameters()).stream().
                    map(TypeParameter::toString).
                    collect(Collectors.joining(","));
            if (org.remapper.util.StringUtils.equals(md1.getName().getIdentifier(), md2.getName().getIdentifier()) &&
                    md1.getReturnType2() != null && md2.getReturnType2() != null &&
                    org.remapper.util.StringUtils.equals(md1.getReturnType2().toString(), md2.getReturnType2().toString()) &&
                    org.remapper.util.StringUtils.equals(pl1, pl2) && org.remapper.util.StringUtils.equals(tp1, tp2)) {
                return true;
            }
        }
        return false;
    }

    private void findMethodInvocation(List<StatementNodeTree> allOperations, List<StatementNodeTree> allControls,
                                      DeclarationNodeTree dnt, List<StatementNodeTree> locations) {
        MethodDeclaration declaration2 = (MethodDeclaration) dnt.getDeclaration();
        for (StatementNodeTree snt : allOperations) {
            ASTNode statement = snt.getStatement();
            if (hasMethodInvocation(statement, declaration2)) {
                locations.add(snt);
            }
        }
        for (StatementNodeTree snt : allControls) {
            if (snt.getType() == StatementType.DO_STATEMENT) {
                DoStatement statement = (DoStatement) snt.getStatement();
                if (hasMethodInvocation(statement.getExpression(), declaration2)) {
                    locations.add(snt);
                }
            } else if (snt.getType() == StatementType.WHILE_STATEMENT) {
                WhileStatement statement = (WhileStatement) snt.getStatement();
                if (hasMethodInvocation(statement.getExpression(), declaration2)) {
                    locations.add(snt);
                }
            } else if (snt.getType() == StatementType.FOR_STATEMENT) {
                ForStatement statement = (ForStatement) snt.getStatement();
                if (hasMethodInvocation(statement.getExpression(), declaration2)) {
                    locations.add(snt);
                }
            } else if (snt.getType() == StatementType.ENHANCED_FOR_STATEMENT) {
                EnhancedForStatement statement = (EnhancedForStatement) snt.getStatement();
                if (hasMethodInvocation(statement.getExpression(), declaration2)) {
                    locations.add(snt);
                }
            } else if (snt.getType() == StatementType.IF_STATEMENT) {
                IfStatement statement = (IfStatement) snt.getStatement();
                if (hasMethodInvocation(statement.getExpression(), declaration2)) {
                    locations.add(snt);
                }
            } else if (snt.getType() == StatementType.SWITCH_STATEMENT) {
                SwitchStatement statement = (SwitchStatement) snt.getStatement();
                if (hasMethodInvocation(statement.getExpression(), declaration2)) {
                    locations.add(snt);
                }
            } else if (snt.getType() == StatementType.TRY_STATEMENT) {
                TryStatement statement = (TryStatement) snt.getStatement();
                List<Expression> resources = statement.resources();
                for (Expression expression : resources) {
                    if (hasMethodInvocation(expression, declaration2)) {
                        locations.add(snt);
                    }
                }
            }
        }
    }

    private boolean hasMethodInvocation(ASTNode statement, MethodDeclaration declaration2) {
        if (statement == null) return false;
        List<MethodInvocation> invocations = new ArrayList<>();
        statement.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                if (node.getName().getIdentifier().equals(declaration2.getName().getIdentifier()) &&
                        node.arguments().size() == declaration2.parameters().size())
                    invocations.add(node);
                return true;
            }
        });
        return !invocations.isEmpty();
    }

    private boolean isExtractedFromStatement(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, DeclarationNodeTree extractedEntity,
                                             Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements, List<Refactoring> refactorings) {
        MethodNode methodNode = extractedEntity.getMethodNode();
        MethodDeclaration declaration = (MethodDeclaration) extractedEntity.getDeclaration();
        List<StatementNodeTree> allOperations = methodNode.getAllOperations();
        List<StatementNodeTree> allControls = methodNode.getAllControls();
        List<StatementNodeTree> allBlocks = methodNode.getAllBlocks();
        Set<Pair<StatementNodeTree, StatementNodeTree>> pairs = new HashSet<>();
        for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
            StatementNodeTree oldStatement = pair.getLeft();
            StatementNodeTree newStatement = pair.getRight();
            if (oldStatement.getRoot().getMethodEntity() == oldEntity && newStatement.getRoot().getMethodEntity() == newEntity &&
                    !oldStatement.getExpression().equals(newStatement.getExpression()) && contains(newStatement, extractedEntity.getName())) {
                pairs.add(pair);
            }
        }
        if (!(allOperations.size() == 1 && allBlocks.size() == 1 && allControls.isEmpty())) {
            for (Pair<StatementNodeTree, StatementNodeTree> pair : pairs) {
                StatementNodeTree oldStatement = pair.getLeft();
                StatementNodeTree newStatement = pair.getRight();
                List<MethodInvocation> list = new ArrayList<>();
                newStatement.getStatement().accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        if (node.getName().getIdentifier().equals(extractedEntity.getName()) && node.arguments().size() == declaration.parameters().size())
                            list.add(node);
                        return true;
                    }
                });
                if (list.isEmpty())
                    continue;
                Map<String, String> replacements = new HashMap();
                for (MethodInvocation invocation : list) {
                    List<Expression> arguments = invocation.arguments();
                    List<SingleVariableDeclaration> parameters = declaration.parameters();
                    for (int i = 0; i < arguments.size(); i++) {
                        Expression argument = arguments.get(i);
                        SingleVariableDeclaration parameter = parameters.get(i);
                        if (!argument.toString().equals(parameter.getName().getIdentifier()))
                            replacements.put(parameter.getName().getIdentifier(), argument.toString());
                    }
                }
                for (StatementNodeTree operation : allOperations) {
                    String expression = operation.getExpression();
                    if (oldStatement.getExpression().equals(expression))
                        return true;
                    for (String key : replacements.keySet()) {
                        expression = replaceLast(expression, key, replacements.get(key));
                    }
                    if (oldStatement.getExpression().equals(expression))
                        return true;
                }
            }
            return false;
        }
        StatementNodeTree operation = allOperations.get(0);
        if (operation.getType() != StatementType.RETURN_STATEMENT)
            return false;
        ReturnStatement statement = (ReturnStatement) operation.getStatement();
        Expression expression = statement.getExpression();

        MethodNode oldMethodNode = oldEntity.getMethodNode();
        MethodNode newMethodNode = newEntity.getMethodNode();
        Map<String, String> oldVariableDeclaration = new HashMap<>();
        Map<String, String> newVariableDeclaration = new HashMap<>();
        List<StatementNodeTree> allOldOperations = oldMethodNode.getAllOperations();
        List<StatementNodeTree> allNewOperations = newMethodNode.getAllOperations();
        for (StatementNodeTree oldOperation : allOldOperations) {
            if (oldOperation.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && !oldOperation.isMatched()) {
                VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) oldOperation.getStatement();
                List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
                for (VariableDeclarationFragment fragment : fragments) {
                    if (fragment.getInitializer() != null) {
                        Expression initializer = fragment.getInitializer();
                        if (initializer instanceof CastExpression)
                            oldVariableDeclaration.put(fragment.getName().getIdentifier(), ((CastExpression) initializer).getExpression().toString());
                        else
                            oldVariableDeclaration.put(fragment.getName().getIdentifier(), initializer.toString());
                    }
                }
            }
        }
        for (StatementNodeTree newOperation : allNewOperations) {
            if (newOperation.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && !newOperation.isMatched()) {
                VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) newOperation.getStatement();
                List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
                for (VariableDeclarationFragment fragment : fragments) {
                    if (fragment.getInitializer() != null) {
                        Expression initializer = fragment.getInitializer();
                        if (initializer instanceof CastExpression)
                            newVariableDeclaration.put(fragment.getName().getIdentifier(), ((CastExpression) initializer).getExpression().toString());
                        else
                            newVariableDeclaration.put(fragment.getName().getIdentifier(), initializer.toString());
                    }
                }
            }
        }

        for (Pair<StatementNodeTree, StatementNodeTree> pair : pairs) {
            StatementNodeTree oldStatement = pair.getLeft();
            StatementNodeTree newStatement = pair.getRight();
            List<MethodInvocation> list = new ArrayList<>();
            newStatement.getStatement().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation node) {
                    if (node.getName().getIdentifier().equals(extractedEntity.getName()) && node.arguments().size() == declaration.parameters().size())
                        list.add(node);
                    return true;
                }
            });
            if (list.isEmpty())
                continue;
            if (isSameStatement(oldEntity, newEntity, oldStatement, newStatement, list, declaration, expression, refactorings, false))
                return true;
        }

        for (StatementNodeTree newOperation : allNewOperations) {
            List<MethodInvocation> list = new ArrayList<>();
            newOperation.getStatement().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation node) {
                    if (node.getName().getIdentifier().equals(extractedEntity.getName()) && node.arguments().size() == declaration.parameters().size())
                        list.add(node);
                    return true;
                }
            });
            if (list.isEmpty())
                continue;
            for (StatementNodeTree oldOperation : allOldOperations) {
                if (oldOperation.getType() == newOperation.getType() &&
                        isSameStatement(oldEntity, newEntity, oldOperation, newOperation, list, declaration, expression, refactorings, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String replaceLast(String originalString, String searchString, String replacementString) {
        int lastIndex = originalString.lastIndexOf(searchString);
        if (lastIndex != -1) {
            String newString = originalString.substring(0, lastIndex) + replacementString +
                    originalString.substring(lastIndex + searchString.length());
            return newString;
        }
        return originalString;
    }

    private boolean contains(StatementNodeTree snt, String name) {
        List<String> list = new ArrayList<>();
        if (snt.getType() == StatementType.DO_STATEMENT) {
            DoStatement doStatement = (DoStatement) snt.getStatement();
            Expression expression = doStatement.getExpression();
            expression.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            return list.contains(name);
        } else if (snt.getType() == StatementType.ENHANCED_FOR_STATEMENT) {
            EnhancedForStatement enhancedForStatement = (EnhancedForStatement) snt.getStatement();
            SingleVariableDeclaration parameter = enhancedForStatement.getParameter();
            Expression expression = enhancedForStatement.getExpression();
            parameter.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            parameter.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            return list.contains(name);
        } else if (snt.getType() == StatementType.FOR_STATEMENT) {
            ForStatement forStatement = (ForStatement) snt.getStatement();
            List<Expression> initializers = forStatement.initializers();
            Expression expression = forStatement.getExpression();
            List<Expression> updaters = forStatement.updaters();
            for (Expression initializer : initializers) {
                initializer.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(SimpleName node) {
                        list.add(node.getIdentifier());
                        return true;
                    }
                });
            }
            expression.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            for (Expression updater : updaters) {
                updater.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(SimpleName node) {
                        list.add(node.getIdentifier());
                        return true;
                    }
                });
            }
            return list.contains(name);
        } else if (snt.getType() == StatementType.IF_STATEMENT) {
            IfStatement ifStatement = (IfStatement) snt.getStatement();
            Expression expression = ifStatement.getExpression();
            expression.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            return list.contains(name);
        } else if (snt.getType() == StatementType.SWITCH_STATEMENT) {
            SwitchStatement switchStatement = (SwitchStatement) snt.getStatement();
            Expression expression = switchStatement.getExpression();
            expression.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            return list.contains(name);
        } else if (snt.getType() == StatementType.TRY_STATEMENT) {
            TryStatement tryStatement = (TryStatement) snt.getStatement();
            List<Expression> resources = tryStatement.resources();
            for (Expression resource : resources) {
                resource.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(SimpleName node) {
                        list.add(node.getIdentifier());
                        return true;
                    }
                });
            }
            return list.contains(name);
        } else if (snt.getType() == StatementType.WHILE_STATEMENT) {
            WhileStatement whileStatement = (WhileStatement) snt.getStatement();
            Expression expression = whileStatement.getExpression();
            expression.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            return list.contains(name);
        } else if (snt.getType() == StatementType.CATCH_CLAUSE) {
            CatchClause catchClause = (CatchClause) snt.getStatement();
            SingleVariableDeclaration variable = catchClause.getException();
            variable.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    list.add(node.getIdentifier());
                    return true;
                }
            });
            return list.contains(name);
        }
        ASTNode statement = snt.getStatement();
        statement.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                list.add(node.getIdentifier());
                return true;
            }
        });
        return list.contains(name);
    }

    private boolean isSameStatement(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity,
                                    StatementNodeTree oldStatement, StatementNodeTree newStatement,
                                    List<MethodInvocation> list, MethodDeclaration declaration,
                                    Expression expression, List<Refactoring> refactorings, boolean subRefactoringChecked) {
        Map<String, String> replacements = new HashMap();
        for (MethodInvocation invocation : list) {
            List<Expression> arguments = invocation.arguments();
            List<SingleVariableDeclaration> parameters = declaration.parameters();
            for (int i = 0; i < arguments.size(); i++) {
                Expression argument = arguments.get(i);
                SingleVariableDeclaration parameter = parameters.get(i);
                if (!argument.toString().equals(parameter.getName().getIdentifier()))
                    replacements.put(parameter.getName().getIdentifier(), argument.toString());
            }
            if (expression instanceof ConditionalExpression conditionalExpression) {
                String thenExpression = conditionalExpression.getThenExpression().toString();
                String elseExpression = conditionalExpression.getElseExpression().toString();
                String replacedThenExpression = thenExpression;
                String replacedElseExpression = elseExpression;
                for (String key : replacements.keySet()) {
                    replacedThenExpression = replacedThenExpression.replace(key, replacements.get(key));
                    replacedElseExpression = replacedElseExpression.replace(key, replacements.get(key));
                }
                if (isSameStatement(oldEntity, newEntity, oldStatement, newStatement, invocation, replacedThenExpression, replacedElseExpression, refactorings, subRefactoringChecked))
                    return true;
            }
            if (expression instanceof InfixExpression infixExpression) {
                String leftOperand = infixExpression.getLeftOperand().toString();
                String rightOperand = infixExpression.getRightOperand().toString();
                String replacedLeftOperand = leftOperand;
                String replacedRightOperand = rightOperand;
                for (String key : replacements.keySet()) {
                    replacedLeftOperand = replacedLeftOperand.replace(key, replacements.get(key));
                    replacedRightOperand = replacedRightOperand.replace(key, replacements.get(key));
                }
                if (isSameStatement(oldEntity, newEntity, oldStatement, newStatement, invocation, replacedLeftOperand, replacedRightOperand, refactorings, subRefactoringChecked))
                    return true;
            }
            if (expression instanceof CastExpression castExpression) {
                String castString = castExpression.getExpression().toString();
                String replacedString = expression.toString();
                String replacedCastString = castString;
                for (String key : replacements.keySet()) {
                    replacedString = replacedString.replace(key, replacements.get(key));
                    replacedCastString = replacedCastString.replace(key, replacements.get(key));
                }
                if (isSameStatement(oldEntity, newEntity, oldStatement, newStatement, invocation, replacedString, replacedCastString, refactorings, subRefactoringChecked))
                    return true;
            }
        }
        return false;
    }

    private boolean isSameStatement(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, StatementNodeTree oldStatement,
                                    StatementNodeTree newStatement, MethodInvocation invocation,
                                    String replacedString1, String replacedString2, List<Refactoring> refactorings,
                                    boolean subRefactoringChecked) {
        List<VariableDeclarationFragment> allOldOperations = getVariableDeclaration(oldEntity);
        Map<String, String> variableDeclaration1 = new HashMap<>();
        for (VariableDeclarationFragment fragment : allOldOperations) {
            if (fragment.getInitializer() != null) {
                Expression initializer = fragment.getInitializer();
                if (initializer instanceof CastExpression)
                    variableDeclaration1.put(fragment.getName().getIdentifier(), ((CastExpression) initializer).getExpression().toString());
                else
                    variableDeclaration1.put(fragment.getName().getIdentifier(), initializer.toString());
            }
        }

        List<VariableDeclarationFragment> allNewOperations = getVariableDeclaration(newEntity);
        Map<String, String> variableDeclaration2 = new HashMap<>();
        for (VariableDeclarationFragment fragment : allNewOperations) {
            if (fragment.getInitializer() != null) {
                Expression initializer = fragment.getInitializer();
                if (initializer instanceof CastExpression)
                    variableDeclaration2.put(fragment.getName().getIdentifier(), ((CastExpression) initializer).getExpression().toString());
                else
                    variableDeclaration2.put(fragment.getName().getIdentifier(), initializer.toString());
            }
        }

        String oldExpression = oldStatement.getExpression();
        String newExpression = newStatement.getExpression();
        if (oldExpression.equals(newExpression)) return false;
        if (oldStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && newStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
            VariableDeclarationStatement oldVariableDeclaration = (VariableDeclarationStatement) oldStatement.getStatement();
            VariableDeclarationStatement newVariableDeclaration = (VariableDeclarationStatement) newStatement.getStatement();
            List<IExtendedModifier> oldModifiers = oldVariableDeclaration.modifiers();
            for (IExtendedModifier modifier : oldModifiers) {
                oldExpression = oldExpression.replace(modifier.toString(), "");
            }
            List<IExtendedModifier> newModifiers = newVariableDeclaration.modifiers();
            for (IExtendedModifier modifier : newModifiers) {
                newExpression = newExpression.replace(modifier.toString(), "");
            }
        }
        oldExpression = oldExpression.strip();
        newExpression = newExpression.strip();
        String newExpression1 = newExpression.replace(invocation.toString(), replacedString1);
        String newExpression2 = newExpression.replace(invocation.toString(), replacedString2);
        if (oldExpression.equals(newExpression1) ||
                oldExpression.equals(newExpression2))
            return true;
        String replacedOldExpression = oldExpression;
        for (String key : variableDeclaration1.keySet()) {
            replacedOldExpression = replacedOldExpression.replace(key, variableDeclaration1.get(key));
        }
        String replacedNewExpression1 = newExpression1;
        String replacedNewExpression2 = newExpression2;
        for (String key : variableDeclaration2.keySet()) {
            replacedNewExpression1 = replacedNewExpression1.replace(key, variableDeclaration2.get(key));
            replacedNewExpression2 = replacedNewExpression2.replace(key, variableDeclaration2.get(key));
        }
        if (replacedOldExpression.equals(replacedNewExpression1) || replacedOldExpression.equals(replacedNewExpression2) ||
                replacedOldExpression.equals(newExpression1) || replacedOldExpression.equals(newExpression2) ||
                oldExpression.equals(replacedNewExpression1) || oldExpression.equals(replacedNewExpression2)) {
            if (subRefactoringChecked && allOldOperations.size() == 1) {
                Map<StatementNodeTree, StatementNodeTree> references = new TreeMap<>(Comparator.comparingInt(StatementNodeTree::getPosition));
                references.put(oldStatement, newStatement);
                InlineVariableRefactoring refactoring = new InlineVariableRefactoring(allOldOperations.get(0), oldEntity, newEntity, references);
                refactorings.add(refactoring);
            }
            if (subRefactoringChecked && allNewOperations.size() == 1) {
                Map<StatementNodeTree, StatementNodeTree> references = new TreeMap<>(Comparator.comparingInt(StatementNodeTree::getPosition));
                references.put(oldStatement, newStatement);
                ExtractVariableRefactoring refactoring = new ExtractVariableRefactoring(allNewOperations.get(0), oldEntity, newEntity, references);
                refactorings.add(refactoring);
            }
            return true;
        }
        return false;
    }

    private List<VariableDeclarationFragment> getVariableDeclaration(DeclarationNodeTree entity) {
        MethodNode methodNode = entity.getMethodNode();
        List<VariableDeclarationFragment> variableDeclarations = new ArrayList<>();
        List<StatementNodeTree> allOperations = methodNode.getAllOperations();
        for (StatementNodeTree operation : allOperations) {
            if (operation.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && !operation.isMatched()) {
                VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) operation.getStatement();
                List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
                variableDeclarations.addAll(fragments);
            }
        }
        return variableDeclarations;
    }

    private boolean hasMethodInvocation(DeclarationNodeTree node1, DeclarationNodeTree node2) {
        MethodDeclaration declaration1 = (MethodDeclaration) node1.getDeclaration();
        MethodDeclaration declaration2 = (MethodDeclaration) node2.getDeclaration();
        List<MethodInvocation> invocations = new ArrayList<>();
        List<ConstructorInvocation> constructors = new ArrayList<>();
        declaration1.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                if (node.getName().getIdentifier().equals(declaration2.getName().getIdentifier()) &&
                        node.arguments().size() == declaration2.parameters().size())
                    invocations.add(node);
                return true;
            }

            public boolean visit(ConstructorInvocation node) {
                if (declaration2.isConstructor() &&
                        node.arguments().size() == declaration2.parameters().size())
                    constructors.add(node);
                return true;
            }
        });
        List<DeclarationNodeTree> children = node2.getParent().getChildren();
        for (DeclarationNodeTree child : children) {
            if (child.getType() != EntityType.METHOD) continue;
            MethodDeclaration declaration = (MethodDeclaration) child.getDeclaration();
            if (child.getName().equals(node2.getName()) && declaration2.parameters().size() == declaration.parameters().size())
                return false;
        }
        return (!invocations.isEmpty() || !constructors.isEmpty()) && StringUtils.equals(node1.getNamespace(), node2.getNamespace());
    }

    private boolean matchedLOCAreGreaterThanUnmatchedLOC(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity,
                                                         DeclarationNodeTree anotherEntity, boolean isExtracted,
                                                         Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements) {
        MethodNode methodNode = anotherEntity.getMethodNode();
        List<StatementNodeTree> statements = methodNode.getMatchedStatements();
        List<StatementNodeTree> allOperations = methodNode.getAllOperations();
        int totalLOC = methodNode.getAllControls().size() + methodNode.getAllBlocks().size() + allOperations.size() - 1;
        int matchedLOC = 0;
        // isExtracted == true ? extract method : inline method
        if (isExtracted) {
            for (StatementNodeTree snt : statements) {
                for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
                    StatementNodeTree oldStatement = pair.getLeft();
                    StatementNodeTree newStatement = pair.getRight();
                    if (snt == newStatement && oldStatement.getRoot().getMethodEntity() == oldEntity) {
                        matchedLOC += 1;
                    }
                }
            }
        } else {
            for (StatementNodeTree snt : statements) {
                for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
                    StatementNodeTree oldStatement = pair.getLeft();
                    StatementNodeTree newStatement = pair.getRight();
                    if (snt == oldStatement && newStatement.getRoot().getMethodEntity() == newEntity) {
                        matchedLOC += 1;
                    }
                }
            }
        }
        for (StatementNodeTree operation : allOperations) {
            StatementType type = operation.getType();
            if ((type == StatementType.RETURN_STATEMENT || type == StatementType.VARIABLE_DECLARATION_STATEMENT) && !operation.isMatched())
                totalLOC -= 1;
        }
        if (matchedLOC == 0) return false;
        int unMatchedLOC = totalLOC - matchedLOC;
        return matchedLOC >= unMatchedLOC;
    }

    private void detectRefactoringsBetweenMatchedDeletedEntities(Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities,
                                                                 Set<DeclarationNodeTree> deletedEntities,
                                                                 Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                                                 MatchPair matchPair, List<Refactoring> refactorings) {
        for (DeclarationNodeTree inlinedEntity : deletedEntities) {
            if (inlinedEntity.getType() != EntityType.METHOD)
                continue;
            List<EntityInfo> dependencies = inlinedEntity.getDependencies();
            for (Pair<DeclarationNodeTree, DeclarationNodeTree> pair : matchedEntities) {
                DeclarationNodeTree oldEntity = pair.getLeft();
                DeclarationNodeTree newEntity = pair.getRight();
                if (oldEntity.getType() == EntityType.METHOD && newEntity.getType() == EntityType.METHOD) {
                    if (inlinedEntity.getName().equals("checkCondition") && oldEntity.getName().equals("stripAffix"))
                        System.out.println(1);
                    if (!dependencies.contains(oldEntity.getEntity()) && !hasMethodInvocation(oldEntity, inlinedEntity))
                        continue;
                    if (MethodUtils.isNewFunction(newEntity.getDeclaration(), oldEntity.getDeclaration()))
                        continue;
                    if (MethodUtils.isGetter((MethodDeclaration) inlinedEntity.getDeclaration()) || MethodUtils.isSetter((MethodDeclaration) inlinedEntity.getDeclaration()))
                        continue;
                    double dice = DiceFunction.calculateBodyDice((LeafNode) newEntity, (LeafNode) oldEntity, (LeafNode) inlinedEntity);
                    if (dice >= 0.15 ||
                            matchedLOCAreGreaterThanUnmatchedLOC(oldEntity, newEntity, inlinedEntity, false, matchedStatements)) {
                        boolean isMove = !inlinedEntity.getNamespace().equals(newEntity.getNamespace()) &&
                                !matchedEntities.contains(Pair.of(inlinedEntity.getParent(), newEntity.getParent()));
                        if (isMove) {
                            MoveAndInlineOperationRefactoring refactoring = new MoveAndInlineOperationRefactoring(oldEntity, newEntity, inlinedEntity, matchedStatements);
                            refactorings.add(refactoring);
                        } else {
                            InlineOperationRefactoring refactoring = new InlineOperationRefactoring(oldEntity, newEntity, inlinedEntity, matchedStatements);
                            refactorings.add(refactoring);
                        }
                    } else {
                        MethodNode inlinedMethodNode = inlinedEntity.getMethodNode();
                        List<StatementNodeTree> allOperations = inlinedMethodNode.getAllOperations();
                        List<StatementNodeTree> allControls = inlinedMethodNode.getAllControls();
                        List<StatementNodeTree> allBlocks = inlinedMethodNode.getAllBlocks();
                        if (allOperations.size() == 1 && allControls.isEmpty() && allBlocks.size() == 1 &&
                                allOperations.get(0).getType() == StatementType.RETURN_STATEMENT && allOperations.get(0).getExpression().startsWith("return ")) {
                            boolean isInlined = false;
                            for (Pair<StatementNodeTree, StatementNodeTree> pair2 : matchedStatements) {
                                StatementNodeTree oldStatement = pair2.getLeft();
                                StatementNodeTree newStatement = pair2.getRight();
                                if (oldStatement.getRoot().getMethodEntity() == oldEntity && newStatement.getRoot().getMethodEntity() == newEntity) {
                                    MethodNode oldMethodNode = oldEntity.getMethodNode();
                                    List<StatementNodeTree> operations = oldMethodNode.getAllOperations();
                                    List<StatementNodeTree> controls = oldMethodNode.getAllControls();
                                    List<StatementNodeTree> locations = new ArrayList<>();
                                    findMethodInvocation(operations, controls, inlinedEntity, locations);
                                    List<MethodInvocation> invocations = new ArrayList<>();
                                    for (StatementNodeTree location : locations) {
                                        location.getStatement().accept(new ASTVisitor() {
                                            @Override
                                            public boolean visit(MethodInvocation node) {
                                                if (node.getName().getIdentifier().equals(inlinedEntity.getName())) {
                                                    invocations.add(node);
                                                }
                                                return true;
                                            }
                                        });
                                    }
                                    for (MethodInvocation invocation : invocations) {
                                        List<Expression> arguments = invocation.arguments();
                                        MethodDeclaration declaration = (MethodDeclaration) inlinedEntity.getDeclaration();
                                        List<SingleVariableDeclaration> parameters = declaration.parameters();
                                        String operation = allOperations.get(0).getExpression().substring("return ".length());
                                        if (operation.endsWith(";\n"))
                                            operation = operation.substring(0, operation.length() - 2);
                                        if (arguments.size() == parameters.size()) {
                                            for (int i = 0; i < arguments.size(); i++) {
                                                String argument = arguments.get(i).toString();
                                                String parameter = parameters.get(i).getName().getIdentifier();
                                                operation = operation.replace(argument, parameter);
                                            }
                                        }
                                        if (oldStatement.getExpression().contains(invocation.toString()) && !newStatement.getExpression().equals(oldStatement.getExpression()) &&
                                                newStatement.getExpression().equals(oldStatement.getExpression().replace(invocation.toString(), operation))) {
                                            isInlined = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!isInlined)
                                continue;
                            boolean isMove = !inlinedEntity.getNamespace().equals(newEntity.getNamespace()) &&
                                    !matchedEntities.contains(Pair.of(inlinedEntity.getParent(), newEntity.getParent()));
                            if (isMove) {
                                MoveAndInlineOperationRefactoring refactoring = new MoveAndInlineOperationRefactoring(oldEntity, newEntity, inlinedEntity, matchedStatements);
                                refactorings.add(refactoring);
                            } else {
                                InlineOperationRefactoring refactoring = new InlineOperationRefactoring(oldEntity, newEntity, inlinedEntity, matchedStatements);
                                refactorings.add(refactoring);
                            }
                        }
                    }
                }
            }
        }
    }

    private DeclarationNodeTree findMatchedEntity(Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities, DeclarationNodeTree entity) {
        for (Pair<DeclarationNodeTree, DeclarationNodeTree> pair : matchedEntities) {
            if (pair.getLeft() == entity)
                return pair.getRight();
            if (pair.getRight() == entity)
                return pair.getLeft();
        }
        return null;
    }

    private boolean isSubTypeOf(Set<Pair<DeclarationNodeTree, DeclarationNodeTree>> matchedEntities, DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, int range) {
        DeclarationNodeTree oldParent = oldEntity.getParent();
        DeclarationNodeTree newParent = newEntity.getParent();
        if ((oldParent.getType() == EntityType.CLASS || oldParent.getType() == EntityType.INTERFACE) &&
                (newParent.getType() == EntityType.CLASS || newParent.getType() == EntityType.INTERFACE)) {
            TypeDeclaration removedClass = (TypeDeclaration) oldParent.getDeclaration();
            TypeDeclaration addedClass = (TypeDeclaration) newParent.getDeclaration();
            DeclarationNodeTree matchedAddedEntity = findMatchedEntity(matchedEntities, newParent);
            TypeDeclaration matchedAddedClass = matchedAddedEntity == null ? null : (TypeDeclaration) (matchedAddedEntity.getDeclaration());
            DeclarationNodeTree matchedDeletedEntity = findMatchedEntity(matchedEntities, oldParent);
            TypeDeclaration matchedDeletedClass = matchedDeletedEntity == null ? null : (TypeDeclaration) (matchedDeletedEntity.getDeclaration());
            if (range == 0)
                return matchedDeletedClass != null && isSubTypeOf(matchedDeletedClass, addedClass);
            if (range == 1)
                return matchedAddedClass != null && isSubTypeOf(removedClass, matchedAddedClass);
        }
        return false;
    }

    private boolean isSubTypeOf(TypeDeclaration removedClass, TypeDeclaration addedClass) {
        ITypeBinding removedBinding = removedClass.resolveBinding();
        ITypeBinding addedBinding = addedClass.resolveBinding();
        if (removedBinding != null) {
            ITypeBinding superClassBinding = removedBinding.getSuperclass();
            if (superClassBinding != null && addedBinding != null) {
                boolean superClass = isSubclassOrImplementation(superClassBinding, addedBinding);
                if (superClass)
                    return true;
            }
            ITypeBinding[] interfaces = removedBinding.getInterfaces();
            for (ITypeBinding typeBinding : interfaces) {
                if (addedBinding != null) {
                    boolean isInterface = isSubclassOrImplementation(typeBinding, addedBinding);
                    if (isInterface)
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean isSubclassOrImplementation(ITypeBinding removedBinding, ITypeBinding addedBinding) {
        if (removedBinding == addedBinding || removedBinding.getTypeDeclaration().isEqualTo(addedBinding) ||
                StringUtils.equals(removedBinding.getQualifiedName(), addedBinding.getQualifiedName())) {
            return true;
        }
        ITypeBinding superClassBinding = removedBinding.getSuperclass();
        if (superClassBinding != null && isSubclassOrImplementation(superClassBinding, addedBinding)) {
            return true;
        }
        ITypeBinding[] interfaceBindings = removedBinding.getInterfaces();
        for (ITypeBinding interfaceBinding : interfaceBindings) {
            if (isSubclassOrImplementation(interfaceBinding, addedBinding)) {
                return true;
            }
        }
        return false;
    }

    private void detectRefactoringsInMatchedStatements(Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                                       List<Refactoring> refactorings) {
        for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
            StatementNodeTree oldStatement = pair.getLeft();
            StatementNodeTree newStatement = pair.getRight();
            DeclarationNodeTree oldEntity = oldStatement.getRoot().getMethodEntity();
            DeclarationNodeTree newEntity = newStatement.getRoot().getMethodEntity();
            if (oldStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && newStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
                processVariableDeclarationStatement(oldStatement, newStatement, oldEntity, newEntity, refactorings);
            }
            if (oldStatement.getType() == StatementType.IF_STATEMENT && newStatement.getType() == StatementType.IF_STATEMENT) {
                processIfStatement(oldStatement, newStatement, oldEntity, newEntity, refactorings);
            }
            if (oldStatement.getType() == StatementType.ENHANCED_FOR_STATEMENT && newStatement.getType() == StatementType.ENHANCED_FOR_STATEMENT) {
                processEnhancedForStatement(oldStatement, newStatement, oldEntity, newEntity, refactorings);
            }
            if (oldStatement.getType() == StatementType.FOR_STATEMENT && newStatement.getType() == StatementType.FOR_STATEMENT) {
                processForStatement(oldStatement, newStatement, oldEntity, newEntity, refactorings);
            }
            if (oldStatement.getType() == StatementType.CATCH_CLAUSE && newStatement.getType() == StatementType.CATCH_CLAUSE) {
                processCatchClause(oldStatement, newStatement, oldEntity, newEntity, refactorings);
            }
            if (oldStatement.getType() == StatementType.TRY_STATEMENT && newStatement.getType() == StatementType.TRY_STATEMENT) {
                processTryStatement(oldStatement, newStatement, oldEntity, newEntity, refactorings);
            }
            if ((oldStatement.getType() == StatementType.FOR_STATEMENT || oldStatement.getType() == StatementType.ENHANCED_FOR_STATEMENT ||
                    oldStatement.getType() == StatementType.WHILE_STATEMENT || oldStatement.getType() == StatementType.DO_STATEMENT) &&
                    (newStatement.getType() == StatementType.FOR_STATEMENT || newStatement.getType() == StatementType.ENHANCED_FOR_STATEMENT ||
                            newStatement.getType() == StatementType.WHILE_STATEMENT || newStatement.getType() == StatementType.DO_STATEMENT)) {
                processLoopStatement(oldStatement, newStatement, oldEntity, newEntity, matchedStatements, refactorings);
            }
            if ((oldStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT || oldStatement.getType() == StatementType.EXPRESSION_STATEMENT ||
                    oldStatement.getType() == StatementType.RETURN_STATEMENT) &&
                    (newStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT || newStatement.getType() == StatementType.EXPRESSION_STATEMENT ||
                            newStatement.getType() == StatementType.RETURN_STATEMENT)) {
                processAnonymousWithLambda(oldStatement, newStatement, oldEntity, newEntity, refactorings);
            }
            if (oldStatement.getType() == StatementType.IF_STATEMENT && newStatement.getType() == StatementType.IF_STATEMENT) {
                processInvertCondition(oldStatement, newStatement, oldEntity, newEntity, matchedStatements, refactorings);
            }
            if (oldStatement instanceof OperationNode && newStatement instanceof OperationNode) {
                List<LambdaExpression> oldLambdaExpressions = new ArrayList<>();
                List<LambdaExpression> newLambdaExpressions = new ArrayList<>();
                oldStatement.getStatement().accept(new ASTVisitor() {
                    @Override
                    public boolean visit(LambdaExpression node) {
                        oldLambdaExpressions.add(node);
                        return true;
                    }
                });
                newStatement.getStatement().accept(new ASTVisitor() {
                    @Override
                    public boolean visit(LambdaExpression node) {
                        newLambdaExpressions.add(node);
                        return true;
                    }
                });
                if (oldLambdaExpressions.size() == 1 && newLambdaExpressions.size() == 1) {
                    LambdaExpression oldLambdaExpression = oldLambdaExpressions.get(0);
                    LambdaExpression newLambdaExpression = newLambdaExpressions.get(0);
                    List<VariableDeclaration> oldParameters = oldLambdaExpression.parameters();
                    List<VariableDeclaration> newParameters = newLambdaExpression.parameters();
                    if (oldParameters.size() == 1 && newParameters.size() == 1) {
                        VariableDeclaration oldFragment = oldParameters.get(0);
                        VariableDeclaration newFragment = newParameters.get(0);
                        if (!oldFragment.getName().getIdentifier().equals(newFragment.getName().getIdentifier())) {
                            RenameVariableRefactoring refactoring = new RenameVariableRefactoring(oldFragment, newFragment, oldEntity, newEntity);
                            refactorings.add(refactoring);
                        }
                    }
                }
                List<MethodDeclaration> oldMethods = new ArrayList<>();
                List<MethodDeclaration> newMethods = new ArrayList<>();
                oldStatement.getStatement().accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        oldMethods.add(node);
                        return true;
                    }
                });
                newStatement.getStatement().accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        newMethods.add(node);
                        return true;
                    }
                });
                for (MethodDeclaration oldMethod : oldMethods) {
                    for (MethodDeclaration newMethod : newMethods) {
                        String originalType = oldMethod.getReturnType2() == null ? "" : oldMethod.getReturnType2().toString();
                        String changedType = newMethod.getReturnType2() == null ? "" : newMethod.getReturnType2().toString();
                        if (oldMethod.getName().getIdentifier().equals(newMethod.getName().getIdentifier()) &&
                                StringUtils.equals(originalType, changedType) &&
                                oldMethod.parameters().size() == newMethod.parameters().size() && equalsParameters(oldMethod, newMethod)) {
                            DeclarationNodeTree leftEntity = new LeafNode((CompilationUnit) oldMethod.getRoot(), oldStatement.getLocationInfo().getFilePath(), oldMethod);
                            DeclarationNodeTree rightEntity = new LeafNode((CompilationUnit) newMethod.getRoot(), newStatement.getLocationInfo().getFilePath(), newMethod);
                            leftEntity.setDeclaration(oldMethod);
                            leftEntity.setType(EntityType.METHOD);
                            leftEntity.setParent(new InternalNode((CompilationUnit) oldMethod.getRoot(), oldStatement.getLocationInfo().getFilePath(), oldMethod));
                            leftEntity.getParent().setType(EntityType.CLASS);
                            if (oldMethod.getParent() instanceof AnonymousClassDeclaration && oldMethod.getParent().getParent() instanceof ClassInstanceCreation)
                                leftEntity.setNamespace(oldStatement.getRoot().getMethodEntity().getNamespace() + "." + oldStatement.getRoot().getMethodEntity().getName() + ".new " +
                                        ((ClassInstanceCreation) oldMethod.getParent().getParent()).getType().toString());
                            rightEntity.setDeclaration(newMethod);
                            rightEntity.setType(EntityType.METHOD);
                            rightEntity.setParent(new InternalNode((CompilationUnit) oldMethod.getRoot(), oldStatement.getLocationInfo().getFilePath(), oldMethod));
                            rightEntity.getParent().setType(EntityType.CLASS);
                            if (newMethod.getParent() instanceof AnonymousClassDeclaration && newMethod.getParent().getParent() instanceof ClassInstanceCreation)
                                rightEntity.setNamespace(newStatement.getRoot().getMethodEntity().getNamespace() + "." + newStatement.getRoot().getMethodEntity().getName() + ".new " +
                                        ((ClassInstanceCreation) newMethod.getParent().getParent()).getType().toString());
                            checkForOperationAnnotationChanges(leftEntity, rightEntity, refactorings);
                            checkForOperationModifierChanges(leftEntity, rightEntity, refactorings);
                            checkForThrownExceptionTypeChanges(leftEntity, rightEntity, refactorings);
                        }
                        if (oldMethod.getName().getIdentifier().equals(newMethod.getName().getIdentifier()) &&
                                !StringUtils.equals(originalType, changedType) && oldMethod.getBody() != null && newMethod.getBody() != null &&
                                oldMethod.getBody().toString().equals(newMethod.getBody().toString()) &&
                                oldMethod.parameters().size() == newMethod.parameters().size() && equalsParameters(oldMethod, newMethod)) {
                            ChangeReturnTypeRefactoring refactoring = new ChangeReturnTypeRefactoring(oldMethod.getReturnType2(), newMethod.getReturnType2(), oldEntity, newEntity);
                            refactorings.add(refactoring);
                        }
                    }
                }
            }
        }
    }

    private boolean equalsParameters(MethodDeclaration oldMethod, MethodDeclaration newMethod) {
        List<SingleVariableDeclaration> oldParameters = oldMethod.parameters();
        List<SingleVariableDeclaration> newParameters = newMethod.parameters();
        for (int i = 0; i < oldParameters.size(); i++) {
            SingleVariableDeclaration oldParameter = oldParameters.get(i);
            SingleVariableDeclaration newParameter = newParameters.get(i);
            if (!oldParameter.getName().getIdentifier().equals(newParameter.getName().getIdentifier()) ||
                    !oldParameter.getType().toString().equals(newParameter.getType().toString()))
                return false;
        }
        return true;
    }

    private void processVariableDeclarationStatement(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                                     DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        VariableDeclarationStatement statement1 = (VariableDeclarationStatement) oldStatement.getStatement();
        VariableDeclarationStatement statement2 = (VariableDeclarationStatement) newStatement.getStatement();
        List<VariableDeclarationFragment> fragments1 = statement1.fragments();
        List<VariableDeclarationFragment> fragments2 = statement2.fragments();
        Type type1 = statement1.getType();
        Type type2 = statement2.getType();
        if (fragments1.size() != fragments2.size())
            return;
        for (int i = 0; i < fragments1.size(); i++) {
            VariableDeclarationFragment fragment1 = fragments1.get(i);
            VariableDeclarationFragment fragment2 = fragments2.get(i);
            boolean isRepaired = isRepaired(type1, type2, fragment1, fragment2, oldEntity, newEntity, refactorings);
            if (!fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) && isRepaired) {
                RenameVariableRefactoring refactoring = new RenameVariableRefactoring(fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (!type1.toString().equals(type2.toString()) && isRepaired) {
                ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            List<IExtendedModifier> modifiers1 = statement1.modifiers();
            List<IExtendedModifier> modifiers2 = statement2.modifiers();
            AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
            for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
                AddVariableAnnotationRefactoring refactoring = new AddVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
                RemoveVariableAnnotationRefactoring refactoring = new RemoveVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
                ModifyVariableAnnotationRefactoring refactoring = new ModifyVariableAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            int variableModifiers1 = statement1.getModifiers();
            int variableModifiers2 = statement2.getModifiers();
            if (!Flags.isFinal(variableModifiers1) && Flags.isFinal(variableModifiers2)) {
                AddVariableModifierRefactoring refactoring = new AddVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (Flags.isFinal(variableModifiers1) && !Flags.isFinal(variableModifiers2)) {
                RemoveVariableModifierRefactoring refactoring = new RemoveVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
    }

    private boolean isRepaired(Type type1, Type type2, VariableDeclaration fragment1, VariableDeclaration fragment2,
                               DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        MethodNode oldMethodNode = oldEntity.getMethodNode();
        MethodNode newMethodNode = newEntity.getMethodNode();
        List<StatementNodeTree> allOldOperations = oldMethodNode.getAllOperations();
        List<StatementNodeTree> allNewOperations = newMethodNode.getAllOperations();
        for (StatementNodeTree operation : allOldOperations) {
            if (operation.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && !operation.isMatched()) {
                VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) operation.getStatement();
                List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
                for (VariableDeclarationFragment fragment : fragments) {
                    if (fragment.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) &&
                            (variableDeclaration.getType().toString().equals(type2.toString()) ||
                                    variableDeclaration.getType().toString().equals("var") ||
                                    type2.toString().equals("var"))) {
                        if (!variableDeclaration.getType().toString().equals(type2.toString())) {
                            ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment, fragment2, oldEntity, newEntity);
                            refactorings.add(refactoring);
                        }
                        LocationInfo locationInfo1 = oldEntity.getLocationInfo();
                        LocationInfo locationInfo2 = operation.getLocationInfo();
                        if (locationInfo2.getStartLine() > locationInfo1.getStartLine() && locationInfo2.getEndLine() < locationInfo1.getEndLine())
                            return false;
                    }
                }
            }
        }
        for (StatementNodeTree operation : allNewOperations) {
            if (operation.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && !operation.isMatched()) {
                VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) operation.getStatement();
                List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
                for (VariableDeclarationFragment fragment : fragments) {
                    if (fragment.getName().getIdentifier().equals(fragment1.getName().getIdentifier()) &&
                            (variableDeclaration.getType().toString().equals(type1.toString()) ||
                                    variableDeclaration.getType().toString().equals("var") ||
                                    type1.toString().equals("var"))) {
                        if (!variableDeclaration.getType().toString().equals(type1.toString())) {
                            ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment, oldEntity, newEntity);
                            refactorings.add(refactoring);
                        }
                        LocationInfo locationInfo1 = newEntity.getLocationInfo();
                        LocationInfo locationInfo2 = operation.getLocationInfo();
                        if (locationInfo2.getStartLine() > locationInfo1.getStartLine() && locationInfo2.getEndLine() < locationInfo1.getEndLine())
                            return false;
                    }
                }
            }
        }
        return true;
    }

    private void processIfStatement(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                    DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        IfStatement statement1 = (IfStatement) oldStatement.getStatement();
        IfStatement statement2 = (IfStatement) newStatement.getStatement();
        Expression expression1 = statement1.getExpression();
        Expression expression2 = statement2.getExpression();
        if (expression1 instanceof PatternInstanceofExpression && expression2 instanceof PatternInstanceofExpression) {
            PatternInstanceofExpression exp1 = (PatternInstanceofExpression) expression1;
            PatternInstanceofExpression exp2 = (PatternInstanceofExpression) expression2;
            SingleVariableDeclaration fragment1 = exp1.getRightOperand();
            SingleVariableDeclaration fragment2 = exp2.getRightOperand();
            Type type1 = fragment1.getType();
            Type type2 = fragment2.getType();
            boolean isRepaired = isRepaired(type1, type2, fragment1, fragment2, oldEntity, newEntity, refactorings);
            if (!fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) && isRepaired) {
                RenameVariableRefactoring refactoring = new RenameVariableRefactoring(fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (!type1.toString().equals(type2.toString()) && isRepaired) {
                ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
    }

    private void processEnhancedForStatement(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                             DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        EnhancedForStatement statement1 = (EnhancedForStatement) oldStatement.getStatement();
        EnhancedForStatement statement2 = (EnhancedForStatement) newStatement.getStatement();
        SingleVariableDeclaration fragment1 = statement1.getParameter();
        SingleVariableDeclaration fragment2 = statement2.getParameter();
        Type type1 = fragment1.getType();
        Type type2 = fragment2.getType();
        boolean isRepaired = isRepaired(type1, type2, fragment1, fragment2, oldEntity, newEntity, refactorings);
        if (!fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) && isRepaired) {
            RenameVariableRefactoring refactoring = new RenameVariableRefactoring(fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!type1.toString().equals(type2.toString()) && isRepaired) {
            ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        List<IExtendedModifier> modifiers1 = fragment1.modifiers();
        List<IExtendedModifier> modifiers2 = fragment2.modifiers();
        AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
        for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddVariableAnnotationRefactoring refactoring = new AddVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveVariableAnnotationRefactoring refactoring = new RemoveVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
            ModifyVariableAnnotationRefactoring refactoring = new ModifyVariableAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        int variableModifiers1 = fragment1.getModifiers();
        int variableModifiers2 = fragment2.getModifiers();
        if (!Flags.isFinal(variableModifiers1) && Flags.isFinal(variableModifiers2)) {
            AddVariableModifierRefactoring refactoring = new AddVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isFinal(variableModifiers1) && !Flags.isFinal(variableModifiers2)) {
            RemoveVariableModifierRefactoring refactoring = new RemoveVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void processForStatement(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                     DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        ForStatement statement1 = (ForStatement) oldStatement.getStatement();
        ForStatement statement2 = (ForStatement) newStatement.getStatement();
        List initializers1 = statement1.initializers();
        List initializers2 = statement2.initializers();
        if (initializers1.size() == 1 && initializers2.size() == 1) {
            Object obj1 = initializers1.get(0);
            Object obj2 = initializers2.get(0);
            if (obj1 instanceof VariableDeclarationExpression && obj2 instanceof VariableDeclarationExpression) {
                VariableDeclarationExpression initializer1 = (VariableDeclarationExpression) obj1;
                VariableDeclarationExpression initializer2 = (VariableDeclarationExpression) obj2;
                Type type1 = initializer1.getType();
                Type type2 = initializer2.getType();
                List<VariableDeclarationFragment> fragments1 = initializer1.fragments();
                List<VariableDeclarationFragment> fragments2 = initializer2.fragments();
                VariableDeclarationFragment fragment1 = fragments1.get(0);
                VariableDeclarationFragment fragment2 = fragments2.get(0);
                boolean isRepaired = isRepaired(type1, type2, fragment1, fragment2, oldEntity, newEntity, refactorings);
                if (type1 != null && type2 != null && !type1.toString().equals(type2.toString()) && isRepaired) {
                    ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
                if (!fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) && isRepaired) {
                    RenameVariableRefactoring refactoring = new RenameVariableRefactoring(fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
                if (!type1.toString().equals(type2.toString())) {
                    ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
                List<IExtendedModifier> modifiers1 = initializer1.modifiers();
                List<IExtendedModifier> modifiers2 = initializer2.modifiers();
                AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
                for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
                    AddVariableAnnotationRefactoring refactoring = new AddVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
                for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
                    RemoveVariableAnnotationRefactoring refactoring = new RemoveVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
                for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
                    ModifyVariableAnnotationRefactoring refactoring = new ModifyVariableAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
                int variableModifiers1 = initializer1.getModifiers();
                int variableModifiers2 = initializer2.getModifiers();
                if (!Flags.isFinal(variableModifiers1) && Flags.isFinal(variableModifiers2)) {
                    AddVariableModifierRefactoring refactoring = new AddVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
                if (Flags.isFinal(variableModifiers1) && !Flags.isFinal(variableModifiers2)) {
                    RemoveVariableModifierRefactoring refactoring = new RemoveVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
        }
    }

    private void processCatchClause(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                    DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        CatchClause statement1 = (CatchClause) oldStatement.getStatement();
        CatchClause statement2 = (CatchClause) newStatement.getStatement();
        SingleVariableDeclaration fragment1 = statement1.getException();
        SingleVariableDeclaration fragment2 = statement2.getException();
        Type type1 = fragment1.getType();
        Type type2 = fragment2.getType();
        boolean isRepaired = isRepaired(type1, type2, fragment1, fragment2, oldEntity, newEntity, refactorings);
        if (!fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) && isRepaired) {
            RenameVariableRefactoring refactoring = new RenameVariableRefactoring(fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (!type1.toString().equals(type2.toString()) && isRepaired) {
            ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        List<IExtendedModifier> modifiers1 = fragment1.modifiers();
        List<IExtendedModifier> modifiers2 = fragment2.modifiers();
        AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
        for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
            AddVariableAnnotationRefactoring refactoring = new AddVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
            RemoveVariableAnnotationRefactoring refactoring = new RemoveVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
            ModifyVariableAnnotationRefactoring refactoring = new ModifyVariableAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        int variableModifiers1 = fragment1.getModifiers();
        int variableModifiers2 = fragment2.getModifiers();
        if (!Flags.isFinal(variableModifiers1) && Flags.isFinal(variableModifiers2)) {
            AddVariableModifierRefactoring refactoring = new AddVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (Flags.isFinal(variableModifiers1) && !Flags.isFinal(variableModifiers2)) {
            RemoveVariableModifierRefactoring refactoring = new RemoveVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void processTryStatement(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                     DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        TryStatement statement1 = (TryStatement) oldStatement.getStatement();
        TryStatement statement2 = (TryStatement) newStatement.getStatement();
        List<Expression> resources1 = statement1.resources();
        List<Expression> resources2 = statement2.resources();
        List<Expression> removed1 = new ArrayList<>();
        List<Expression> removed2 = new ArrayList<>();
        for (Expression resource1 : resources1) {
            for (Expression resource2 : resources2) {
                if (resource1 instanceof VariableDeclarationExpression && resource2 instanceof VariableDeclarationExpression) {
                    VariableDeclarationExpression expression1 = (VariableDeclarationExpression) resource1;
                    VariableDeclarationExpression expression2 = (VariableDeclarationExpression) resource2;
                    List<VariableDeclarationFragment> fragments1 = expression1.fragments();
                    List<VariableDeclarationFragment> fragments2 = expression2.fragments();
                    Type type1 = expression1.getType();
                    Type type2 = expression2.getType();
                    VariableDeclarationFragment fragment1 = fragments1.get(0);
                    VariableDeclarationFragment fragment2 = fragments2.get(0);
                    if (fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) &&
                            type1.toString().equals(type2.toString())) {
                        removed1.add(resource1);
                        removed2.add(resource2);
                    }
                }
            }
        }
        if (resources1.size() != resources2.size()) {
            for (Expression resource1 : resources1) {
                if (removed1.contains(resource1)) continue;
                for (Expression resource2 : resources2) {
                    if (removed2.contains(resource2)) continue;
                    if (resource1 instanceof VariableDeclarationExpression && resource2 instanceof VariableDeclarationExpression) {
                        VariableDeclarationExpression expression1 = (VariableDeclarationExpression) resource1;
                        VariableDeclarationExpression expression2 = (VariableDeclarationExpression) resource2;
                        List<VariableDeclarationFragment> fragments1 = expression1.fragments();
                        List<VariableDeclarationFragment> fragments2 = expression2.fragments();
                        Type type1 = expression1.getType();
                        Type type2 = expression2.getType();
                        VariableDeclarationFragment fragment1 = fragments1.get(0);
                        VariableDeclarationFragment fragment2 = fragments2.get(0);
                        if (fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) ||
                                type1.toString().equals(type2.toString())) {
                            checkForResource(oldEntity, newEntity, refactorings, resource1, resource2);
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < resources1.size(); i++) {
                Object resource1 = resources1.get(i);
                Object resource2 = resources2.get(i);
                checkForResource(oldEntity, newEntity, refactorings, resource1, resource2);
            }
        }
    }

    private void checkForResource(DeclarationNodeTree oldEntity, DeclarationNodeTree newEntity, List<Refactoring> refactorings, Object resource1, Object resource2) {
        if (resource1 instanceof VariableDeclarationExpression && resource2 instanceof VariableDeclarationExpression) {
            VariableDeclarationExpression expression1 = (VariableDeclarationExpression) resource1;
            VariableDeclarationExpression expression2 = (VariableDeclarationExpression) resource2;
            List<VariableDeclarationFragment> fragments1 = expression1.fragments();
            List<VariableDeclarationFragment> fragments2 = expression2.fragments();
            Type type1 = expression1.getType();
            Type type2 = expression2.getType();
            VariableDeclarationFragment fragment1 = fragments1.get(0);
            VariableDeclarationFragment fragment2 = fragments2.get(0);
            boolean isRepaired = isRepaired(type1, type2, fragment1, fragment2, oldEntity, newEntity, refactorings);
            if (!fragment1.getName().getIdentifier().equals(fragment2.getName().getIdentifier()) && isRepaired) {
                RenameVariableRefactoring refactoring = new RenameVariableRefactoring(fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (!type1.toString().equals(type2.toString()) && isRepaired) {
                ChangeVariableTypeRefactoring refactoring = new ChangeVariableTypeRefactoring(fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            List<IExtendedModifier> modifiers1 = expression1.modifiers();
            List<IExtendedModifier> modifiers2 = expression2.modifiers();
            AnnotationListDiff annotationListDiff = new AnnotationListDiff(modifiers1, modifiers2);
            for (Annotation annotation : annotationListDiff.getAddedAnnotations()) {
                AddVariableAnnotationRefactoring refactoring = new AddVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            for (Annotation annotation : annotationListDiff.getRemovedAnnotations()) {
                RemoveVariableAnnotationRefactoring refactoring = new RemoveVariableAnnotationRefactoring(annotation, fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            for (Pair<Annotation, Annotation> annotationDiff : annotationListDiff.getAnnotationDiffs()) {
                ModifyVariableAnnotationRefactoring refactoring = new ModifyVariableAnnotationRefactoring(annotationDiff.getLeft(), annotationDiff.getRight(), fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            int variableModifiers1 = expression1.getModifiers();
            int variableModifiers2 = expression2.getModifiers();
            if (!Flags.isFinal(variableModifiers1) && Flags.isFinal(variableModifiers2)) {
                AddVariableModifierRefactoring refactoring = new AddVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
            if (Flags.isFinal(variableModifiers1) && !Flags.isFinal(variableModifiers2)) {
                RemoveVariableModifierRefactoring refactoring = new RemoveVariableModifierRefactoring("final", fragment1, fragment2, oldEntity, newEntity);
                refactorings.add(refactoring);
            }
        }
    }

    private void processLoopStatement(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                      DeclarationNodeTree newEntity, Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                      List<Refactoring> refactorings) {
        if (oldStatement.getType() != newStatement.getType()) {
            ChangeLoopTypeRefactoring refactoring = new ChangeLoopTypeRefactoring(oldStatement, newStatement, oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        StatementNodeTree parent = newStatement.getParent();
        while (parent.getDepth() > 0 &&
                !(parent.getType() == StatementType.FOR_STATEMENT || parent.getType() == StatementType.ENHANCED_FOR_STATEMENT ||
                        parent.getType() == StatementType.WHILE_STATEMENT || parent.getType() == StatementType.DO_STATEMENT)) {
            parent = parent.getParent();
        }
        StatementNodeTree child = retrieveSNTByBFS(oldStatement);
        if (child != null && matchedStatements.contains(Pair.of(child, parent))) {
            LoopInterchangeRefactoring refactoring1 = new LoopInterchangeRefactoring(oldStatement, newStatement, oldEntity, newEntity);
            refactorings.add(refactoring1);
        }
    }

    private void processAnonymousWithLambda(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                            DeclarationNodeTree newEntity, List<Refactoring> refactorings) {
        ASTNode statement1 = oldStatement.getStatement();
        ASTNode statement2 = newStatement.getStatement();
        List<AnonymousClassDeclaration> anonymous1 = new ArrayList<>();
        List<LambdaExpression> lambda1 = new ArrayList<>();
        List<MethodReference> references1 = new ArrayList<>();
        List<AnonymousClassDeclaration> anonymous2 = new ArrayList<>();
        List<LambdaExpression> lambda2 = new ArrayList<>();
        List<MethodReference> references2 = new ArrayList<>();
        statement1.accept(new ASTVisitor() {
            @Override
            public boolean visit(AnonymousClassDeclaration node) {
                anonymous1.add(node);
                return true;
            }

            @Override
            public boolean visit(LambdaExpression node) {
                lambda1.add(node);
                return true;
            }

            @Override
            public boolean visit(ExpressionMethodReference node) {
                references1.add(node);
                return true;
            }

            @Override
            public boolean visit(SuperMethodReference node) {
                references1.add(node);
                return true;
            }

            @Override
            public boolean visit(TypeMethodReference node) {
                references1.add(node);
                return true;
            }
        });
        statement2.accept(new ASTVisitor() {
            @Override
            public boolean visit(AnonymousClassDeclaration node) {
                anonymous2.add(node);
                return true;
            }

            @Override
            public boolean visit(LambdaExpression node) {
                lambda2.add(node);
                return true;
            }

            @Override
            public boolean visit(ExpressionMethodReference node) {
                references2.add(node);
                return true;
            }

            @Override
            public boolean visit(SuperMethodReference node) {
                references2.add(node);
                return true;
            }

            @Override
            public boolean visit(TypeMethodReference node) {
                references2.add(node);
                return true;
            }
        });
        if (anonymous1.size() == 1 && lambda1.isEmpty() && references1.isEmpty() &&
                lambda2.size() == 1 && references2.isEmpty() && anonymous2.isEmpty()) {
            ReplaceAnonymousWithLambdaRefactoring refactoring = new ReplaceAnonymousWithLambdaRefactoring(anonymous1.get(0), lambda2.get(0), oldEntity, newEntity);
            refactorings.add(refactoring);
        }
        if (anonymous1.size() == 1 && lambda1.isEmpty() && references1.isEmpty() &&
                lambda2.size() == 0 && references2.size() == 1 && anonymous2.isEmpty()) {
            ReplaceAnonymousWithLambdaRefactoring refactoring = new ReplaceAnonymousWithLambdaRefactoring(anonymous1.get(0), references2.get(0), oldEntity, newEntity);
            refactorings.add(refactoring);
        }
    }

    private void processInvertCondition(StatementNodeTree oldStatement, StatementNodeTree newStatement, DeclarationNodeTree oldEntity,
                                        DeclarationNodeTree newEntity, Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                        List<Refactoring> refactorings) {
        if (!oldStatement.getExpression().equals(newStatement.getExpression()))
            return;
        String expression1 = oldStatement.getExpression();
        String expression2 = newStatement.getExpression();
        boolean invertedFlag = false;
        if (!expression1.equals(expression2) && (expression1.replace("==", "!=").equals(expression2) || expression1.replace("!=", "==").equals(expression2))) {
            InvertConditionRefactoring refactoring = new InvertConditionRefactoring(oldStatement, newStatement, oldEntity, newEntity);
            refactorings.add(refactoring);
            invertedFlag = true;
        }
        if (!expression1.equals(expression2) && (invertedFlag && expression1.replace("if(", "if(!").equals(expression2) || expression1.replace("if(!", "if(").equals(expression2))) {
            InvertConditionRefactoring refactoring = new InvertConditionRefactoring(oldStatement, newStatement, oldEntity, newEntity);
            refactorings.add(refactoring);
            invertedFlag = true;
        }
        if (!expression1.equals(expression2) && (invertedFlag && expression1.replace("if(", "if(!").equals(expression2.replace("if(!(", "if（!")) ||
                expression1.replace("if(!", "if(").equals(expression2.replace("))", ")")))) {
            InvertConditionRefactoring refactoring = new InvertConditionRefactoring(oldStatement, newStatement, oldEntity, newEntity);
            refactorings.add(refactoring);
            invertedFlag = true;
        }
        List<StatementNodeTree> children1 = oldStatement.getChildren();
        List<StatementNodeTree> children2 = newStatement.getChildren();
        if (!invertedFlag) {
            for (StatementNodeTree child1 : children1) {
                if (invertedFlag)
                    break;
                for (StatementNodeTree child2 : children2) {
                    if (matchedStatements.contains(Pair.of(child1, child2)) && !Objects.equals(child1.getBlockExpression(), child2.getBlockExpression()) &&
                            ((child1.getBlockType() == BlockType.IF_BLOCK && child2.getBlockType() == BlockType.ELSE_BLOCK) ||
                                    (child1.getBlockType() == BlockType.ELSE_BLOCK && child2.getBlockType() == BlockType.IF_BLOCK))) {
                        InvertConditionRefactoring refactoring = new InvertConditionRefactoring(oldStatement, newStatement, oldEntity, newEntity);
                        refactorings.add(refactoring);
                        invertedFlag = true;
                        break;
                    }
                }
            }
        }
    }

    private StatementNodeTree retrieveSNTByBFS(StatementNodeTree current) {
        Queue<StatementNodeTree> queue = new LinkedList<>(current.getChildren());
        Set<StatementNodeTree> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            StatementNodeTree node = queue.poll();
            if (node.getType() == StatementType.FOR_STATEMENT || node.getType() == StatementType.ENHANCED_FOR_STATEMENT ||
                    node.getType() == StatementType.WHILE_STATEMENT || node.getType() == StatementType.DO_STATEMENT)
                return node;
            for (StatementNodeTree child : node.getChildren()) {
                if (!visited.contains(child)) {
                    queue.add(child);
                    visited.add(child);
                }
            }
        }
        return null;
    }

    private void detectRefactoringsBetweenMatchedAndAddedStatements(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                                                    Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                                                    Set<StatementNodeTree> addedStatements, List<Refactoring> refactorings) {
        for (StatementNodeTree addedStatement : addedStatements) {
            if (addedStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
                checkForExtractVariable(methodNodePairs, matchedStatements, addedStatement, refactorings);
            }
        }

        checkForSplitConditional(methodNodePairs, matchedStatements, addedStatements, refactorings);
    }

    private void checkForExtractVariable(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                         Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                         StatementNodeTree addedStatement, List<Refactoring> refactorings) {
        VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) addedStatement.getStatement();
        List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
        DeclarationNodeTree newEntity = addedStatement.getRoot().getMethodEntity();
        for (VariableDeclarationFragment fragment : fragments) {
            DeclarationNodeTree oldEntity = null;
            Map<StatementNodeTree, StatementNodeTree> references = new TreeMap<>(Comparator.comparingInt(StatementNodeTree::getPosition));
            for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
                StatementNodeTree oldStatement = pair.getLeft();
                StatementNodeTree newStatement = pair.getRight();
                if (!methodNodePairs.contains(Pair.of(oldStatement.getRoot(), addedStatement.getRoot())))
                    continue;
                List<StatementNodeTree> descendants = addedStatement.getParent().getDescendants();
                if (!descendants.contains(newStatement))
                    continue;
                if (newStatement.getPosition() <= addedStatement.getPosition())
                    continue;
                String expression1 = oldStatement.getExpression();
                String expression2 = newStatement.getExpression();
                if (oldEntity == null)
                    oldEntity = oldStatement.getRoot().getMethodEntity();
                if (!expression1.equals(expression2) && contains(newStatement, fragment.getName().getIdentifier()) && fragment.getInitializer() != null &&
                        (newStatement.getChildren().isEmpty() || (!newStatement.getChildren().isEmpty() && expression2.contains(fragment.getName().getIdentifier()))) &&
                        (expression1.contains(fragment.getInitializer().toString()) ||
                                isExtractedFromStatement(expression1, expression2, fragment.getName().getIdentifier(), fragment.getInitializer()) ||
                                DiceFunction.calculateBodyDice(fragment, oldStatement, newStatement) >= DiceFunction.minSimilarity)) {
                    if (isNotExtractedFromStatement(oldStatement, newStatement, variableDeclaration.getType().toString(), fragment.getName().getIdentifier()))
                        references.put(oldStatement, newStatement);
                }
                Expression initializer = fragment.getInitializer();
                if (initializer instanceof ConditionalExpression) {
                    ConditionalExpression conditionalExpression = (ConditionalExpression) initializer;
                    Expression thenExpression = conditionalExpression.getThenExpression();
                    Expression elseExpression = conditionalExpression.getElseExpression();
                    if (contains(newStatement, fragment.getName().getIdentifier()) && !expression1.equals(expression2) &&
                            (newStatement.getChildren().isEmpty() || (!newStatement.getChildren().isEmpty() && expression2.contains(fragment.getName().getIdentifier()))) &&
                            (expression1.replace(thenExpression.toString(), fragment.getName().getIdentifier()).equals(expression2) ||
                                    (expression1.replace(elseExpression.toString(), fragment.getName().getIdentifier()).equals(expression2))))
                        if (isNotExtractedFromStatement(oldStatement, newStatement, variableDeclaration.getType().toString(), fragment.getName().getIdentifier()))
                            references.put(oldStatement, newStatement);
                }
                if (initializer instanceof ClassInstanceCreation) {
                    ClassInstanceCreation creation = (ClassInstanceCreation) initializer;
                    List<Expression> arguments = creation.arguments();
                    if (arguments.size() == 1) {
                        Expression argument = arguments.get(0);
                        if (contains(newStatement, fragment.getName().getIdentifier()) && !expression1.equals(expression2) &&
                                (newStatement.getChildren().isEmpty() || (!newStatement.getChildren().isEmpty() && expression2.contains(fragment.getName().getIdentifier()))) &&
                                expression2.replace(fragment.getName().getIdentifier(), argument.toString()).equals(expression1))
                            if (isNotExtractedFromStatement(oldStatement, newStatement, variableDeclaration.getType().toString(), fragment.getName().getIdentifier()))
                                references.put(oldStatement, newStatement);
                    }
                }
            }
            if (!references.isEmpty()) {
                ExtractVariableRefactoring refactoring = new ExtractVariableRefactoring(fragment, oldEntity, newEntity, references);
                refactorings.add(refactoring);
            }
        }
    }

    private boolean isNotExtractedFromStatement(StatementNodeTree oldStatement, StatementNodeTree newStatement, String type, String name) {
        if (oldStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && newStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
            VariableDeclarationStatement statement1 = (VariableDeclarationStatement) oldStatement.getStatement();
            VariableDeclarationStatement statement2 = (VariableDeclarationStatement) oldStatement.getStatement();
            List<VariableDeclarationFragment> fragments1 = statement1.fragments();
            List<VariableDeclarationFragment> fragments2 = statement2.fragments();
            if (statement1.getType().toString().equals(type) && statement2.getType().toString().equals(type)) {
                for (VariableDeclarationFragment fragment1 : fragments1) {
                    if (fragment1.getName().getIdentifier().equals(name)) {
                        for (VariableDeclarationFragment fragment2 : fragments2) {
                            if (fragment2.getName().getIdentifier().equals(name))
                                return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isExtractedFromStatement(String expression1, String expression2, String name, Expression initializer) {
        if (initializer instanceof MethodInvocation) {
            MethodInvocation invocation = (MethodInvocation) initializer;
            Expression identifier = invocation.getExpression();
            if (identifier != null) {
                String invoker = identifier.toString();
                if (expression1.replace(invoker, name).equals(expression2))
                    return true;
            }
        }
        return false;
    }

    private void checkForSplitConditional(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                          Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                          Set<StatementNodeTree> addedStatements, List<Refactoring> refactorings) {
        for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
            StatementNodeTree oldStatement = pair.getLeft();
            StatementNodeTree newStatement = pair.getRight();
            if (oldStatement.getType() == StatementType.IF_STATEMENT && newStatement.getType() == StatementType.IF_STATEMENT) {
                String expression1 = oldStatement.getExpression();
                String expression2 = newStatement.getExpression();
                Set<StatementNodeTree> splitConditionals = new LinkedHashSet<>();
                DeclarationNodeTree oldEntity = null;
                DeclarationNodeTree newEntity = null;
                for (StatementNodeTree addedStatement : addedStatements) {
                    if (!methodNodePairs.contains(Pair.of(oldStatement.getRoot(), addedStatement.getRoot())))
                        continue;
                    String addedExpression = addedStatement.getExpression();
                    if (!expression1.equals(addedExpression) && (expression1.contains(addedExpression.replace("if(", "")) ||
                            expression1.contains(addedExpression.replace("if(", "").replace(")", ""))) &&
                            !(expression2.contains(addedExpression.replace("if(", ""))
                                    || expression2.contains(addedExpression.replace("if(", "").replace(")", ""))
                                    || expression2.contains(addedExpression.replace(")", "")))) {
                        splitConditionals.add(newStatement);
                        splitConditionals.add(addedStatement);
                        if (oldEntity == null)
                            oldEntity = oldStatement.getRoot().getMethodEntity();
                        if (newEntity == null)
                            newEntity = addedStatement.getRoot().getMethodEntity();
                    }
                }
                if (!splitConditionals.isEmpty()) {
                    SplitConditionalRefactoring refactoring = new SplitConditionalRefactoring(oldStatement, splitConditionals, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
        }
    }

    private void detectRefactoringsBetweenMatchedAndDeletedStatements(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                                                      Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                                                      Set<StatementNodeTree> deletedStatements, List<Refactoring> refactorings) {
        for (StatementNodeTree deletedStatement : deletedStatements) {
            if (deletedStatement.getType() == StatementType.EXPRESSION_STATEMENT) {
                checkForMergeDeclarationAndAssignment(methodNodePairs, deletedStatement, matchedStatements, refactorings);
            }
            if (deletedStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
                checkForInlineVariable(methodNodePairs, deletedStatement, matchedStatements, refactorings);
            }
            if (deletedStatement.getType() == StatementType.IF_STATEMENT) {
                checkForReplaceIfWithTernary(methodNodePairs, deletedStatement, matchedStatements, refactorings);
            }
        }

        checkForMergeConditional(methodNodePairs, matchedStatements, deletedStatements, refactorings);
    }

    private void checkForMergeDeclarationAndAssignment(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                                       StatementNodeTree deletedStatement, Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                                       List<Refactoring> refactorings) {
        List<Assignment> assignments = new ArrayList<>();
        deletedStatement.getStatement().accept(new ASTVisitor() {
            @Override
            public boolean visit(Assignment node) {
                assignments.add(node);
                return true;
            }
        });
        if (assignments.isEmpty())
            return;
        for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
            StatementNodeTree oldStatement = pair.getLeft();
            StatementNodeTree newStatement = pair.getRight();
            if (!methodNodePairs.contains(Pair.of(deletedStatement.getRoot(), newStatement.getRoot())))
                continue;
            if (oldStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT && newStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
                VariableDeclarationStatement statement1 = (VariableDeclarationStatement) oldStatement.getStatement();
                VariableDeclarationStatement statement2 = (VariableDeclarationStatement) newStatement.getStatement();
                if (statement1.toString().equals(statement2.toString()))
                    continue;
                List<VariableDeclarationFragment> fragments1 = statement1.fragments();
                List<VariableDeclarationFragment> fragments2 = statement2.fragments();
                if (fragments1.size() != fragments2.size())
                    return;
                for (Assignment assignment : assignments) {
                    for (int i = 0; i < fragments1.size(); i++) {
                        VariableDeclarationFragment fragment1 = fragments1.get(i);
                        VariableDeclarationFragment fragment2 = fragments2.get(i);
                        if (fragment1.getName().getIdentifier().equals(assignment.getLeftHandSide().toString()) &&
                                fragment2.getInitializer() != null &&
                                fragment2.getInitializer().toString().equals(assignment.getRightHandSide().toString())) {
                            DeclarationNodeTree oldEntity = oldStatement.getRoot().getMethodEntity();
                            DeclarationNodeTree newEntity = newStatement.getRoot().getMethodEntity();
                            MergeDeclarationAndAssignmentRefactoring refactoring = new MergeDeclarationAndAssignmentRefactoring(oldStatement, deletedStatement, newStatement, oldEntity, newEntity);
                            refactorings.add(refactoring);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void checkForInlineVariable(Set<Pair<MethodNode, MethodNode>> methodNodePairs, StatementNodeTree deletedStatement,
                                        Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements, List<Refactoring> refactorings) {
        VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) deletedStatement.getStatement();
        List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
        DeclarationNodeTree oldEntity = deletedStatement.getRoot().getMethodEntity();
        for (VariableDeclarationFragment fragment : fragments) {
            DeclarationNodeTree newEntity = null;
            Map<StatementNodeTree, StatementNodeTree> references = new TreeMap<>(Comparator.comparingInt(StatementNodeTree::getPosition));
            for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
                StatementNodeTree oldStatement = pair.getLeft();
                StatementNodeTree newStatement = pair.getRight();
                if (!methodNodePairs.contains(Pair.of(deletedStatement.getRoot(), newStatement.getRoot())))
                    continue;
                List<StatementNodeTree> descendants = deletedStatement.getParent().getDescendants();
                if (!descendants.contains(oldStatement))
                    continue;
                if (oldStatement.getPosition() <= deletedStatement.getPosition())
                    continue;
                String expression1 = oldStatement.getExpression();
                String expression2 = newStatement.getExpression();
                if (newEntity == null)
                    newEntity = newStatement.getRoot().getMethodEntity();
                String name = fragment.getName().getIdentifier();
                Expression expression = fragment.getInitializer();
                String initializer = expression != null ? expression.toString() : null;
                if (initializer == null && variableDeclaration.getType().toString().equals("boolean"))
                    initializer = "false";
                if (!expression1.equals(expression2) && expression1.contains(name) && contains(oldStatement, name) && initializer != null &&
                        (expression2.contains(initializer) ||
                                DiceFunction.calculateBodyDice(fragment, newStatement, oldStatement) >= DiceFunction.minSimilarity)) {
                    if (expression instanceof ClassInstanceCreation) {
                        ClassInstanceCreation creation = (ClassInstanceCreation) expression;
                        String type = creation.getType().toString();
                        if (expression1.replace(name, type).equals(expression2))
                            continue;
                    }
                    references.put(oldStatement, newStatement);
                }
            }
            if (!references.isEmpty()) {
                InlineVariableRefactoring refactoring = new InlineVariableRefactoring(fragment, oldEntity, newEntity, references);
                refactorings.add(refactoring);
            }
        }
    }

    private void checkForReplaceIfWithTernary(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                              StatementNodeTree deletedStatement, Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                              List<Refactoring> refactorings) {
        for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
            StatementNodeTree oldStatement = pair.getLeft();
            StatementNodeTree newStatement = pair.getRight();
            if (!methodNodePairs.contains(Pair.of(deletedStatement.getRoot(), newStatement.getRoot())))
                continue;
            DeclarationNodeTree oldEntity = oldStatement.getRoot().getMethodEntity();
            DeclarationNodeTree newEntity = newStatement.getRoot().getMethodEntity();
            if (oldStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT &&
                    newStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
                String expression2 = newStatement.getExpression();
                String expression1 = deletedStatement.getExpression();
                expression1 = expression1.substring(expression1.indexOf("if") + 2);
                if ((expression2.contains(expression1 + " ? ") || (expression2.contains(expression1.replace("(", "").replace(")", "") + " ? ")) ||
                        expression2.contains(expression1.replace("==", "!=") + " ? ") ||
                        expression2.contains(expression1.replace("!=", "==") + " ? ") ||
                        expression2.contains(expression1.replace("(", "(!") + " ? ") ||
                        expression2.contains(expression1.replace("(!", "(") + " ? ")) &&
                        expression2.contains(" : ") && expression2.lastIndexOf(expression1 + " ? ") < expression2.lastIndexOf(" : ")) {
                    ReplaceIfElseWithTernaryRefactoring refactoring = new ReplaceIfElseWithTernaryRefactoring(oldStatement, deletedStatement, newStatement, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
        }
    }

    private void checkForMergeConditional(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                          Set<Pair<StatementNodeTree, StatementNodeTree>> matchedStatements,
                                          Set<StatementNodeTree> deletedStatements, List<Refactoring> refactorings) {
        for (Pair<StatementNodeTree, StatementNodeTree> pair : matchedStatements) {
            StatementNodeTree oldStatement = pair.getLeft();
            StatementNodeTree newStatement = pair.getRight();
            if (oldStatement.getType() == StatementType.IF_STATEMENT && newStatement.getType() == StatementType.IF_STATEMENT) {
                String expression1 = oldStatement.getExpression();
                String expression2 = newStatement.getExpression();
                Set<StatementNodeTree> mergedConditionals = new LinkedHashSet<>();
                DeclarationNodeTree oldEntity = null;
                DeclarationNodeTree newEntity = null;
                for (StatementNodeTree deletedStatement : deletedStatements) {
                    if (!methodNodePairs.contains(Pair.of(deletedStatement.getRoot(), newStatement.getRoot())))
                        continue;
                    String deletedExpression = deletedStatement.getExpression();
                    if (!expression2.equals(deletedExpression) && (expression2.contains(deletedExpression.replace("if(", "")) ||
                            expression2.contains(deletedExpression.replace("if(", "").replace(")", ""))) &&
                            !(expression1.contains(deletedExpression.replace("if(", ""))
                                    || expression1.contains(deletedExpression.replace("if(", "").replace(")", ""))
                                    || expression1.contains(deletedExpression.replace(")", "")))) {
                        mergedConditionals.add(oldStatement);
                        mergedConditionals.add(deletedStatement);
                        if (oldEntity == null)
                            oldEntity = deletedStatement.getRoot().getMethodEntity();
                        if (newEntity == null)
                            newEntity = newStatement.getRoot().getMethodEntity();
                    }
                }
                if (!mergedConditionals.isEmpty()) {
                    MergeConditionalRefactoring refactoring = new MergeConditionalRefactoring(mergedConditionals, newStatement, oldEntity, newEntity);
                    refactorings.add(refactoring);
                }
            }
        }
    }

    private void detectRefactoringsBetweenAddedAndDeletedStatements(Set<Pair<MethodNode, MethodNode>> methodNodePairs,
                                                                    Set<StatementNodeTree> addedStatements, Set<StatementNodeTree> deletedStatements,
                                                                    MatchPair matchPair, List<Refactoring> refactorings) {
        for (StatementNodeTree addedStatement : addedStatements) {
            for (StatementNodeTree deletedStatement : deletedStatements) {
                if (!methodNodePairs.contains(Pair.of(deletedStatement.getRoot(), addedStatement.getRoot())))
                    continue;
                if ((deletedStatement.getType() == StatementType.FOR_STATEMENT || deletedStatement.getType() == StatementType.ENHANCED_FOR_STATEMENT ||
                        deletedStatement.getType() == StatementType.WHILE_STATEMENT || deletedStatement.getType() == StatementType.DO_STATEMENT) &&
                        (addedStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT || addedStatement.getType() == StatementType.EXPRESSION_STATEMENT ||
                                addedStatement.getType() == StatementType.RETURN_STATEMENT)) {
                    if (MethodUtils.isStreamAPI(addedStatement.getStatement()) && DiceFunction.calculateSimilarity(matchPair, matchPair, deletedStatement, addedStatement) >= DiceFunction.minSimilarity) {
                        DeclarationNodeTree oldEntity = deletedStatement.getRoot().getMethodEntity();
                        DeclarationNodeTree newEntity = addedStatement.getRoot().getMethodEntity();
                        ReplaceLoopWithPipelineRefactoring refactoring = new ReplaceLoopWithPipelineRefactoring(deletedStatement, addedStatement, oldEntity, newEntity);
                        refactorings.add(refactoring);
                    }
                }

                if ((addedStatement.getType() == StatementType.FOR_STATEMENT || addedStatement.getType() == StatementType.ENHANCED_FOR_STATEMENT ||
                        addedStatement.getType() == StatementType.WHILE_STATEMENT || addedStatement.getType() == StatementType.DO_STATEMENT) &&
                        (deletedStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT || deletedStatement.getType() == StatementType.EXPRESSION_STATEMENT ||
                                deletedStatement.getType() == StatementType.RETURN_STATEMENT)) {
                    if (MethodUtils.isStreamAPI(deletedStatement.getStatement()) && DiceFunction.calculateSimilarity(matchPair, matchPair, deletedStatement, addedStatement) >= DiceFunction.minSimilarity) {
                        DeclarationNodeTree oldEntity = deletedStatement.getRoot().getMethodEntity();
                        DeclarationNodeTree newEntity = addedStatement.getRoot().getMethodEntity();
                        ReplacePipelineWithLoopRefactoring refactoring = new ReplacePipelineWithLoopRefactoring(deletedStatement, addedStatement, oldEntity, newEntity);
                        refactorings.add(refactoring);
                    }
                }
            }

            if (addedStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
                VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) addedStatement.getStatement();
                List<VariableDeclarationFragment> fragments = variableDeclaration.fragments();
                DeclarationNodeTree newEntity = addedStatement.getRoot().getMethodEntity();
                DeclarationNodeTree oldEntity = null;
                for (VariableDeclarationFragment fragment : fragments) {
                    Map<StatementNodeTree, StatementNodeTree> references = new TreeMap<>(Comparator.comparingInt(StatementNodeTree::getPosition));
                    for (StatementNodeTree anotherAddedStatement : addedStatements) {
                        if (addedStatement.getRoot() != anotherAddedStatement.getRoot())
                            continue;
                        if (anotherAddedStatement == addedStatement) continue;
                        for (StatementNodeTree deletedStatement : deletedStatements) {
                            if (!methodNodePairs.contains(Pair.of(deletedStatement.getRoot(), anotherAddedStatement.getRoot())))
                                continue;
                            String expression1 = deletedStatement.getExpression();
                            String expression2 = anotherAddedStatement.getExpression();
                            if (!expression1.equals(expression2) && deletedStatement.getType() == anotherAddedStatement.getType() &&
                                    contains(anotherAddedStatement, fragment.getName().getIdentifier()) && fragment.getInitializer() != null &&
                                    (expression1.contains(fragment.getInitializer().toString()) ||
                                            isSameStatement(expression1, expression2, fragment.getName().getIdentifier(), fragment.getInitializer()))) {
                                boolean notExist = true;
                                if (anotherAddedStatement.getType() == StatementType.VARIABLE_DECLARATION_STATEMENT) {
                                    VariableDeclarationStatement anotherVariable = (VariableDeclarationStatement) anotherAddedStatement.getStatement();
                                    List<VariableDeclarationFragment> anotherFragments = anotherVariable.fragments();
                                    for (VariableDeclarationFragment anotherFragment : anotherFragments) {
                                        if (anotherFragment.getName().getIdentifier().equals(fragment.getName().getIdentifier())) {
                                            notExist = false;
                                            break;
                                        }
                                    }
                                }
                                if (anotherAddedStatement.getType() == StatementType.EXPRESSION_STATEMENT) {
                                    ExpressionStatement statement1 = (ExpressionStatement) deletedStatement.getStatement();
                                    ExpressionStatement statement2 = (ExpressionStatement) anotherAddedStatement.getStatement();
                                    if (statement1.getExpression().getNodeType() != statement2.getExpression().getNodeType())
                                        notExist = false;
                                }
                                if (notExist)
                                    references.put(deletedStatement, anotherAddedStatement);
                                if (oldEntity == null)
                                    oldEntity = deletedStatement.getRoot().getMethodEntity();
                            }
                        }
                    }
                    if (!references.isEmpty()) {
                        ExtractVariableRefactoring refactoring = new ExtractVariableRefactoring(fragment, oldEntity, newEntity, references);
                        refactorings.add(refactoring);
                    }
                }
            }
        }
    }

    private boolean isSameStatement(String expression1, String expression2, String name, Expression expression) {
        if (expression instanceof CastExpression castExpression) {
            String castString = castExpression.getExpression().toString();
            String replacedString = expression.toString();
            String replacedCastString = castString;
            replacedString = expression2.replace(name, replacedString);
            replacedCastString = expression2.replace(name, replacedCastString);
            if (expression1.equals(replacedString) || expression1.equals(replacedCastString))
                return true;
        }
        return false;
    }
}
