package org.reextractor.util;

import org.remapper.dto.ChildNode;
import org.remapper.dto.LeafNode;
import org.remapper.service.JDTService;
import org.remapper.util.JDTServiceImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiceFunction {

    public static double calculateBodyDice(LeafNode leafAdditional, LeafNode leafRefactored, LeafNode leafOriginal) {
        JDTService jdtService = new JDTServiceImpl();
        List<ChildNode> list1 = leafAdditional.getDescendantsInBody(jdtService);
        List<ChildNode> list2 = leafRefactored.getDescendantsInBody(jdtService);
        List<ChildNode> list3 = leafOriginal.getDescendantsInBody(jdtService);
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
        double bodyDice = list1.size() == 0 ? 0 : 1.0 * intersection / list1.size();
        return bodyDice;
    }
}
