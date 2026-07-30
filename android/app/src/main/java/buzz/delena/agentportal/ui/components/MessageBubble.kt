package buzz.delena.agentportal.ui.components

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j

/** One chat message row: user bubbles are plain text and right-aligned; assistant bubbles render markdown/code and are left-aligned. */
@Composable
fun MessageBubble(
    isUser: Boolean,
    contentMarkdown: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isUser) ApColors.AccentSoft else ApColors.Surface
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (isUser) {
                Text(
                    text = contentMarkdown,
                    color = ApColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                MarkdownContent(markdown = contentMarkdown)
            }
        }
        Text(
            text = timeLabel,
            color = ApColors.TextMuted,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
        )
    }
}

/** Grammar-less locator: see the comment where this is used in [MarkdownContent]. */
private object NoOpGrammarLocator : GrammarLocator {
    override fun grammar(prism4j: Prism4j, language: String): Prism4j.Grammar? = null
    override fun languages(): MutableSet<String> = mutableSetOf()
}

@Composable
private fun MarkdownContent(markdown: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val textColorArgb = ApColors.TextPrimary.toArgb()
    val linkColorArgb = ApColors.Accent.toArgb()
    val markwon = remember(context) {
        // No prism4j-bundler annotation processor is wired into the Gradle module (would need a
        // build.gradle.kts edit this UI-layer task must not make), so there is no generated
        // GrammarLocator with real language grammars. NoOpGrammarLocator makes token-level
        // syntax highlighting degrade gracefully to plain (still monospace-styled) code blocks
        // instead of crashing Prism4j's constructor, which requires a non-null locator.
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(SyntaxHighlightPlugin.create(Prism4j(NoOpGrammarLocator), Prism4jThemeDarkula.create()))
            .build()
    }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColorArgb)
                setLinkTextColor(linkColorArgb)
                textSize = 15f
            }
        },
        update = { textView ->
            runCatching { markwon.setMarkdown(textView, markdown) }
                .onFailure { textView.text = markdown }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun MessageBubblePreview() {
    AgentPortalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            MessageBubble(
                isUser = true,
                contentMarkdown = "Can you refactor the auth middleware to use async/await?",
                timeLabel = "10:02 AM",
            )
            MessageBubble(
                isUser = false,
                contentMarkdown = "Sure — here's the plan:\n\n1. Convert `authMiddleware` to `async`\n2. Replace callback chain with `await`\n3. Add a `try/catch`\n\n```ts\nasync function authMiddleware(req, res, next) {\n  try {\n    await verify(req.token)\n    next()\n  } catch (e) {\n    next(e)\n  }\n}\n```",
                timeLabel = "10:02 AM",
            )
        }
    }
}
