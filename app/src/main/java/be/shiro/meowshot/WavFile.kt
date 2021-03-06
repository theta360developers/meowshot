/*
 * Copyright (C) 2018 theta4j project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.shiro.meowshot

import java.io.Closeable
import java.io.RandomAccessFile

class WavFile(
    path: String
) : Closeable {
    companion object {
        val SAMPLE_RATE = 44100
        val BITS_PER_SAMPLE = 16
    }

    private val HEADER_SIZE = 44L
    private val CHUNK_SIZE_POS = 4L
    private val SUB_CHUNK2_SIZE_POS = 40L

    // RIFF Header
    private val RIFF_CHUNK_ID = "RIFF"
    private val RIFF_CHUNK_SIZE: Int = 0       // Dummy
    private val RIFF_FORMAT = "WAVE"
    private val RIFF_SUB_CHUNK1_ID = "fmt "    // space char is required. do not trim.
    private val RIFF_SUB_CHUNK1_SIZE: Int = 16 // 16 for PCM
    private val RIFF_AUDIO_FORMAT: Short = 1   // PCM = 1
    private val RIFF_NUM_CHANNELS: Short = 1   // Monaural
    private val RIFF_SAMPLE_RATE: Int = SAMPLE_RATE;  // 8kHz
    private val RIFF_BYTE_RATE: Int = RIFF_SAMPLE_RATE * RIFF_NUM_CHANNELS * BITS_PER_SAMPLE / 8
    private val RIFF_BLOCK_ALIGN: Short = (RIFF_NUM_CHANNELS * BITS_PER_SAMPLE / 8).toShort()
    private val RIFF_BITS_PER_SAMPLE: Short = BITS_PER_SAMPLE.toShort()
    private val RIFF_SUB_CHUNK2_ID = "data"
    private val RIFF_SUB_CHUNK2_SIZE: Int = 0  // Dummy

    private val raf = RandomAccessFile(path, "rw")

    init {
        raf.setLength(0) // clear the file

        // RIFF Header
        raf.seek(0)
        raf.write(RIFF_CHUNK_ID.toByteArray())
        raf.write(littleEndian(RIFF_CHUNK_SIZE)) // Dummy value for skip bytes
        raf.write(RIFF_FORMAT.toByteArray())
        raf.write(RIFF_SUB_CHUNK1_ID.toByteArray())
        raf.write(littleEndian(RIFF_SUB_CHUNK1_SIZE))
        raf.write(littleEndian(RIFF_AUDIO_FORMAT))
        raf.write(littleEndian(RIFF_NUM_CHANNELS))
        raf.write(littleEndian(RIFF_SAMPLE_RATE))
        raf.write(littleEndian(RIFF_BYTE_RATE))
        raf.write(littleEndian(RIFF_BLOCK_ALIGN))
        raf.write(littleEndian(RIFF_BITS_PER_SAMPLE))
        raf.write(RIFF_SUB_CHUNK2_ID.toByteArray())
        raf.write(littleEndian(RIFF_SUB_CHUNK2_SIZE)) // Dummy value for skip bytes
    }

    fun write(values: ShortArray, length: Int) {
        raf.seek(raf.length())
        for (i in 0 until length) {
            write(values[i])
        }
        updateFileSize()
        updateDataSize()
    }

    fun cutEnd(millis: Long) {
        val sizeToCut = RIFF_BYTE_RATE * millis / 1000
        var newSize = raf.length() - sizeToCut
        if (newSize < HEADER_SIZE) {
            newSize = HEADER_SIZE
        }
        raf.setLength(newSize)
        updateFileSize()
        updateDataSize()
    }

    override fun close() {
        raf.close()
    }

    private fun write(value: Short) {
        raf.write(littleEndian(value))
    }

    private fun updateFileSize() {
        val fileSize = (raf.length() - 8).toInt();
        raf.seek(CHUNK_SIZE_POS)
        raf.write(littleEndian(fileSize))
    }

    private fun updateDataSize() {
        val dataSize = (raf.length() - HEADER_SIZE).toInt()
        raf.seek(SUB_CHUNK2_SIZE_POS)
        raf.write(littleEndian(dataSize))
    }

    private fun littleEndian(value: Short): ByteArray {
        return byteArrayOf(
            value.toByte(),
            value.toInt().shr(8).toByte()
        )
    }

    private fun littleEndian(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            value.shr(8).toByte(),
            value.shr(16).toByte(),
            value.shr(24).toByte()
        )
    }
}
