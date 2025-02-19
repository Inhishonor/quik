package dev.octoshrimpy.quik.feature.blocking.manager

data class BlockingManagerState(
    val blockingManager: Int = 0,
    val spamBlockerInstalled: Boolean = false,
    val callBlockerInstalled: Boolean = false,
    val callControlInstalled: Boolean = false,
    val siaInstalled: Boolean = false
)
