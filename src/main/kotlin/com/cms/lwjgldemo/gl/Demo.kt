package com.cms.lwjgldemo.gl

import com.cms.lwjgldemo.gl.textures.Texture
import com.cms.lwjgldemo.gl.view.LetterBoxView
import org.lwjgl.Version
import org.lwjgl.glfw.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil


class Demo {

    // coordinate system width and height with a bottom-left origin
    private val width: Float = 1920f
    private val height: Float = 1080f
    
    // The window handle
    private var window: Long = 0
    private var debugProc: Callback? = null

    // last coordinates of cursor location on screen
    private var mouseXPos: Float = 0f
    private var mouseYPos: Float = 0f

    private var boxView = LetterBoxView(this.width, this.height)

    private var cursorTexture = Texture("red.png")

    fun run() {
        println("Hello LWJGL " + Version.getVersion() + "!")
        try {
            init()
            loop()
        } finally {
            try {
                destroy()
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE) // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1)

        // Create the window
        window = GLFW.glfwCreateWindow(500, 500, "Hello World!", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        GLFW.glfwSetCursorPosCallback(window) { _, xpos, ypos ->
            println("Mouse detected at position ${xpos},${ypos}")
            mouseXPos = xpos.toFloat()
            mouseYPos = ypos.toFloat()
        }
        GLFW.glfwSetKeyCallback(window) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) GLFW.glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop
        }
        GLFW.glfwSetWindowRefreshCallback(window) { window: Long -> render() }
        GLFW.glfwSetWindowSizeCallback(window) { window: Long, width: Int, height: Int ->
            windowSizeChanged(window, width, height)
        }
        GLFW.glfwSetFramebufferSizeCallback(window) { window, width, height ->
            framebufferSizeChanged(window, width, height)
        }
        MemoryStack.stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            GLFW.glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())

            // Center the window
            GLFW.glfwSetWindowPos(
                window,
                (vidmode!!.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window)

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // Keep track of debug callback
        debugProc = GLUtil.setupDebugMessageCallback()

        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        // Make the window visible
        GLFW.glfwShowWindow(window)

        glfwInvoke(
            window,
            { window: Long, width: Int, height: Int ->
                windowSizeChanged(
                    window,
                    width,
                    height
                )
            }) { window: Long, width: Int, height: Int ->
            framebufferSizeChanged(
                window,
                width,
                height
            )
        }
    }

    private fun windowSizeChanged(window: Long, width: Int, height: Int) {
        println("Window size changed to ${width},${height}")
    }

    private fun framebufferSizeChanged(window: Long, width: Int, height: Int) {
        println("Frame buffer size changed to ${width},${height}")
        glViewport(0, 0, width, height)
        boxView.setBufferSize(width.toFloat(), height.toFloat())
    }

    /**
     * Invokes the specified callbacks using the current window and framebuffer sizes of the specified GLFW window.
     *
     * @param window            the GLFW window
     * @param windowSizeCB      the window size callback, may be null
     * @param framebufferSizeCB the framebuffer size callback, may be null
     */
    fun glfwInvoke(
        window: Long,
        windowSizeCB: GLFWWindowSizeCallbackI?,
        framebufferSizeCB: GLFWFramebufferSizeCallbackI?
    ) {
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            if (windowSizeCB != null) {
                GLFW.glfwGetWindowSize(window, w, h)
                windowSizeCB.invoke(window, w[0], h[0])
            }
            if (framebufferSizeCB != null) {
                GLFW.glfwGetFramebufferSize(window, w, h)
                framebufferSizeCB.invoke(window, w[0], h[0])
            }
        }
    }

    private fun loop() {
        // prepare drawing

        // load resources
        cursorTexture.load()

        glClearColor(1.0f, 1.0f, 0.0f, 0.0f)

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!GLFW.glfwWindowShouldClose(window)) {
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            GLFW.glfwPollEvents()

            render()
        }

        // cleanup
        cursorTexture.cleanup()
    }

    private fun render() {
        // clear the framebuffer
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // enable blending (transparency)
        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)

        // unit top-down orthographic projection
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        glOrtho(0.0, 1.0, 1.0, 0.0, -1.0, 1.0)

        // fill the view background
        glColor3d(0.0, 1.0, 0.0) // background color
        val marginX = boxView.getProjectionMarginX()
        val marginY = boxView.getProjectionMarginY()
        glRectf(marginX, marginY, 1-marginX, 1-marginY)

        // debug lines
        glBegin(GL_LINES)
        glColor3d(0.0, 0.0, 1.0) // blue

        fun glVertexV(x: Float, y: Float) {
            val mapped = boxView.projectVirtualPoint(x, y)
            glVertex2f(mapped.x, mapped.y)
        }
        fun glVertexS(x: Float, y: Float) {
            val mapped = boxView.projectScreenPoint(x, y)
            glVertex2f(mapped.x, mapped.y)
        }

        glVertexV(0f, 0f)
        glVertexV(width/2, height/2)

        // draw line to mouse in projected space
        glVertexS(0f, 0f)
        glVertexS(mouseXPos, mouseYPos)

        // draw line to mouse in virtual space
        val mouseMappedToVirtual = boxView.projectScreenPointToVirtual(mouseXPos, mouseYPos)
        // then project that back
        glVertexV(0f, 0f)
        glVertexV(mouseMappedToVirtual.x, mouseMappedToVirtual.y)

        glEnd()

        // draw cursor texture
        cursorTexture.renderAt(boxView.projectVirtualPoint(mouseMappedToVirtual.x, mouseMappedToVirtual.y), 1f/boxView.bufferWidth, 1f/boxView.bufferHeight)

        // show the latest drawing
        GLFW.glfwSwapBuffers(window)
    }

    private fun destroy() {
        GL.setCapabilities(null)
        if (debugProc != null) {
            debugProc!!.free()
        }
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(window)
        GLFW.glfwDestroyWindow(window)
        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)!!.free()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Demo().run()
        }
    }
}