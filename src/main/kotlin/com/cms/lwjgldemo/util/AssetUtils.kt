package com.cms.lwjgldemo.util

import org.lwjgl.BufferUtils
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class AssetUtils {

    companion object {

        /**
         * Loads an asset file "/resources/assets/<assetName>" into a ByteBuffer.
         *
         * This works when the asset is a real file or part of a JAR.
         */
        @Throws(IOException::class)
        fun assetAsByteBuffer(assetName: String, bufferSize: Int): ByteBuffer {
            val assetPath = "/assets/${assetName}"

            // note: can't find another class loader that actually works
            val url = AssetUtils.javaClass.getResource(assetPath)
                ?: throw IOException("Classpath resource not found: $assetPath")

            // attempt to load the asset as a file. If it's not a file,
            // load it as a dream.
            val file = File(url.file)
            var buffer: ByteBuffer
            if (file.isFile) {
                // load as a file
                val fis = FileInputStream(file)
                val fc = fis.channel
                buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
                fc.close()
                fis.close()
            } else {
                buffer = BufferUtils.createByteBuffer(bufferSize)
                val source = url.openStream() ?: throw FileNotFoundException(assetName)
                source.use { source ->
                    val buf = ByteArray(8192)
                    while (true) {
                        val bytes = source.read(buf, 0, buf.size)
                        if (bytes == -1) break
                        if (buffer.remaining() < bytes) buffer = resizeBuffer(
                            buffer,
                            Math.max(buffer.capacity() * 2, buffer.capacity() - buffer.remaining() + bytes)
                        )
                        buffer.put(buf, 0, bytes)
                    }
                    buffer.flip()
                }
            }
            return buffer
        }

        private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
            val newBuffer = BufferUtils.createByteBuffer(newCapacity)
            buffer.flip()
            newBuffer.put(buffer)
            return newBuffer
        }

    }

}