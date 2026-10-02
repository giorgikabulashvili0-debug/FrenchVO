package com.georgeslebatoon.frenchvo

import android.content.Context
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CancellationException

class TomEngine(private val context: Context) {
    private var engine: OfflineTts? = null
    private fun copyAssets(path: String, target: File) {
        val children = context.assets.list(path) ?: emptyArray()
        if (children.isNotEmpty()) {
            target.mkdirs()
            children.forEach { copyAssets("$path/$it", File(target, it)) }
        } else if (!target.exists()) {
            target.parentFile?.mkdirs()
            val temporary = File(target.path + ".tmp")
            context.assets.open(path).use { input -> temporary.outputStream().use { input.copyTo(it) } }
            check(temporary.renameTo(target)) { "Impossible de préparer la voix" }
        }
    }
    fun generate(text: String, speed: Float, output: File, listener: Listener) {
        val root = File(context.filesDir, "tom-v1")
        if (engine == null) {
            listener.progress("Préparation de Tom… (première utilisation)")
            copyAssets("tom", root)
            if (listener.cancelled()) throw CancellationException()
            engine = OfflineTts(config = OfflineTtsConfig(model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(model = File(root,"fr_FR-tom-medium.onnx").path,
                    tokens = File(root,"tokens.txt").path, dataDir = File(root,"espeak-ng-data").path),
                numThreads = 2)))
        }
        val chunks = splitText(text)
        RandomAccessFile(output, "rw").use { wav ->
            wav.setLength(0); wav.write(ByteArray(44))
            val sampleRate = engine!!.sampleRate()
            chunks.forEachIndexed { index, part ->
                if (listener.cancelled()) throw CancellationException()
                listener.progress("Tom : partie ${index + 1}/${chunks.size}…")
                val audio = engine!!.generateWithCallback(part, 0, speed) { if (listener.cancelled()) 0 else 1 }
                if (listener.cancelled()) throw CancellationException()
                val bytes = ByteArray(audio.samples.size * 2)
                audio.samples.forEachIndexed { i, value ->
                    val pcm = (value.coerceIn(-1f,1f) * 32767).toInt()
                    bytes[i*2] = pcm.toByte(); bytes[i*2+1] = (pcm shr 8).toByte()
                }
                wav.write(bytes)
            }
            val dataSize = (wav.length() - 44).toInt()
            check(dataSize > 0) { "Aucun audio généré" }
            wav.seek(0)
            fun le(value: Int, size: Int) { repeat(size) { wav.write(value shr (it*8)) } }
            wav.writeBytes("RIFF"); le(dataSize+36,4); wav.writeBytes("WAVEfmt ")
            le(16,4); le(1,2); le(1,2); le(sampleRate,4); le(sampleRate*2,4)
            le(2,2); le(16,2); wav.writeBytes("data"); le(dataSize,4)
        }
    }
    fun release() { engine?.release(); engine = null }
    interface Listener { fun progress(message: String); fun cancelled(): Boolean }
    companion object {
        fun splitText(text: String): List<String> {
            val result = mutableListOf<String>()
            var rest = text.trim()
            while (rest.length > 600) {
                val window = rest.take(600)
                var end = window.lastIndexOfAny(charArrayOf('.', '!', '?', '\n')) + 1
                if (end < 150) end = window.lastIndexOf(' ')
                if (end < 1) end = 600
                result.add(rest.take(end)); rest = rest.drop(end).trimStart()
            }
            if (rest.isNotBlank()) result.add(rest)
            return result
        }
    }
}
