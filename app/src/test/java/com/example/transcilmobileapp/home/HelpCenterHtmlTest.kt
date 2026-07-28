package com.example.transcilmobileapp.home

import com.example.transcilmobileapp.data.model.HelpArticleDto
import com.example.transcilmobileapp.data.model.HelpCenterDto
import com.example.transcilmobileapp.data.model.HelpTopicDto
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpCenterHtmlTest {

    @Test
    fun build_includesSupportAndArticles() {
        val html = HelpCenterHtml.build(
            HelpCenterDto(
                schema_version = "help_center_v1",
                support_email = "info@transcil.com",
                support_mobile = "+918019355656",
                topics = listOf(
                    HelpTopicDto(
                        id = "battery",
                        title = "Battery and swaps",
                        articles = listOf(
                            HelpArticleDto(
                                id = "battery-low",
                                title = "What to do when battery is low",
                                body_html = "<p>Scan QR</p>",
                            )
                        ),
                    )
                ),
                version = 1,
            )
        )

        assertTrue(html.contains("info@transcil.com"))
        assertTrue(html.contains("Battery and swaps"))
        assertTrue(html.contains("What to do when battery is low"))
        assertTrue(html.contains("<p>Scan QR</p>"))
    }
}
