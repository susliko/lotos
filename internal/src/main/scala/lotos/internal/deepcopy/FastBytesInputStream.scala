package lotos.internal.deepcopy

import java.io.InputStream

class FastBytesInputStream(buf: Array[Byte], count: Int) extends InputStream {

  var pos: Int = 0

  override final def available: Int = count - pos

  override final def read: Int =
    if (pos < count) buf({
      pos += 1; pos - 1
    }) & 0xff
    else -1

  override final def read(b: Array[Byte], off: Int, length: Int): Int = {
    val newLength =
      if (pos >= count) -1
      else if (pos + length > count) count - pos
      else length
    System.arraycopy(buf, pos, b, off, newLength)
    pos += newLength
    newLength
  }

  override final def skip(amount: Long): Long = {
    val n: Long =
      if (pos + amount > count) count - pos
      else if (amount < 0) 0
      else amount
    pos += n.toInt
    n
  }
}
