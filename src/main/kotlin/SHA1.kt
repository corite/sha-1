import kotlin.math.nextUp

@ExperimentalUnsignedTypes
class SHA1 {

    fun hash(bytes:UByteArray):UByteArray {
        val blocks = preprocess(bytes)

        val h0 = "67452301".toUInt(16)
        val h1 = "EFCDAB89".toUInt(16)
        val h2 = "98BADCFE".toUInt(16)
        val h3 = "10325476".toUInt(16)
        val h4 = "C3D2E1F0".toUInt(16)

        var h = uintArrayOf(h0,h1,h2,h3,h4)
        for (block in blocks) {
            h = processBlock(block, h)
        }

        return getBytes(h[0])+getBytes(h[1])+getBytes(h[2])+getBytes(h[3])+getBytes(h[4])
    }

    fun hashToString(bytes: UByteArray):String {
        val hashedBytes = hash(bytes)

        return hashedBytes.joinToString("") { padWithZeros(it.toString(16), 2) }

    }

    private fun processBlock(bytes: UByteArray, h:UIntArray):UIntArray {
        var (h0,h1,h2,h3,h4) = h

        val words = getWordsFromBytes(bytes)
        val extendedWords = words+UIntArray(80 - 16)
        for (i in 16 until 80) {
            extendedWords[i] =(extendedWords[i-3] xor extendedWords[i-8] xor extendedWords[i-14] xor extendedWords[i-16]) rotl 1
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
            val tmp = (a rotl 5) + f + e + k +extendedWords[i]
            e = d
            d = c
            c = b rotl 30
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


    private fun getWordsFromBytes(bytes:UByteArray):UIntArray {
        val words = UIntArray(bytes.size/4)
        for (i in words.indices) {
            val chunk = bytes.copyOfRange(i*4, (i+1)*4)
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
        return bytes.joinToString("") { padWithZeros(it.toString(16), 2) }.toUInt(16)
    }

    private fun padWithZeros(number:String, amount:Int) :String {
        return "0".repeat(amount-number.length)+number
    }


    /**
     * custom infix function that rotates a given UInt by a specified amount
     */
    infix fun UInt.rotl(amount:Int):UInt {
        val distance = amount % UInt.SIZE_BITS //reduce to the minimal amount of shifting
        val mask = "1".repeat(distance).toUInt(2) shl (UInt.SIZE_BITS-distance)//mask to get the bits that will be shifted out on the left side
        val pushedOutBits = (this and mask) shr (UInt.SIZE_BITS-distance)//the actual bits that get shifted out, but "normalized" to the LSBs (right side of the word)
        return (this shl distance) or pushedOutBits
    }


}