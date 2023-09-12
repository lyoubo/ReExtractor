package org.reextractor.util;

import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.remapper.dto.DeclarationNodeTree;
import org.remapper.dto.EntityType;

public class AttributeUtils {

    public static String getVariableDeclaration(DeclarationNodeTree attribute) {
        StringBuilder sb = new StringBuilder();
        if (attribute.getType() == EntityType.FIELD) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) attribute.getDeclaration();
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) fieldDeclaration.fragments().get(0);
            sb.append(fragment.getName().getIdentifier());
            sb.append(" : ");
            sb.append(fieldDeclaration.getType().toString());
        } else if (attribute.getType() == EntityType.ENUM_CONSTANT) {
            EnumConstantDeclaration enumConstantDeclaration = (EnumConstantDeclaration) attribute.getDeclaration();
            sb.append(enumConstantDeclaration.getName().getIdentifier());
            sb.append(" : ");
            sb.append(attribute.getParent().getName());
        }
        return sb.toString();
    }
}
