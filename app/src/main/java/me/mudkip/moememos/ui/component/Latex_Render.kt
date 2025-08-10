package me.mudkip.moememos.ui.component

fun createLatexHtml(text: String): String {
    // Escape HTML characters in text
    val escapedText = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
    
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
            <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
            <script>
                window.MathJax = {
                    tex: {
                        inlineMath: [['$', '$'], ['\\(', '\\)']],
                        displayMath: [['$$', '$$'], ['\\[', '\\]']]
                    },
                    chtml: {
                        scale: 1,
                        minScale: 0.5,
                        matchFontHeight: false,
                        displayAlign: 'left',
                        displayIndent: '0'
                    },
                    options: {
                        skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre']
                    },
                    startup: {
                        ready: function() {
                            MathJax.startup.defaultReady();
                            // Trigger resize after rendering
                            MathJax.startup.promise.then(function() {
                                if (window.Android && window.Android.onMathJaxReady) {
                                    window.Android.onMathJaxReady();
                                }
                            });
                        }
                    }
                };
            </script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    font-size: 16px;
                    line-height: 1.5;
                    margin: 0;
                    padding: 0;
                    background-color: transparent;
                    color: #333;
                }
                .math-container {
                    padding: 0;
                    background-color: transparent;
                }
            </style>
        </head>
        <body>
            <div class="math-container">
                $escapedText
            </div>
        </body>
        </html>
    """.trimIndent()
}
