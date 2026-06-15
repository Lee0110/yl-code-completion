package com.lyl.ylcodecompletion.ghost

import com.intellij.codeInsight.inline.completion.InlineCompletionEvent
import com.intellij.codeInsight.inline.completion.InlineCompletionProvider
import com.intellij.codeInsight.inline.completion.InlineCompletionProviderID
import com.intellij.codeInsight.inline.completion.InlineCompletionRequest
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSingleSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.lyl.ylcodecompletion.completion.SuffixDeduplicator
import com.lyl.ylcodecompletion.completion.YlCompletionContext
import com.lyl.ylcodecompletion.completion.YlContextBuilder
import com.lyl.ylcodecompletion.completion.YlTriggerGuard
import com.lyl.ylcodecompletion.llm.DeepSeekFimClient
import com.lyl.ylcodecompletion.llm.LlmException
import com.lyl.ylcodecompletion.llm.LlmRequest
import com.lyl.ylcodecompletion.settings.YlCompletionSettingsState
import com.lyl.ylcodecompletion.settings.YlPasswordSafe
import com.lyl.ylcodecompletion.status.YlBusyState
import kotlinx.coroutines.future.await
import java.util.concurrent.atomic.AtomicBoolean

class YlInlineCompletionProvider : InlineCompletionProvider {

    companion object {
        private val LOG = Logger.getInstance(YlInlineCompletionProvider::class.java)
        private val AUTH_WARNED = AtomicBoolean(false)
    }

    override val id: InlineCompletionProviderID =
        InlineCompletionProviderID("com.lyl.ylcodecompletion")

    override fun isEnabled(event: InlineCompletionEvent): Boolean {
        val settings = YlCompletionSettingsState.getInstance()
        if (!settings.enabled) return false
        return when (event) {
            is InlineCompletionEvent.DocumentChange,
            is InlineCompletionEvent.DirectCall,
            is InlineCompletionEvent.LookupChange -> true
            else -> false
        }
    }

    override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
        val settings = YlCompletionSettingsState.getInstance()
        val editor = request.editor
        val psiFile = request.file
        val caret = request.endOffset

        val allow = ReadAction.compute<Boolean, RuntimeException> {
            YlTriggerGuard.shouldTrigger(editor, psiFile, settings)
        }
        if (!allow) return InlineCompletionSuggestion.Empty

        val ctx: YlCompletionContext = ReadAction.compute<YlCompletionContext, RuntimeException> {
            YlContextBuilder.build(editor, psiFile, caret, settings.contextMaxChars)
        }

        val apiKey = YlPasswordSafe.loadApiKey()
        if (apiKey.isNullOrBlank()) {
            warnAuthOnce("API key is empty; open settings to configure")
            return InlineCompletionSuggestion.Empty
        }

        val busy = YlBusyState.getInstance()
        if (!busy.tryStart()) {
            LOG.debug("Drop completion request: another in flight")
            return InlineCompletionSuggestion.Empty
        }

        val llmRequest = LlmRequest(
            settings.model,
            ctx.prefix(),
            ctx.suffix().ifEmpty { null },
            settings.maxTokens,
            settings.temperature,
            settings.topP,
            listOf("\n\n")
        )

        val response = try {
            DeepSeekFimClient.getInstance()
                .complete(llmRequest, apiKey, settings.baseUrl, settings.timeoutMs)
                .await()
        } catch (e: LlmException) {
            busy.finishError()
            handleLlmError(e)
            return InlineCompletionSuggestion.Empty
        } catch (e: Exception) {
            busy.finishError()
            LOG.debug("Inline completion request failed", e)
            return InlineCompletionSuggestion.Empty
        }
        busy.finishOk()

        val raw = response.text
        if (raw.isBlank()) return InlineCompletionSuggestion.Empty

        val deduped = SuffixDeduplicator.dedup(raw, ctx.suffix()) ?: return InlineCompletionSuggestion.Empty
        if (deduped.isBlank()) return InlineCompletionSuggestion.Empty

        LOG.debug("Inline completion suggestion length=${deduped.length}, finish=${response.finishReason}")

        return InlineCompletionSingleSuggestion.build(request) {
            emit(InlineCompletionGrayTextElement(deduped))
        }
    }

    private fun handleLlmError(e: LlmException) {
        when (e.kind) {
            LlmException.Kind.AUTH -> warnAuthOnce(e.message ?: "Authentication failed")
            LlmException.Kind.TIMEOUT,
            LlmException.Kind.NETWORK -> LOG.debug("Inline completion network/timeout: ${e.message}")
            else -> LOG.warn("Inline completion error: ${e.kind} ${e.message}")
        }
    }

    private fun warnAuthOnce(msg: String) {
        if (AUTH_WARNED.compareAndSet(false, true)) {
            LOG.warn("YL Code Completion auth issue: $msg")
        } else {
            LOG.debug("YL Code Completion auth issue (suppressed): $msg")
        }
    }
}
