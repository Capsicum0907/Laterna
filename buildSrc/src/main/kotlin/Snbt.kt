import java.io.DataInputStream
import java.io.File
import java.util.zip.GZIPInputStream

object Snbt {
    fun fromNbt(file: File): String = DataInputStream(GZIPInputStream(file.inputStream()).buffered()).use { input ->
        val type = input.readByte().toInt()
        check(type == COMPOUND) { "$file does not start with a compound" }
        input.readUTF()
        buildString { value(input, type, this) }
    }

    private fun value(input: DataInputStream, type: Int, out: StringBuilder) {
        when (type) {
            1 -> out.append(input.readByte()).append('b')
            2 -> out.append(input.readShort()).append('s')
            3 -> out.append(input.readInt())
            4 -> out.append(input.readLong()).append('L')
            5 -> out.append(input.readFloat()).append('f')
            6 -> out.append(input.readDouble()).append('d')
            7 -> array(input, "B", out) { out.append(input.readByte()).append('b') }
            8 -> string(input.readUTF(), out)
            9 -> {
                val element = input.readByte().toInt()
                val size = input.readInt()
                out.append('[')
                repeat(size) { index ->
                    if (index > 0) out.append(',')
                    value(input, element, out)
                }
                out.append(']')
            }
            COMPOUND -> {
                out.append('{')
                var first = true
                while (true) {
                    val child = input.readByte().toInt()
                    if (child == 0) break
                    if (!first) out.append(',')
                    first = false
                    string(input.readUTF(), out)
                    out.append(':')
                    value(input, child, out)
                }
                out.append('}')
            }
            11 -> array(input, "I", out) { out.append(input.readInt()) }
            12 -> array(input, "L", out) { out.append(input.readLong()).append('L') }
            else -> error("Unknown NBT tag type $type")
        }
    }

    private fun array(input: DataInputStream, prefix: String, out: StringBuilder, element: () -> Unit) {
        val size = input.readInt()
        out.append('[').append(prefix).append(';')
        repeat(size) { index ->
            if (index > 0) out.append(',')
            element()
        }
        out.append(']')
    }

    private fun string(text: String, out: StringBuilder) {
        out.append('"')
        for (c in text) {
            if (c == '"' || c == '\\') out.append('\\')
            out.append(c)
        }
        out.append('"')
    }

    private const val COMPOUND = 10
}
