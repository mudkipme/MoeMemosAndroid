package me.mudkip.moememos.ui.component

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    // Check if content contains #nsfw tag
    val hasNsfwTag = remember(text) {
        text.contains("#nsfw", ignoreCase = true)
    }
    
    var isBlurred by remember(text) { mutableStateOf(hasNsfwTag) }

    BoxWithConstraints {
        // Check if text contains LaTeX expressions
        val hasLatex = remember(text) {
            text.contains("$$") || text.contains("$") || text.contains("\\(") || text.contains("\\[")
        }
        
        Box(
            modifier = if (isBlurred) {
                modifier.clickable { isBlurred = false }
            } else {
                modifier
            }
        ) {
            if (hasLatex) {
                // Use WebView for LaTeX rendering
                WebViewLatexRenderer(
                    text = text,
                    modifier = if (isBlurred) Modifier.blur(10.dp) else Modifier
                )
            } else {
                // Use standard markdown processing for non-LaTeX content
                val (annotatedString, inlineContent) = remember(text, this@BoxWithConstraints.maxWidth) {
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
                            Placeholder(this@BoxWithConstraints.maxWidth.value.sp, (this@BoxWithConstraints.maxWidth.value * 9f / 16f).sp, PlaceholderVerticalAlign.AboveBaseline),
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
                    maxWidth = this@BoxWithConstraints.maxWidth.value,
                    bulletColor = bulletColor,
                    headlineLarge = headlineLarge,
                    headlineMedium = headlineMedium,
                    headlineSmall = headlineSmall
                )

                Pair(builder.toAnnotatedString(), inlineContent)
            }

                Text(
                    text = annotatedString,
                    modifier = if (isBlurred) Modifier.blur(10.dp) else Modifier,
                    textAlign = textAlign,
                    inlineContent = inlineContent,
                )
            }
        }
    }
}

@Composable
fun WebViewLatexRenderer(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewHeight by remember { mutableStateOf(50.dp) }
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Wait a bit for MathJax to finish rendering, then measure content
                        view?.postDelayed({
                            view.evaluateJavascript(
                                "(function() { " +
                                "   var mathElement = document.querySelector('.MathJax, .mjx-chtml, mjx-container');" +
                                "   if (mathElement) return mathElement.offsetHeight;" +
                                "   return document.body.scrollHeight;" +
                                "})();"
                            ) { height ->
                                try {
                                    val contentHeight = height.replace("\"", "").toFloatOrNull()
                                        ?.let { it / context.resources.displayMetrics.density }
                                    if (contentHeight != null && contentHeight > 10) {
                                        webViewHeight = (contentHeight + 4).dp // Minimal padding for rendered formula
                                    }
                                } catch (e: Exception) {
                                    // Fallback if measurement fails
                                    webViewHeight = 50.dp
                                }
                            }
                        }, 500) // Wait 500ms for MathJax rendering
                    }
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(webViewHeight)
            .background(Color.Transparent),
        update = { webView ->
            val htmlContent = createLatexHtml(text)
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}