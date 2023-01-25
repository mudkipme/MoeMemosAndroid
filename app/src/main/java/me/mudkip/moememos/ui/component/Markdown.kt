package me.mudkip.moememos.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import me.mudkip.moememos.ext.appendMarkdown
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

@OptIn(ExperimentalTextApi::class)
@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    imageContent: @Composable (url: String) -> Unit,
    checkboxChange: (checked: Boolean, startOffset: Int, endOffset: Int) -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val bulletColor = MaterialTheme.colorScheme.tertiary
    val headlineLarge = MaterialTheme.typography.headlineLarge
    val headlineMedium = MaterialTheme.typography.headlineMedium
    val headlineSmall = MaterialTheme.typography.headlineSmall
    val uriHandler = LocalUriHandler.current

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

        ClickableText(
            text = annotatedString,
            modifier = modifier,
            inlineContent = inlineContent,
            onClick = {
                annotatedString.getUrlAnnotations(it, it)
                    .firstOrNull()?.let { url ->
                        uriHandler.openUri(url.item.url)
                    }
            }
        )
    }


}

@Composable
fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onClick: (Int) -> Unit
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            layoutResult.value?.let { layoutResult ->
                onClick(layoutResult.getOffsetForPosition(pos))
            }
        }
    }

    Text(
        text = text,
        modifier = modifier.then(pressIndicator),
        inlineContent = inlineContent,
        onTextLayout = {
            layoutResult.value = it
        }
    )
}