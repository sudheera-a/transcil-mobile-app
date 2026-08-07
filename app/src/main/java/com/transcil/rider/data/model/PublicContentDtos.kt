/**
 * DTOs for public (often unauthenticated) content endpoints: HTML pages and help center structure.
 * Consumed by home/help fragments; field names match server JSON (some snake_case left as-is).
 *
 * Kotlin notes:
 * - `data class` per resource shape; nullable fields tolerate partial API responses.
 * - Nested DTOs (HelpTopicDto → HelpArticleDto) mirror the JSON tree Gson expects.
 */
package com.transcil.rider.data.model

data class HtmlDocumentDto(
    val html: String?,
)

data class HelpCenterDto(
    val schema_version: String?,
    val support_email: String?,
    val support_mobile: String?,
    val topics: List<HelpTopicDto>?,
    val version: Int?,
)

data class HelpTopicDto(
    val id: String?,
    val title: String?,
    val articles: List<HelpArticleDto>?,
)

data class HelpArticleDto(
    val id: String?,
    val title: String?,
    val body_html: String?,
)
