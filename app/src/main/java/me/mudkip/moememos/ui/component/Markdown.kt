package me.mudkip.moememos.ui.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import me.mudkip.moememos.ext.appendMarkdown
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    imageContent: @Composable (url: String) -> Unit = {},
    checkboxChange: (checked: Boolean, startOffset: Int, endOffset: Int) -> Unit = { _, _, _ -> }
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val bulletColor = MaterialTheme.colorScheme.tertiary
    val headlineLarge = MaterialTheme.typography.headlineLarge
    val headlineMedium = MaterialTheme.typography.headlineMedium
    val headlineSmall = MaterialTheme.typography.headlineSmall

    BoxWithConstraints {
        val (annotatedString, inlineContent) = remember(text, maxWidth) {
            val markdownAst = MarkdownParser(GFMFlavourDescriptor()).parse(MarkdownElementTypes.MARKDOWN_FILE, text, true)
            val builder = AnnotatedString.Builder()
            val inlineContent = HashMap<String, InlineTextContent>()

            builder.appendMarkdown(
                markdownText = text,
                node = markdownAst,
                depth = 0,
                linkColor = linkColor,
                linkInteractionListener = null, // Use default interaction listener
                onImage = { key, url ->
                    inlineContent[key] = InlineTextContent(
                        Placeholder(maxWidth.value.sp, (maxWidth.value * 9f / 16f).sp, PlaceholderVerticalAlign.AboveBaseline),
                    ) {
                        imageContent(url)
                    }
                },
                onCheckbox = { key, startOffset, endOffset ->
                    inlineContent[key] = InlineTextContent(
                        Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.Center)
                    ) {
                        val checkboxText = text.substring(startOffset, endOffset)
                        Checkbox(checked = checkboxText.length > 1 && checkboxText[1] != ' ', onCheckedChange = {
                            checkboxChange(it, startOffset, endOffset)
                        })
                    }
                },
                maxWidth = maxWidth.value,
                bulletColor = bulletColor,
                headlineLarge = headlineLarge,
                headlineMedium = headlineMedium,
                headlineSmall = headlineSmall
            )

            Pair(builder.toAnnotatedString(), inlineContent)
        }

        Text(
            text = annotatedString,
            modifier = modifier,
            textAlign = textAlign,
            inlineContent = inlineContent,
        )
    }
}