package com.rockmusic.app.data.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rockmusic.app.domain.integration.OfficialProviderRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class OfficialRouteLaunchTarget(
    val uri: String,
    val packageName: String?,
)

object OfficialRouteLaunchPlanner {
    private val allowedWebHosts = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
    )
    private val allowedPackages = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
    )
    private val videoIdPattern = Regex("^[A-Za-z0-9_-]{6,64}$")

    fun targets(route: OfficialProviderRoute): List<OfficialRouteLaunchTarget> {
        val webUri = validateWebUri(route.webUri)
        val appUri = route.androidAppUri
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.also(::validateAppUri)
        val packages = route.preferredPackages
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

        require(packages.all(allowedPackages::contains)) {
            "The official provider route contains an unsupported Android package"
        }

        val packageTargets = packages.map { packageName ->
            OfficialRouteLaunchTarget(
                uri = appUri ?: webUri,
                packageName = packageName,
            )
        }

        return (packageTargets + OfficialRouteLaunchTarget(webUri, packageName = null)).distinct()
    }

    private fun validateWebUri(value: String): String {
        val cleaned = value.trim()
        val uri = runCatching { URI(cleaned) }.getOrNull()
        require(
            uri != null &&
                uri.scheme.equals("https", ignoreCase = true) &&
                uri.host?.lowercase() in allowedWebHosts &&
                uri.userInfo == null &&
                uri.port == -1 &&
                uri.fragment == null,
        ) {
            "The official provider web destination must be an allow-listed YouTube HTTPS URL"
        }
        return cleaned
    }

    private fun validateAppUri(value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        require(uri != null && uri.isAbsolute && uri.userInfo == null && uri.fragment == null) {
            "The official provider app destination is invalid"
        }

        when (uri.scheme.lowercase()) {
            "https" -> require(uri.host?.lowercase() in allowedWebHosts && uri.port == -1) {
                "The official provider app HTTPS destination is not allow-listed"
            }

            "vnd.youtube" -> require(
                uri.rawQuery == null &&
                    videoIdPattern.matches(uri.schemeSpecificPart.orEmpty()),
            ) {
                "The official YouTube app destination does not contain a valid video ID"
            }

            else -> error("The official provider app scheme is not supported")
        }
    }
}

@Singleton
class OfficialProviderRouteLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun launch(route: OfficialProviderRoute): Result<OfficialRouteLaunchTarget> {
        val targets = runCatching { OfficialRouteLaunchPlanner.targets(route) }
            .getOrElse { return Result.failure(it) }
        var lastFailure: Throwable? = null

        targets.forEach { target ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target.uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                target.packageName?.let { packageName -> setPackage(packageName) }
            }
            try {
                context.startActivity(intent)
                return Result.success(target)
            } catch (error: ActivityNotFoundException) {
                lastFailure = error
            } catch (error: SecurityException) {
                lastFailure = error
            }
        }

        return Result.failure(
            lastFailure ?: IllegalStateException(
                "No official provider application or web browser is available",
            ),
        )
    }
}
