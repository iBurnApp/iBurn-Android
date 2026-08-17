package com.gaiagps.iburn

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageUrlTest {

    @Test
    fun normalizeRemoteImageUrl_upgradesSurveyGizmoS3UrlToHttps() {
        val path = "fileuploads/101590/6047425/chosen-yogurt.jpg"

        assertEquals(
            "https://surveygizmoresponseuploads.s3.amazonaws.com/$path",
            normalizeRemoteImageUrl("http://surveygizmoresponseuploads.s3.amazonaws.com/$path")
        )
    }

    @Test
    fun normalizeRemoteImageUrl_leavesHttpsUrlUnchanged() {
        val url = "https://surveygizmoresponseuploads.s3.amazonaws.com/fileuploads/image.jpg"

        assertEquals(url, normalizeRemoteImageUrl(url))
    }

    @Test
    fun normalizeRemoteImageUrl_doesNotRewriteOtherHosts() {
        val url = "http://example.com/image.jpg"

        assertEquals(url, normalizeRemoteImageUrl(url))
    }
}
