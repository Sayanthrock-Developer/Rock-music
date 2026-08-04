package com.rockmusic.app.player

import android.content.Context
import android.media.MediaRouter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.IdentityHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("DEPRECATION")
class SystemAudioRouteController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val router = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
    private val idsByRoute = IdentityHashMap<MediaRouter.RouteInfo, Int>()
    private val routesById = mutableMapOf<Int, MediaRouter.RouteInfo>()
    private var nextRouteId = 1

    @Synchronized
    fun routes(): List<SystemAudioRoute> {
        val mediaRouter = router ?: return emptyList()
        val selected = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
        val currentRoutes = buildList {
            val routeCount = mediaRouter.routeCount
            for (routerIndex in 0 until routeCount) {
                val route = mediaRouter.getRouteAt(routerIndex)
                if (route.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_AUDIO != 0) add(route)
            }
        }

        idsByRoute.entries.removeAll { entry ->
            currentRoutes.none { current -> current === entry.key }
        }
        routesById.entries.removeAll { entry ->
            currentRoutes.none { current -> current === entry.value }
        }

        return currentRoutes.map { route ->
            val stableId = idsByRoute[route] ?: nextRouteId++.also { assignedId ->
                idsByRoute[route] = assignedId
                routesById[assignedId] = route
            }
            SystemAudioRoute(
                index = stableId,
                name = route.getName(context).toString(),
                description = route.description?.toString(),
                isSelected = route === selected,
                isEnabled = route.isEnabled,
            )
        }
    }

    @Synchronized
    fun select(index: Int): Result<Unit> = runCatching {
        val mediaRouter = router ?: error("Audio output routing is unavailable on this device")
        val route = routesById[index]
            ?: error("The selected audio route is no longer available")
        val routeCount = mediaRouter.routeCount
        val isStillAvailable = (0 until routeCount)
            .any { routerIndex -> mediaRouter.getRouteAt(routerIndex) === route }
        require(isStillAvailable) { "The selected audio route is no longer available" }
        require(route.isEnabled) { "The selected audio route is disabled" }
        require(route.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_AUDIO != 0) {
            "The selected route does not support audio"
        }
        mediaRouter.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, route)
    }
}

data class SystemAudioRoute(
    /** Opaque stable ID for the route snapshot, not a mutable MediaRouter list index. */
    val index: Int,
    val name: String,
    val description: String?,
    val isSelected: Boolean,
    val isEnabled: Boolean,
)
