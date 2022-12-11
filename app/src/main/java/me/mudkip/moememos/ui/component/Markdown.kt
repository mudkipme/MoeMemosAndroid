package me.mudkip.moememos.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import me.mudkip.moememos.ext.appendMarkdown
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

@OptIn(ExperimentalTextApi::class)
@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current

    val annotatedString = remember(text) {
        val markdownAst = MarkdownParser(GFMFlavourDescriptor()).parse(MarkdownElementTypes.MARKDOWN_FILE, text, true)
        val builder = AnnotatedString.Builder()

        builder.appendMarkdown(
            markdownText = text,
            node = markdownAst,
            depth = 0,
            linkColor = linkColor
        )
        builder.toAnnotatedString()
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        onClick = {
            annotatedString.getUrlAnnotations(it, it)
                .firstOrNull()?.let { url ->
                    uriHandler.openUri(url.item.url)
                }
        }
    )
}

@Composable
fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
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
        onTextLayout = {
            layoutResult.value = it
        }
    )
}