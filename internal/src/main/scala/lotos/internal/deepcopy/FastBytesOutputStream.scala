package lotos.internal.deepcopy

import java.io.OutputStream

class FastBytesOutputStream(val initSize: Int = 5 * 1024) extends OutputStream {
  var buf: Array[Byte] = new Array[Byte](initSize)
  var size             = 0

  private def verifyBufferSize(sz: Int): Unit = {
    if (sz > buf.length) {
      var old = buf
      buf = new Array[Byte](Math.max(sz, 2 * buf.length))
      System.arraycopy(old, 0, buf, 0, old.length)
      old = null
    }
  }

  def getSize: Int = size

  def getByteArray: Array[Byte] = buf

  override final def write(b: Array[Byte]): Unit = {
    verifyBufferSize(size + b.length)
    System.arraycopy(b, 0, buf, size, b.length)
    size += b.length
  }

  override final def write(b: Array[Byte], off: Int, len: Int): Unit = {
    verifyBufferSize(size + len)
    System.arraycopy(b, off, buf, size, len)
    size += len
  }

  final def write(b: Int): Unit = {
    verifyBufferSize(size + 1)
    buf({
      size += 1; size - 1
    }) = b.toByte
  }

  def reset(): Unit = {
    size = 0
  }

  def getInputStream = new FastBytesInputStream(buf, size)
}
