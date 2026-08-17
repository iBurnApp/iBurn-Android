package com.gaiagps.iburn

private const val SURVEY_GIZMO_UPLOADS_HTTP_ORIGIN =
    "http://surveygizmoresponseuploads.s3.amazonaws.com/"
private const val SURVEY_GIZMO_UPLOADS_HTTPS_ORIGIN =
    "https://surveygizmoresponseuploads.s3.amazonaws.com/"

/**
 * Upgrades legacy SurveyGizmo S3 image URLs present in 2026 Playa data to HTTPS.
 *
 * Android blocks cleartext HTTP traffic by default, and this S3 bucket serves the same objects over
 * HTTPS. Keep the rewrite host-specific so URLs for servers without HTTPS support are unchanged.
 */
internal fun normalizeRemoteImageUrl(url: String): String =
    if (url.startsWith(SURVEY_GIZMO_UPLOADS_HTTP_ORIGIN)) {
        SURVEY_GIZMO_UPLOADS_HTTPS_ORIGIN + url.removePrefix(SURVEY_GIZMO_UPLOADS_HTTP_ORIGIN)
    } else {
        url
    }
