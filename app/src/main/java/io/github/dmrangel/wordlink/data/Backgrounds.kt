package io.github.dmrangel.wordlink.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Background photos picked by the player. They are copied into app storage,
 * scaled down once on import, and blurred once per blur setting by shrinking
 * and box-blurring a small copy; drawing it scaled up is then a single cheap
 * texture draw, so the photo costs nothing per frame.
 */
object Backgrounds {
    private const val DIR = "backgrounds"
    private const val MAX_SIDE = 1920

    /** Copies the picked image into app storage and returns its file name. */
    fun import(context: Context, uri: Uri): String? {
        val bitmap = runCatching { decode(context, uri) }.getOrNull() ?: return null
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val name = "bg-${System.currentTimeMillis()}.jpg"
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return name
    }

    fun clear(context: Context) {
        File(context.filesDir, DIR).listFiles()?.forEach { it.delete() }
    }

    /** Decodes the stored photo, blurred by [blur] (0 = sharp, 1 = strongest). */
    fun load(context: Context, name: String, blur: Float, screenWidth: Int): Bitmap? {
        val file = File(File(context.filesDir, DIR), name)
        if (!file.exists()) return null
        val target = if (blur <= 0f) screenWidth else (screenWidth / (4f + blur * 28f)).roundToInt().coerceAtLeast(16)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, target) }
        val decoded = BitmapFactory.decodeFile(file.path, options) ?: return null
        if (blur <= 0f) return decoded
        val height = (decoded.height * target / decoded.width.toFloat()).roundToInt().coerceAtLeast(16)
        val small = decoded.scale(target, height)
        if (small !== decoded) decoded.recycle()
        return boxBlur(small, radius = 2, passes = 3)
    }

    // The framework ExifInterface only runs on API 26-27 here (ImageDecoder handles
    // rotation from 28 on), so the androidx copy isn't worth the extra dependency.
    private fun decode(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoder applies EXIF rotation by itself.
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val scale = MAX_SIDE / max(info.size.width, info.size.height).toFloat()
                if (scale < 1f) decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), MAX_SIDE) }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun sampleSize(size: Int, target: Int): Int {
        var sample = 1
        while (size / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Separable box blur; three passes approximate a Gaussian. */
    private fun boxBlur(source: Bitmap, radius: Int, passes: Int): Bitmap {
        val w = source.width
        val h = source.height
        var pixels = IntArray(w * h).also { source.getPixels(it, 0, w, 0, 0, w, h) }
        var scratch = IntArray(w * h)
        repeat(passes) {
            blurLine(pixels, scratch, w, h, radius, horizontal = true)
            blurLine(scratch, pixels, w, h, radius, horizontal = false)
        }
        val out = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true).also { source.recycle() }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        scratch = IntArray(0)
        pixels = scratch
        return out
    }

    private fun blurLine(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val length = if (horizontal) w else h
        val window = radius * 2 + 1
        for (line in 0 until lines) {
            fun at(i: Int): Int {
                val c = i.coerceIn(0, length - 1)
                return if (horizontal) line * w + c else c * w + line
            }
            var r = 0
            var g = 0
            var b = 0
            for (i in -radius..radius) {
                val p = src[at(i)]
                r += p shr 16 and 0xFF
                g += p shr 8 and 0xFF
                b += p and 0xFF
            }
            for (i in 0 until length) {
                dst[at(i)] = (0xFF shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
                val outPixel = src[at(i - radius)]
                val inPixel = src[at(i + radius + 1)]
                r += (inPixel shr 16 and 0xFF) - (outPixel shr 16 and 0xFF)
                g += (inPixel shr 8 and 0xFF) - (outPixel shr 8 and 0xFF)
                b += (inPixel and 0xFF) - (outPixel and 0xFF)
            }
        }
    }
}
