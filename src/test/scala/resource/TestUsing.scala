package resource

import java.io._
import java.nio.charset.Charset
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

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

    Using.fileWriter(Charset.defaultCharset)(tmp).foreach(_.write(expected))

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

    Using.fileInputStream(tmp).foreach { i => i.read(buf, 0, 4); () }

    assertEquals("Failed to successfully read from files", expected.toSeq, buf.toSeq)
  }

  @Test
  def fileChannels(): Unit = {
    val tmp = File.createTempFile("test", "txt")
    val tmp2 = File.createTempFile("test2", "txt")
    val expected = "Example file"

    Using.fileWriter(Charset.defaultCharset)(tmp).foreach(_.write(expected))

    // Use channels to copy the file.
    (Using.fileInputChannel(tmp) and Using.fileOuputChannel(tmp2)).foreach {
      case (in, out) => out.transferFrom(in, 0, in.size); ()
    }

    val read =
      Using.fileReader(Charset.defaultCharset)(tmp2).acquireAndGet(_.readLine)

    assertEquals("Failed to successfully read from files", expected, read)
  }


  def urls(): Unit = {
    val tmp = File.createTempFile("test", "txt")
    val expected = "Example file"
    for(out <- Using.fileWriter(Charset.defaultCharset)(tmp)) out.write(expected)

    val read =
      Using.urlInputStream(tmp.toURI.toURL).map(r => new BufferedReader(new InputStreamReader(r))).acquireAndGet(_.readLine)
    assertEquals("Failed to successfully read from url", expected, read)
  }

  def testGzip(): Unit = {
    val tmp = File.createTempFile("test", "gzip")
    val expected = "Example file"
      Using.gzipOutputStream(new GZIPOutputStream(new FileOutputStream(tmp))) foreach { out =>
        out.write(expected.getBytes)
      }

    val buf = new Array[Byte](expected.getBytes().length)
    Using.gzipInputStream(new GZIPInputStream(new FileInputStream(tmp))) acquireAndGet { in =>
      in.read(buf)
    }

    val result = new String(buf)
    assertEquals("Failed to successfully read from gzip", expected, result)
  }
}
