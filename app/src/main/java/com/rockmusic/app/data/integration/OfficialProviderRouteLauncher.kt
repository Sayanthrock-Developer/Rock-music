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
    private val packagePattern = Regex("^[A-Za-z0-9_.]+$")
    private val allowedAppSchemes = setOf("https", "vnd.youtube")

    fun targets(route: OfficialProviderRoute): List<OfficialRouteLaunchTarget> {
        val webUri = validateWebUri(route.webUri)
        val appUri = route.androidAppUri
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.also(::validateAppUri)

        val packageTargets = route.preferredPackages
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter(packagePattern::matches)
            .distinct()
            .map { packageName ->
                OfficialRouteLaunchTarget(
                    uri = appUri ?: webUri,
                    packageName = packageName,
                )
            }
            .toList()

        return (packageTargets + OfficialRouteLaunchTarget(webUri, packageName = null)).distinct()
    }

    private fun validateWebUri(value: String): String {
        val cleaned = value.trim()
        val uri = runCatching { URI(cleaned) }.getOrNull()
        require(
            uri != null &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null,
        ) {
            "The official provider web destination must be a clean HTTPS URL"
        }
        return cleaned
    }

    private fun validateAppUri(value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        require(
            uri != null &&
                uri.isAbsolute &&
                uri.scheme.lowercase() in allowedAppSchemes &&
                uri.userInfo == null &&
                uri.fragment == null,
        ) {
            "The official provider app destination is invalid"
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
                target.packageName?.let(::setPackage)
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
