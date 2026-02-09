package me.mudkip.moememos.ui.component

import androidx.core.net.toUri
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown as M3Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.rememberMarkdownState
import org.intellij.markdown.ast.getTextInNode

@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    imageBaseUrl: String? = null,
    checkboxChange: (checked: Boolean, startOffset: Int, endOffset: Int) -> Unit = { _, _, _ -> }
) {
    val bodyTextStyle = MaterialTheme.typography.bodyLarge.let {
        if (textAlign == null) it else it.copy(textAlign = textAlign)
    }
    val imageTransformer = remember(imageBaseUrl) {
        object : ImageTransformer {
            @Composable
            override fun transform(link: String): ImageData? {
                return Coil3ImageTransformerImpl.transform(resolveMarkdownImageLink(link, imageBaseUrl))
            }

            @Composable
            override fun intrinsicSize(painter: Painter): Size {
                return Coil3ImageTransformerImpl.intrinsicSize(painter)
            }
        }
    }
    val markdownState = rememberMarkdownState(
        content = text,
        retainState = true
    )

    M3Markdown(
        markdownState = markdownState,
        modifier = modifier,
        imageTransformer = imageTransformer,
        typography = markdownTypography(
            text = bodyTextStyle,
            paragraph = bodyTextStyle,
            ordered = bodyTextStyle,
            bullet = bodyTextStyle,
            list = bodyTextStyle
        ),
        components = markdownComponents(
            codeFence = highlightedCodeFence,
            codeBlock = highlightedCodeBlock,
            checkbox = { model ->
                val checkboxText = model.node.getTextInNode(model.content).toString()
                val checked = checkboxText.length > 1 && checkboxText[1] != ' '
                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        checkboxChange(it, model.node.startOffset, model.node.endOffset)
                    }
                )
            }
        )
    )
}

private fun resolveMarkdownImageLink(link: String, imageBaseUrl: String?): String {
    val uri = link.toUri()
    if (uri.scheme != null || imageBaseUrl.isNullOrBlank()) {
        return link
    }
    return imageBaseUrl.toUri().buildUpon().path(link).build().toString()
}
