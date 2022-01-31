import kotlin.math.nextUp

@ExperimentalUnsignedTypes
class SHA1 {

    fun hash(message:UByteArray):UByteArray {
        val blocks = preprocess(message)

        val h0 = "67452301".toUInt(16)
        val h1 = "EFCDAB89".toUInt(16)
        val h2 = "98BADCFE".toUInt(16)
        val h3 = "10325476".toUInt(16)
        val h4 = "C3D2E1F0".toUInt(16)

        var h = uintArrayOf(h0,h1,h2,h3,h4)
        for (block in blocks) {
            h = processBlock(block, h)
        }

        return getBytes(h0)+getBytes(h1)+getBytes(h2)+getBytes(h3)+getBytes(h4)
    }

    private fun processBlock(bytes: UByteArray, h:UIntArray):UIntArray {
        var (h0,h1,h2,h3,h4) = h

        val words = getWordsFromBytes(bytes)
        val extendedWords = words+UIntArray(80 - 16)
        for (i in 16 until 80) {
            extendedWords[i] = leftRotate(extendedWords[i-3] xor extendedWords[i-8] xor extendedWords[i-14] xor extendedWords[i-16])
        }

        var (a,b,c,d,e) = arrayOf(h0,h1,h2,h3,h4)
        var f:UInt
        var k:UInt

        for (i in 0 until 80) {
            if (i< 20) {
                f = (b and c) or ((b.inv()) and d)
                k = "5A827999".toUInt(16)
            } else if (i < 40) {
                f = b xor c xor d
                k = "6ED9EBA1".toUInt(16)
            } else if (i < 60) {
                f = (b and c) or (b and d) or (c and d)
                k = "8F1BBCDC".toUInt(16)
            } else {
                f = b xor c xor d
                k = "CA62C1D6".toUInt(16)
            }
            val tmp = leftRotate(a,5) + f + e + k +extendedWords[i]
            e = d
            d = c
            c = leftRotate(b, 30)
            b = a
            a = tmp
        }
        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e

        return uintArrayOf(h0,h1,h2,h3,h4)
    }

    private fun leftRotate(word: UInt):UInt {
        val msb = word and (1 shl 31).toUInt()
        return (word shl 1) or msb
    }

    private fun leftRotate(word: UInt, amount: Int):UInt {
        var result = word
        for (i in 0 until amount) {
            result = leftRotate(result)
        }
        return result
    }

    private fun getWordsFromBytes(bytes:UByteArray):UIntArray {
        val words = UIntArray(16)
        for (i in 0 until 16 ) {
            val chunk = bytes.copyOfRange(i*16, (i+1)*16)
            words[i] = getWord(chunk)
        }
        return words
    }

    private fun preprocess(message: UByteArray):Array<UByteArray> {
        val ml = (message.size*8).toULong()
        val messageWithOne = message+"80".toUByte(16)
        val paddedMessage = pad448Mod512(messageWithOne)
        val messageWithMl = paddedMessage+getBytes(ml)
        return splitTo512Sections(messageWithMl)
    }

    private fun pad448Mod512(message: UByteArray):UByteArray {
        val mod = (message.size*8) % 512
        return if (mod <= 448) {
            message+UByteArray((448-mod)/8)
        } else {
            message+UByteArray((512-mod+448)/8)
        }
    }

    private fun getBytes(num:ULong):UByteArray {
        val mask = "FF00000000000000".toULong(16)
        val bytes = UByteArray(8)
        for (i in 0..7) {
            bytes[i] = ((num and (mask shr (8*i))) shr ((7-i)*8)).toUByte()
        }
        return bytes
    }

    private fun getBytes(word: UInt):UByteArray {
        val mask:UInt = "FF000000".toUInt(16)
        val bytes = UByteArray(4)
        for (i in 0..3) {
            bytes[i] = ((word and (mask shr (8*i))) shr ((3-i)*8)).toUByte()
        }
        return bytes
    }

    private fun splitTo512Sections(bytes: UByteArray):Array<UByteArray> {
        val numberOfChunks = (bytes.size/64.0).nextUp().toInt()
        val chunkedBytes = Array(numberOfChunks) {UByteArray(64)}
        for (i in 0 until numberOfChunks) {
            val chunk = bytes.copyOfRange(i*64, (i+1)*64)
            chunkedBytes[i] = chunk
        }
        return chunkedBytes
    }

    private fun getWord(bytes:UByteArray):UInt {
        return bytes.joinToString("") { padHex(it.toString(16), 2) }.toUInt(16)
    }

    private fun padHex(number:String, amount:Int) :String {
        return "0".repeat(amount-number.length)+number
    }


}