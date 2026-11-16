object Targets {
    fun java(minecraft: String): Int = when {
        atLeast(minecraft, "1.20.5") -> 21
        atLeast(minecraft, "1.18") -> 17
        atLeast(minecraft, "1.17") -> 16
        else -> 8
    }

    fun packFormat(minecraft: String): Int = PACK_FORMATS[minecraft]
        ?: error("No pack format recorded for Minecraft $minecraft")

    private val PACK_FORMATS = mapOf(
        "1.16.5" to 6,
        "1.18.2" to 8,
        "1.19.2" to 9,
        "1.20.1" to 15,
        "1.21.1" to 34,
    )

    fun atLeast(version: String, floor: String): Boolean {
        val a = version.split('.').map(String::toInt)
        val b = floor.split('.').map(String::toInt)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return true
    }
}
