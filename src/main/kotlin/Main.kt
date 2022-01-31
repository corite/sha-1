@ExperimentalUnsignedTypes
fun main() {
    val sha1 = SHA1()
    println(sha1.hashToString("".encodeToByteArray().asUByteArray()))


    //todo: use infix fun
    //todo: hash-to-string fun
}