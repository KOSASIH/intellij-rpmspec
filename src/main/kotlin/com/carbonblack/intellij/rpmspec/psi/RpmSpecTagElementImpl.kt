package com.carbonblack.intellij.rpmspec.psi

import com.carbonblack.intellij.rpmspec.RpmSpecReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

abstract class RpmSpecTagElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), RpmSpecTagElement {
    override fun getNameIdentifier(): PsiElement? =
        node.findChildByType(RpmSpecTypes.PREAMBLE_TAG)?.psi


    override fun setName(name: String): PsiElement {
        val keyNode = node.findChildByType(RpmSpecTypes.PREAMBLE_TAG)

        if (keyNode != null) {
            val macro = RpmSpecElementFactory.createTag(project, name)
            val newKeyNode = macro.firstChild.node
            node.replaceChild(keyNode, newKeyNode)
        }
        return this
    }

    override fun getName(): String {
        val valueNode = node.findChildByType(RpmSpecTypes.PREAMBLE_TAG)
        return valueNode!!.text
    }

    override fun getReference(): PsiReference {
        return RpmSpecReference(this, TextRange(0, name.length))
    }
}