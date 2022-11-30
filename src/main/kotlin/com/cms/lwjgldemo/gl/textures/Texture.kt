package com.cms.lwjgldemo.gl.textures

import com.cms.lwjgldemo.util.AssetUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryStack
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.nio.ByteBuffer
import kotlin.math.roundToInt

// Resources
// https://learnopengl.com/Getting-started/Textures
class Texture(private var assetName: String) {

    var id: Int = 0
    var w = 0f
    var h = 0f

    private var image: ByteBuffer? = null
    private var comp = 0

    fun render(rect: Rectangle2D.Float) {
        // enable texture mapping
        glEnable(GL_TEXTURE_2D)

        // model view is needed for texturing
        glMatrixMode(GL_MODELVIEW)

        // apply a translation / scale matrix for this texture
        glPushMatrix()
        glTranslatef(rect.x, rect.y, 0.0f)
        glScalef(rect.width/w, rect.height/h, 1f)

        // no explicit color
        glColor4f(1f, 1f, 1f, 1f)

        // render texture by coordinates
        // TODO From what I've read it sounds like a bad practice to use GL_QUADS even if it's for a square texture.
        //      It may be worth looking at alternatives to understand why.
        //      Also, using more complex textures with well defined geometry could be a more performant alternative to
        //      transparency.
        glBindTexture(GL_TEXTURE_2D, id)
        glBegin(GL_QUADS)
        glTexCoord2f(0.0f, 0.0f)
        glVertex2f(0.0f, 0.0f)
        glTexCoord2f(1.0f, 0.0f)
        glVertex2f(w, 0.0f)
        glTexCoord2f(1.0f, 1.0f)
        glVertex2f(w, h)
        glTexCoord2f(0.0f, 1.0f)
        glVertex2f(0.0f, h)
        glEnd()

        // remove the translation / scale matrix
        glPopMatrix()

        // disable texture rendering
        glDisable(GL_TEXTURE_2D)
    }

    fun load() {
        loadGLTexture()
    }

    fun cleanup() {
        glDeleteTextures(id)
    }


    private fun loadGLTexture() {
        // prepare new texture
        this.id = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

        MemoryStack.stackPush().use { frame ->
            val widthArr = frame.mallocInt(1)
            val heightArr = frame.mallocInt(1)
            val compArr = frame.mallocInt(1)
            this.image = stbi_load_from_memory(
                AssetUtils.assetAsByteBuffer(assetName, 1024),
                widthArr, heightArr, compArr, 0
            )!!
            this.w = widthArr.get().toFloat()
            this.h = heightArr.get().toFloat()
            this.comp = compArr.get()
        }

        // determine image format
        val format: Int = if (comp == 3) {
            // some images are skewed without this step
            if (w.toInt() and 3 != 0) {
                glPixelStorei(GL_UNPACK_ALIGNMENT, 2 - (w.toInt() and 1))
            }
            GL_RGB
        } else {
            // TODO understand performance implications here
            // premultiplyAlpha(this.image!!, this.w.toInt(), this.h.toInt())
            GL_RGBA
        }

        // load image byte data into texture
        glTexImage2D(GL_TEXTURE_2D, 0, format, w.toInt(), h.toInt(), 0, format, GL_UNSIGNED_BYTE, image!!)

        // avoid memory leak
        STBImage.stbi_image_free(image!!)
    }

    /**
     * Can be applied to RGBA byte buffers but would like to better understand the performance implications before
     * using it.
     */
    private fun premultiplyAlpha(image: ByteBuffer, w: Int, h: Int) {
        val stride = w * 4
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * stride + x * 4
                val alpha: Float = (image[i + 3].toInt() and 0xFF) / 255.0f
                image.put(i + 0, ((image[i + 0].toInt() and 0xFF) * alpha).roundToInt().toByte())
                image.put(i + 1, ((image[i + 1].toInt() and 0xFF) * alpha).roundToInt().toByte())
                image.put(i + 2, ((image[i + 2].toInt() and 0xFF) * alpha).roundToInt().toByte())
            }
        }
    }

}