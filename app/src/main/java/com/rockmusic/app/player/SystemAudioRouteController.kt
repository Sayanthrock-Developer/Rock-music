package com.rockmusic.app.player

import android.content.Context
import android.media.MediaRouter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("DEPRECATION")
class SystemAudioRouteController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val router = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter

    fun routes(): List<SystemAudioRoute> {
        val selected = router.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
        return buildList {
            for (index in 0 until router.routeCount) {
                val route = router.getRouteAt(index)
                if (route.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_AUDIO == 0) continue
                add(
                    SystemAudioRoute(
                        index = index,
                        name = route.getName(context).toString(),
                        description = route.description?.toString(),
                        isSelected = route == selected,
                        isEnabled = route.isEnabled,
                    ),
                )
            }
        }
    }

    fun select(index: Int): Result<Unit> = runCatching {
        require(index in 0 until router.routeCount) { "The selected audio route is unavailable" }
        val route = router.getRouteAt(index)
        require(route.isEnabled) { "The selected audio route is disabled" }
        require(route.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_AUDIO != 0) {
            "The selected route does not support audio"
        }
        router.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, route)
    }
}

data class SystemAudioRoute(
    val index: Int,
    val name: String,
    val description: String?,
    val isSelected: Boolean,
    val isEnabled: Boolean,
)
