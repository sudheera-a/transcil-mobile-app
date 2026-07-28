package com.example.transcilmobileapp.home

import com.example.transcilmobileapp.data.model.HelpCenterDto

/** Flattens help-center JSON into one HTML document for WebView. */
object HelpCenterHtml {

    fun build(dto: HelpCenterDto): String {
        val sb = StringBuilder()
        dto.support_email?.takeIf { it.isNotBlank() }?.let {
            sb.append("<p><b>Email:</b> ").append(it).append("</p>")
        }
        dto.support_mobile?.takeIf { it.isNotBlank() }?.let {
            sb.append("<p><b>Mobile:</b> ").append(it).append("</p>")
        }
        dto.topics.orEmpty().forEach { topic ->
            topic.title?.takeIf { it.isNotBlank() }?.let {
                sb.append("<h2>").append(it).append("</h2>")
            }
            topic.articles.orEmpty().forEach { article ->
                article.title?.takeIf { it.isNotBlank() }?.let {
                    sb.append("<h3>").append(it).append("</h3>")
                }
                sb.append(article.body_html.orEmpty())
            }
        }
        return sb.toString()
    }
}
