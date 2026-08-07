/**
 * Flattens help-center JSON into one HTML document for WebView.
 */
package com.transcil.rider.home

import com.transcil.rider.data.model.HelpCenterDto

object HelpCenterHtml {

    fun build(dto: HelpCenterDto): String {
        val sb = StringBuilder()
        dto.support_email?.takeIf { it.isNotBlank() }?.let {
            sb.append("<p><b>Email:</b> ").append(escape(it)).append("</p>")
        }
        dto.support_mobile?.takeIf { it.isNotBlank() }?.let {
            sb.append("<p><b>Mobile:</b> ").append(escape(it)).append("</p>")
        }
        dto.topics.orEmpty().forEach { topic ->
            topic.title?.takeIf { it.isNotBlank() }?.let {
                sb.append("<h2>").append(escape(it)).append("</h2>")
            }
            topic.articles.orEmpty().forEach { article ->
                article.title?.takeIf { it.isNotBlank() }?.let {
                    sb.append("<h3>").append(escape(it)).append("</h3>")
                }
                // Trusted server HTML by product choice for this hardening pass.
                sb.append(article.body_html.orEmpty())
            }
        }
        return sb.toString()
    }

    private fun escape(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
