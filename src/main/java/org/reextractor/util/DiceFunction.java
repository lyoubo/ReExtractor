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

    public static double calculateBodyDice(LeafNode oldNode, LeafNode newNode, LeafNode anotherNode) {
        JDTService jdtService = new JDTServiceImpl();
        MethodDeclaration oldDeclaration = (MethodDeclaration) oldNode.getDeclaration();
        Block oldBody = oldDeclaration.getBody();
        List<ChildNode> list1 = jdtService.getDescendants(oldBody);
        MethodDeclaration newDeclaration = (MethodDeclaration) newNode.getDeclaration();
        Block newBody = newDeclaration.getBody();
        List<ChildNode> list2 = jdtService.getDescendants(newBody);
        MethodDeclaration anotherDeclaration = (MethodDeclaration) anotherNode.getDeclaration();
        Block anotherBody = anotherDeclaration.getBody();
        List<ChildNode> list3 = jdtService.getDescendants(anotherBody);
        return calculateBodyDice(list1, list2, list3);
    }

    public static double calculateBodyDice(List<ChildNode> list1, List<ChildNode> list2, List<ChildNode> list3) {
        int intersection = 0;
        Set<Integer> matched = new HashSet<>();
        for (ChildNode childBefore : list2) {
            for (int i = 0; i < list1.size(); i++) {
                if (matched.contains(i)) continue;
                ChildNode childCurrent = list1.get(i);
                if (childBefore.equals(childCurrent)) {
                    matched.add(i);
                    break;
                }
            }
        }
        List<ChildNode> temp = new ArrayList<>();
        for (int i : matched) {
            temp.add(list1.get(i));
        }
        list1.removeAll(temp);
        Set<Integer> matched1 = new HashSet<>();
        for (ChildNode childBefore : list3) {
            for (int i = 0; i < list1.size(); i++) {
                if (matched1.contains(i)) continue;
                ChildNode childCurrent = list1.get(i);
                if (childBefore.equals(childCurrent)) {
                    intersection++;
                    matched1.add(i);
                    break;
                }
            }
        }
        return list3.isEmpty() ? 0 : 1.0 * intersection / list3.size();
    }

    public static double calculateBodyDice(VariableDeclarationFragment fragment, StatementNodeTree oldStatement, StatementNodeTree newStatement) {
        JDTService jdtService = new JDTServiceImpl();
        List<ChildNode> list1 = jdtService.getDescendants(fragment.getInitializer());
        List<ChildNode> list2 = jdtService.getDescendants(newStatement.getStatement());
        List<ChildNode> list3 = jdtService.getDescendants(oldStatement.getStatement());
        return calculateBodyDice(list1, list2, list3);
    }
}
