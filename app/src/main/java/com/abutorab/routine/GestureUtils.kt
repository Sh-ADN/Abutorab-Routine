package com.abutorab.routine

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange

suspend fun PointerInputScope.detectZoomPanFling(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onFling: (velocity: androidx.compose.ui.unit.Velocity) -> Unit
) {
    awaitEachGesture {
        val velocityTracker = VelocityTracker()
        awaitFirstDown(requireUnconsumed = false)
        
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (canceled) break
            
            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            val centroid = event.calculateCentroid(useCurrent = false)
            
            if (zoomChange != 1f || panChange != Offset.Zero) {
                onGesture(centroid, panChange, zoomChange)
            }
            
            event.changes.forEach { change ->
                if (change.positionChange() != Offset.Zero) {
                    change.consume()
                }
                // Track velocity
                velocityTracker.addPointerInputChange(change)
            }
        } while (event.changes.any { it.pressed })
        
        val velocity = velocityTracker.calculateVelocity()
        onFling(velocity)
    }
}
