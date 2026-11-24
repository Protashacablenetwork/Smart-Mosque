package com.example.util

import kotlin.math.abs

/**
 * Pure Kotlin ISO/IEC 18004 QR Code matrix generator (Byte mode, Error Correction Level M, Versions 1..6).
 * Requires zero external dependencies so Digital Family ID Cards render reliably offline.
 */
object QrCodeGenerator {

    data class QrMatrix(
        val size: Int,
        val modules: List<BooleanArray>
    ) {
        fun get(x: Int, y: Int): Boolean = modules[y][x]
    }

    private data class VersionSpec(
        val version: Int,
        val dataCodewords: Int,
        val ecPerBlock: Int,
        val numBlocks: Int,
        val alignmentPos: IntArray
    )

    private val SPECS = listOf(
        VersionSpec(1, 16, 10, 1, intArrayOf()),
        VersionSpec(2, 28, 16, 1, intArrayOf(6, 18)),
        VersionSpec(3, 44, 26, 1, intArrayOf(6, 22)),
        VersionSpec(4, 64, 18, 2, intArrayOf(6, 26)),
        VersionSpec(5, 86, 24, 2, intArrayOf(6, 30)),
        VersionSpec(6, 108, 16, 4, intArrayOf(6, 34))
    )

    fun encode(text: String): QrMatrix {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val spec = SPECS.firstOrNull { rawBytes.size + 2 <= it.dataCodewords } ?: SPECS.last()
        val maxBytes = spec.dataCodewords - 2
        val payload = if (rawBytes.size > maxBytes) rawBytes.copyOf(maxBytes) else rawBytes

        val dataCodewords = buildDataCodewords(payload, spec.dataCodewords)
        val finalCodewords = interleaveWithEcc(dataCodewords, spec)
        val size = spec.version * 4 + 17

        val bestModules = buildBestMaskedMatrix(size, spec.alignmentPos, finalCodewords)
        return QrMatrix(size = size, modules = bestModules)
    }

    private fun buildDataCodewords(payload: ByteArray, capacityBytes: Int): IntArray {
        val bits = ArrayList<Int>(capacityBytes * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        // Byte mode indicator (0100) + 8-bit char count (versions 1..9)
        appendBits(0b0100, 4)
        appendBits(payload.size, 8)
        for (b in payload) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        val capBits = capacityBytes * 8
        val termBits = minOf(4, capBits - bits.size)
        appendBits(0, termBits)
        while (bits.size % 8 != 0) bits.add(0)

        val out = IntArray(capacityBytes)
        var idx = 0
        var i = 0
        while (i < bits.size && idx < capacityBytes) {
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[i + b]
            }
            out[idx++] = v
            i += 8
        }
        var padToggle = true
        while (idx < capacityBytes) {
            out[idx++] = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
        }
        return out
    }

    private fun interleaveWithEcc(data: IntArray, spec: VersionSpec): IntArray {
        val blockSize = spec.dataCodewords / spec.numBlocks
        val divisor = rsDivisor(spec.ecPerBlock)
        val dataBlocks = Array(spec.numBlocks) { b ->
            data.copyOfRange(b * blockSize, (b + 1) * blockSize)
        }
        val ecBlocks = Array(spec.numBlocks) { b ->
            rsRemainder(dataBlocks[b], divisor)
        }

        val total = spec.dataCodewords + spec.numBlocks * spec.ecPerBlock
        val result = IntArray(total)
        var k = 0
        for (i in 0 until blockSize) {
            for (b in 0 until spec.numBlocks) {
                result[k++] = dataBlocks[b][i]
            }
        }
        for (i in 0 until spec.ecPerBlock) {
            for (b in 0 until spec.numBlocks) {
                result[k++] = ecBlocks[b][i]
            }
        }
        return result
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        var a = x
        var b = y
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((b ushr i) and 1) * a)
        }
        return z
    }

    private fun rsDivisor(degree: Int): IntArray {
        val result = IntArray(degree)
        result[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                result[j] = gfMultiply(result[j], root)
                if (j + 1 < degree) result[j] = result[j] xor result[j + 1]
            }
            root = gfMultiply(root, 0x02)
        }
        return result
    }

    private fun rsRemainder(data: IntArray, divisor: IntArray): IntArray {
        val result = IntArray(divisor.size)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, result.size - 1)
            result[result.size - 1] = 0
            for (i in divisor.indices) {
                result[i] = result[i] xor gfMultiply(divisor[i], factor)
            }
        }
        return result
    }

    private fun buildBestMaskedMatrix(
        size: Int,
        alignPos: IntArray,
        codewords: IntArray
    ): List<BooleanArray> {
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunction(x: Int, y: Int, dark: Boolean) {
            modules[y][x] = dark
            isFunction[y][x] = true
        }

        fun drawFinder(cx: Int, cy: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val x = cx + dx
                    val y = cy + dy
                    if (x in 0 until size && y in 0 until size) {
                        val dist = maxOf(abs(dx), abs(dy))
                        setFunction(x, y, dist != 2 && dist != 4)
                    }
                }
            }
        }

        drawFinder(3, 3)
        drawFinder(size - 4, 3)
        drawFinder(3, size - 4)

        if (alignPos.size >= 2) {
            val pos = alignPos[1]
            for (dy in -2..2) {
                for (dx in -2..2) {
                    val dist = maxOf(abs(dx), abs(dy))
                    setFunction(pos + dx, pos + dy, dist != 1)
                }
            }
        }

        for (i in 0 until size) {
            if (!isFunction[6][i]) setFunction(i, 6, i % 2 == 0)
            if (!isFunction[i][6]) setFunction(6, i, i % 2 == 0)
        }
        // Dark module & reserve format info areas
        for (i in 0..8) {
            if (!isFunction[8][i]) setFunction(i, 8, false)
            if (!isFunction[i][8]) setFunction(8, i, false)
            if (i < 8) {
                setFunction(size - 1 - i, 8, false)
                setFunction(8, size - 1 - i, false)
            }
        }
        setFunction(8, size - 8, true)

        // Place codewords in zigzag pattern
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                val upward = ((right + 1) and 2) == 0
                val y = if (upward) size - 1 - vert else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y][x]) {
                        val bit = if (bitIndex < totalBits) {
                            ((codewords[bitIndex ushr 3] ushr (7 - (bitIndex and 7))) and 1) != 0
                        } else {
                            false
                        }
                        modules[y][x] = bit
                        bitIndex++
                    }
                }
            }
            right -= 2
        }

        // Evaluate all 8 masks and pick minimum penalty
        var bestMask = 0
        var minPenalty = Int.MAX_VALUE
        for (mask in 0 until 8) {
            applyMaskAndFormat(modules, isFunction, size, mask)
            val penalty = computePenalty(modules, size)
            if (penalty < minPenalty) {
                minPenalty = penalty
                bestMask = mask
            }
            applyMaskAndFormat(modules, isFunction, size, mask) // restore data bits
        }

        applyMaskAndFormat(modules, isFunction, size, bestMask)
        return modules.toList()
    }

    private fun applyMaskAndFormat(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        size: Int,
        mask: Int
    ) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunction[y][x]) {
                    val invert = when (mask) {
                        0 -> (x + y) % 2 == 0
                        1 -> y % 2 == 0
                        2 -> x % 3 == 0
                        3 -> (x + y) % 3 == 0
                        4 -> (x / 3 + y / 2) % 2 == 0
                        5 -> (x * y) % 2 + (x * y) % 3 == 0
                        6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
                        else -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
                    }
                    if (invert) modules[y][x] = !modules[y][x]
                }
            }
        }

        // Format info: EC Level M (00) + 3-bit mask -> BCH(15,5)
        val data = mask // (0 << 3) | mask
        var rem = data
        for (i in 0 until 10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        for (i in 0..5) modules[8][i] = ((bits ushr i) and 1) != 0
        modules[8][7] = ((bits ushr 6) and 1) != 0
        modules[8][8] = ((bits ushr 7) and 1) != 0
        modules[7][8] = ((bits ushr 8) and 1) != 0
        for (i in 9..14) modules[14 - i][8] = ((bits ushr i) and 1) != 0

        for (i in 0..6) modules[size - 1 - i][8] = ((bits ushr i) and 1) != 0
        for (i in 7..14) modules[8][size - 15 + i] = ((bits ushr i) and 1) != 0
        modules[size - 8][8] = true
    }

    private fun computePenalty(modules: Array<BooleanArray>, size: Int): Int {
        var result = 0
        for (y in 0 until size) {
            var runColor = modules[y][0]
            var runLen = 1
            for (x in 1 until size) {
                if (modules[y][x] == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) result += 3 + (runLen - 5)
                    runColor = modules[y][x]
                    runLen = 1
                }
            }
            if (runLen >= 5) result += 3 + (runLen - 5)
        }
        for (x in 0 until size) {
            var runColor = modules[0][x]
            var runLen = 1
            for (y in 1 until size) {
                if (modules[y][x] == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) result += 3 + (runLen - 5)
                    runColor = modules[y][x]
                    runLen = 1
                }
            }
            if (runLen >= 5) result += 3 + (runLen - 5)
        }
        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val c = modules[y][x]
                if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) {
                    result += 3
                }
            }
        }
        return result
    }
}
