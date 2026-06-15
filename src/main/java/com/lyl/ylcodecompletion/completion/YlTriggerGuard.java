package com.lyl.ylcodecompletion.completion;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.lyl.ylcodecompletion.settings.YlCompletionSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class YlTriggerGuard {

    private static final int MAX_DOCUMENT_LEN = 2_000_000;

    private YlTriggerGuard() {
    }

    /**
     * 在请求前做一系列前置检查。返回 true 表示允许触发。
     */
    public static boolean shouldTrigger(
            @NotNull Editor editor,
            @Nullable PsiFile psiFile,
            @NotNull YlCompletionSettingsState settings
    ) {
        if (!settings.enabled) return false;
        if (psiFile == null) return false;

        Document doc = editor.getDocument();
        if (!doc.isWritable()) return false;
        if (doc.getTextLength() > MAX_DOCUMENT_LEN) return false;

        Project project = editor.getProject();
        if (project == null || project.isDisposed()) return false;
        if (DumbService.isDumb(project)) return false;

        VirtualFile vf = psiFile.getVirtualFile();
        if (vf == null) return false;
        if (FileTypeRegistry.getInstance().isFileOfType(vf, UnknownFileType.INSTANCE)) return false;
        if (isExtensionDisabled(vf, settings.disabledExtensions)) return false;

        CaretModel cm = editor.getCaretModel();
        if (cm.getCaretCount() != 1) return false;
        Caret caret = cm.getPrimaryCaret();
        if (caret.hasSelection()) return false;

        int offset = caret.getOffset();
        int linePrefixLen = currentLinePrefixLength(doc, offset);
        if (linePrefixLen < settings.triggerMinPrefixLength) return false;

        if (LookupManager.getActiveLookup(editor) != null) return false;
        TemplateManager tm = TemplateManager.getInstance(project);
        if (tm.getActiveTemplate(editor) != null) return false;

        return true;
    }

    private static int currentLinePrefixLength(@NotNull Document doc, int offset) {
        int line = doc.getLineNumber(offset);
        int lineStart = doc.getLineStartOffset(line);
        return offset - lineStart;
    }

    private static boolean isExtensionDisabled(@NotNull VirtualFile vf, @Nullable String raw) {
        if (raw == null || raw.isBlank()) return false;
        String ext = vf.getExtension();
        if (ext == null) return false;
        Set<String> disabled = new HashSet<>();
        for (String s : raw.split(",")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                disabled.add(t.startsWith(".") ? t.substring(1) : t);
            }
        }
        return disabled.contains(ext.toLowerCase(Locale.ROOT));
    }

}
