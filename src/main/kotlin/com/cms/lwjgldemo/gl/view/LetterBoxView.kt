package com.cms.lwjgldemo.gl.view

import java.awt.geom.Point2D

/**
 * Calculates a letterbox rendered view translation and scaling of a virtual coordinate system
 * within a given buffer (pixels) as well as translation and scaling to the projection used to render to that buffer.
 * Right now this assumes the use of a standard top-down orthographic projection with upper-left origin.
 *
 * @param virtualWidth width of the virtual game view in pixels
 * @param virtualHeight height of the virtual game iew in pixels
 */
data class LetterBoxView(val virtualWidth: Float, val virtualHeight: Float) {
    /**
     * Width in pixels of the entire graphics buffer (including margins)
      */
    var bufferWidth = 0.0f
        private set

    /**
     * Height in pixels of he entire graphics buffer (including margins)
     */
    var bufferHeight = 0.0f
        private set

    /**
     * Width in pixels of the portion of the buffer that the virtual view will be rendered within
     */
    private var viewWidthPx = 0.0f

    /**
     * Height of the portion of the buffer that the virtual view will be rendered within
     */
    private var viewHeightPx = 0.0f

    /**
     * Width in pixels of view side margins
     */
    private var marginXPx: Float = 0.0f

    /**
     * Width in pixels of view top/bottom margins
     */
    private var marginYPx: Float = 0.0f

    /**
     * Update the buffer size. This is expected to happen whenever the screen is resized.
     *
     * @param width of the entire graphics buffer in pixels
     * @param height of the entire graphics buffer in pixels
     */
    fun setBufferSize(width: Float, height: Float) {
        this.bufferWidth = width
        this.bufferHeight = height
        recalculate()
    }

    private fun recalculate() {
        val bufferAspectRatio = bufferWidth / bufferHeight
        val targetAspectRatio = virtualWidth / virtualHeight

        // Note: the goal is for view + margins to perfectly fill the buffer without overlapping.

        if (bufferAspectRatio > targetAspectRatio) {
            // add side margins and fill height
            viewWidthPx = bufferHeight * targetAspectRatio
            marginXPx = (bufferWidth - viewWidthPx) / 2f
            viewHeightPx = bufferHeight
            marginYPx = 0f
        } else {
            // add top/bottom margins and fill width
            viewWidthPx = bufferWidth
            marginXPx = 0f
            viewHeightPx = bufferWidth / targetAspectRatio
            marginYPx = (bufferHeight - viewHeightPx) / 2f
        }
    }

    fun getProjectionMarginX(): Float {
        return marginXPx / bufferWidth
    }

    fun getProjectionMarginY(): Float {
        return marginYPx / bufferHeight
    }

    /**
     * Project a point from virtual space to projection space
     */
    fun projectVirtualPoint(x: Float, y: Float): Point2D.Float {
        return Point2D.Float(
            getProjectionMarginX() + (x / virtualWidth)*(viewWidthPx/(bufferWidth-getProjectionMarginX())),
            getProjectionMarginY() + (y / virtualHeight)*(viewHeightPx/(bufferHeight-getProjectionMarginY()))
        )
    }

    /**
     * Project a point on the screen to projection space
     */
    fun projectScreenPoint(x: Float, y: Float): Point2D.Float {
        return Point2D.Float(
            (x / bufferWidth),
            (y / bufferHeight)
        )
    }

    /**
     * Project a point on the screen to virtual space
     */
    fun projectScreenPointToVirtual(x: Float, y: Float): Point2D.Float {
        val pX = x / bufferWidth
        val pY = y / bufferHeight
        return Point2D.Float(
            (pX - getProjectionMarginX())*((bufferWidth-getProjectionMarginX())/viewWidthPx)*virtualWidth,
            (pY - getProjectionMarginY())*((bufferHeight-getProjectionMarginY())/viewHeightPx)*virtualHeight
        )
    }


}