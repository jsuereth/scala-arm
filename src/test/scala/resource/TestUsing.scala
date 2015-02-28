package resource

import java.io.File
import java.nio.charset.Charset

import org.junit.Test
import org.junit.Assert._

/**
 * Tests for the Using helpers.
 */
class TestUsing {


  @Test
  def fileReaderWriter(): Unit = {
    val tmp = File.createTempFile("test", "txt")
    val expected = "Example file"
    for {
      out <- Using.fileWriter(Charset.defaultCharset)(tmp)
    } out.write(expected)
    val read =
      Using.fileReader(Charset.defaultCharset)(tmp).acquireAndGet(_.readLine)
    assertEquals("Failed to successfully read from files", expected, read)
  }

  @Test
  def fileStreams(): Unit = {
    val tmp = File.createTempFile("testst", "txt")
    val expected = Array[Byte](1.toByte, 2.toByte, 3.toByte, 4.toByte)
    for {
      out <- Using.fileOutputStream(tmp)
    } out.write(expected)
    val buf = new Array[Byte](4)
    Using.fileInputStream(tmp).foreach(_.read(buf, 0, 4))
    assertEquals("Failed to successfully read from files", expected.toSeq, buf.toSeq)
  }
  @Test
  def fileChannels(): Unit = {
    val tmp = File.createTempFile("test", "txt")
    val tmp2 = File.createTempFile("test2", "txt")
    val expected = "Example file"
    for {
      out <- Using.fileWriter(Charset.defaultCharset)(tmp)
    } out.write(expected)

    // Use channels to copy the file.
    for {
      in <- Using.fileInputChannel(tmp)
      out <- Using.fileOuputChannel(tmp2)
    } out.transferFrom(in, 0, in.size)
    val read =
      Using.fileReader(Charset.defaultCharset)(tmp2).acquireAndGet(_.readLine)
    assertEquals("Failed to successfully read from files", expected, read)

  }
}
