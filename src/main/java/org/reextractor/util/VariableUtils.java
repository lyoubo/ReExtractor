package org.reextractor.util;

import org.eclipse.jdt.core.dom.*;

public class VariableUtils {

    public static String getVariableDeclaration(VariableDeclaration variable) {
        StringBuilder sb = new StringBuilder();
        sb.append(variable.getName().getIdentifier());
        sb.append(" : ");
        if (variable instanceof SingleVariableDeclaration) {
            sb.append(((SingleVariableDeclaration) variable).getType().toString());
        } else {
            ASTNode parent = variable.getParent();
            if (parent instanceof VariableDeclarationStatement) {
                sb.append(((VariableDeclarationStatement) parent).getType().toString());
            } else if (parent instanceof VariableDeclarationExpression) {
                sb.append(((VariableDeclarationExpression) parent).getType().toString());
            }
        }
        return sb.toString();
    }
}
