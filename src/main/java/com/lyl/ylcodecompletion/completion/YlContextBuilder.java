package com.lyl.ylcodecompletion.completion;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public final class YlContextBuilder {

    private static final double PREFIX_RATIO = 0.7;

    private YlContextBuilder() {
    }

    public static @NotNull YlCompletionContext build(
            @NotNull Editor editor,
            @NotNull PsiFile psiFile,
            int caretOffset,
            int budget
    ) {
        Document doc = editor.getDocument();
        String text = doc.getText();
        int len = text.length();
        int caret = Math.max(0, Math.min(caretOffset, len));

        int budgetSafe = Math.max(200, budget);
        int prefixBudget = (int) Math.round(budgetSafe * PREFIX_RATIO);
        int suffixBudget = budgetSafe - prefixBudget;

        int prefixStart = Math.max(0, caret - prefixBudget);
        prefixStart = avoidSurrogateSplit(text, prefixStart, true);
        String prefix = text.substring(prefixStart, caret);

        int suffixEnd = Math.min(len, caret + suffixBudget);
        suffixEnd = avoidSurrogateSplit(text, suffixEnd, false);
        String suffix = text.substring(caret, suffixEnd);

        VirtualFile vf = psiFile.getVirtualFile();
        String filePath = vf == null ? psiFile.getName() : vf.getPath();
        String languageId = psiFile.getLanguage().getID();

        return new YlCompletionContext(filePath, languageId, prefix, suffix, caret);
    }

    private static int avoidSurrogateSplit(@NotNull String text, int idx, boolean shrinkLeft) {
        if (idx <= 0 || idx >= text.length()) return idx;
        char c = text.charAt(idx);
        char prev = text.charAt(idx - 1);
        if (Character.isLowSurrogate(c) && Character.isHighSurrogate(prev)) {
            return shrinkLeft ? idx + 1 : idx - 1;
        }
        return idx;
    }
}
