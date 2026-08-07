/**
 * Unit tests for [HelpCenterHtml.build]: support contact lines, articles, and HTML escaping.
 */
package com.transcil.rider.home

import com.transcil.rider.data.model.HelpArticleDto
import com.transcil.rider.data.model.HelpCenterDto
import com.transcil.rider.data.model.HelpTopicDto
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

    @Test
    fun build_escapesScriptInTitle() {
        val html = HelpCenterHtml.build(
            HelpCenterDto(
                schema_version = "help_center_v1",
                support_email = "a<script>x</script>@t.com",
                support_mobile = null,
                topics = listOf(
                    HelpTopicDto(
                        id = "t",
                        title = "<script>alert(1)</script>",
                        articles = listOf(
                            HelpArticleDto(
                                id = "a",
                                title = "Ok",
                                body_html = "<p>Trusted</p>",
                            )
                        ),
                    )
                ),
                version = 1,
            )
        )
        assertTrue(!html.contains("<script>alert(1)</script>"))
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
        assertTrue(html.contains("<p>Trusted</p>"))
        assertTrue(html.contains("a&lt;script&gt;x&lt;/script&gt;@t.com"))
    }
}
