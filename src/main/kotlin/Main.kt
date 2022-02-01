@ExperimentalUnsignedTypes
fun main() {
    val sha1 = SHA1()
    println(sha1.hashToString("".encodeToByteArray().asUByteArray()))
}