package com.example.transcilmobileapp.data.model

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
