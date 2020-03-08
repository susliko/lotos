package lotos.internal.deepcopy

import java.io.{InputStream, ObjectInputStream, ObjectOutputStream, OutputStream}

import scala.util.Try

class FastByteArrayOutputStream(val initSize: Int = 5 * 1024) extends OutputStream {
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

  def getInputStream = new FastByteArrayInputStream(buf, size)
}

class FastByteArrayInputStream(buf: Array[Byte], count: Int) extends InputStream {

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

object DeepCopy {

  def copy[T <: AnyRef](orig: T): Either[Throwable, T] = {
    Try {
      val fbos = new FastByteArrayOutputStream();
      val out  = new ObjectOutputStream(fbos)
      out.writeObject(orig)
      out.flush()
      out.close()
      val in = new ObjectInputStream(fbos.getInputStream)
      in.readObject.asInstanceOf[T]
    }.toEither
  }
}
