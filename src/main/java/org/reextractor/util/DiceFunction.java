package org.reextractor.util;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.remapper.dto.ChildNode;
import org.remapper.dto.LeafNode;
import org.remapper.dto.StatementNodeTree;
import org.remapper.service.JDTService;
import org.remapper.util.JDTServiceImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiceFunction extends org.remapper.util.DiceFunction {

    public static double calculateBodyDice(LeafNode leafAdditional, LeafNode leafRefactored, LeafNode leafOriginal) {
        JDTService jdtService = new JDTServiceImpl();
        MethodDeclaration additionalDeclaration = (MethodDeclaration) leafAdditional.getDeclaration();
        Block additionalBody = additionalDeclaration.getBody();
        List<ChildNode> list1 = jdtService.getDescendants(additionalBody);
        MethodDeclaration refactoredDeclaration = (MethodDeclaration) leafRefactored.getDeclaration();
        Block refactoredBody = refactoredDeclaration.getBody();
        List<ChildNode> list2 = jdtService.getDescendants(refactoredBody);
        MethodDeclaration originalDeclaration = (MethodDeclaration) leafOriginal.getDeclaration();
        Block originalBody = originalDeclaration.getBody();
        List<ChildNode> list3 = jdtService.getDescendants(originalBody);
        return calculateBodyDice(list1, list2, list3);
    }

    public static double calculateBodyDice(List<ChildNode> list1, List<ChildNode> list2, List<ChildNode> list3) {
        int intersection = 0;
        Set<Integer> matched = new HashSet<>();
        for (ChildNode childBefore : list2) {
            for (int i = 0; i < list3.size(); i++) {
                if (matched.contains(i)) continue;
                ChildNode childCurrent = list3.get(i);
                if (childBefore.equals(childCurrent)) {
                    matched.add(i);
                    break;
                }
            }
        }
        List<ChildNode> temp = new ArrayList<>();
        for (int i : matched) {
            temp.add(list3.get(i));
        }
        list3.removeAll(temp);
        Set<Integer> matched1 = new HashSet<>();
        for (ChildNode childBefore : list1) {
            for (int i = 0; i < list3.size(); i++) {
                if (matched1.contains(i)) continue;
                ChildNode childCurrent = list3.get(i);
                if (childBefore.equals(childCurrent)) {
                    intersection++;
                    matched1.add(i);
                    break;
                }
            }
        }
        return list1.isEmpty() ? 0 : 1.0 * intersection / list1.size();
    }

    public static double calculateBodyDice(VariableDeclarationFragment fragment, StatementNodeTree leafRefactored, StatementNodeTree leafOriginal) {
        JDTService jdtService = new JDTServiceImpl();
        List<ChildNode> list1 = jdtService.getDescendants(fragment.getInitializer());
        List<ChildNode> list2 = jdtService.getDescendants(leafRefactored.getStatement());
        List<ChildNode> list3 = jdtService.getDescendants(leafOriginal.getStatement());
        return calculateBodyDice(list1, list2, list3);
    }
}
