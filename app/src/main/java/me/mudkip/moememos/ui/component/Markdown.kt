package me.mudkip.moememos.ui.component

import androidx.core.net.toUri
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCheckBox
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.m3.Markdown as M3Markdown
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.model.rememberMarkdownState

@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    imageBaseUrl: String? = null,
    checkboxChange: ((checked: Boolean, startOffset: Int, endOffset: Int) -> Unit)? = null,
    selectable: Boolean = false
) {
    val bodyTextStyle = MaterialTheme.typography.bodyLarge.let {
        if (textAlign == null) it else it.copy(textAlign = textAlign)
    }
    val imageTransformer = remember(imageBaseUrl) {
        object : ImageTransformer {
            @Composable
            override fun transform(link: String): ImageData {
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

    val markdownContent: @Composable () -> Unit = {
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
            annotator = markdownAnnotator(
                config = markdownAnnotatorConfig(eolAsNewLine = true)
            ),
            components = markdownComponents(
                codeFence = highlightedCodeFence,
                codeBlock = highlightedCodeBlock,
                checkbox = {
                    val node = it.node
                    MarkdownCheckBox(
                        content = it.content,
                        node = it.node,
                        style = it.typography.text,
                        checkedIndicator = { checked, modifier ->
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = if (checkboxChange != null) {
                                        { checkboxChange(!checked, node.startOffset, node.endOffset) }
                                    } else {
                                        null
                                    },
                                    modifier = modifier.semantics {
                                        role = Role.Checkbox
                                        stateDescription = if (checked) "Checked" else "Unchecked"
                                    },
                                )
                            }
                        }
                    )
                }
            )
        )
    }

    if (selectable) {
        SelectionContainer {
            markdownContent()
        }
    } else {
        markdownContent()
    }
}

private fun resolveMarkdownImageLink(link: String, imageBaseUrl: String?): String {
    val uri = link.toUri()
    if (uri.scheme != null || imageBaseUrl.isNullOrBlank()) {
        return link
    }
    return imageBaseUrl.toUri().buildUpon().path(link).build().toString()
}
