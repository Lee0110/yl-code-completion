package com.lyl.ylcodecompletion.ghost

import com.intellij.codeInsight.inline.completion.InlineCompletion
import com.intellij.codeInsight.inline.completion.InlineCompletionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

@Suppress("DEPRECATION")
class YlTriggerInlineCompletionAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabled = editor != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val caret = editor.caretModel.primaryCaret
        val handler = InlineCompletion.getHandlerOrNull(editor) ?: return
        handler.invoke(InlineCompletionEvent.DirectCall(editor, caret, e.dataContext))
    }
}
