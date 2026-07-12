package com.auralis.music.canvas

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing [CanvasManager] from Composable functions
 * that are not inside a Hilt-aware ViewModel.
 *
 * Usage:
 * ```kotlin
 * val entryPoint = EntryPointAccessors.fromApplication(
 *     context.applicationContext,
 *     CanvasEntryPoint::class.java
 * )
 * val canvasManager = entryPoint.canvasManager()
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CanvasEntryPoint {
    fun canvasManager(): CanvasManager
}
