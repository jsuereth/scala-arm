package resource

import java.io._
import java.net.URL
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.jar.{JarInputStream, JarOutputStream, JarFile}
import java.util.zip._

/** Convenience methods for common java IO operations.
  *
  *
  * This API attempts to do two things:
  *
  * 1. Clean up common java IO operations and conversions, e.g. always returning "Buffered" streams rather than raw ones.
  * 2. Avoid frustrating errors when dealing with the filesystem, e.g. trying to write to a file whose parent directories
  *    do not exist yet.
  *
  * Note: This code is ported from the sbt IO library.
  */
object Using {

  /** Converts a function which creates an OutputStream into a ManagedResource[BufferedOutputStream]. */
  def bufferedOutputStream(out: => OutputStream): ManagedResource[BufferedOutputStream] =
      managed(out).map(new BufferedOutputStream(_))

  /** Converts a function which creates an InputStream into a ManagedResource[BufferedInputStream]. */
  def bufferedInputStream(in: => InputStream): ManagedResource[BufferedInputStream] =
    managed(in).map(new BufferedInputStream(_))

  /** Creates a managed resource which works against something constructed out of a file.
    *
    * Note: This ensures that the directory in which the file lives is created prior to opening the resource, which is
    *       why it has a bit of an odd signature.
    */
  def file[T: Resource : OptManifest](in : File => T)(source: File): ManagedResource[T] = {
    def open: T = {
      val parent = source.getParentFile
      if(parent != null) {
        // TODO - use an IO library for this
        parent.mkdirs()
      }
      in(source)
    }
    managed(open)
  }

  /** Creates a new BufferedInputStream for a given file. */
  def fileInputStream(source: File): ManagedResource[BufferedInputStream] = file(f => new BufferedInputStream(new FileInputStream(f)))(source)
  /** Creates a new BufferedInputStream for a given file. */
  def fileOutputStream(source: File): ManagedResource[BufferedOutputStream] = file(f => new BufferedOutputStream(new FileOutputStream(f)))(source)
  /** Creates a new input stream from a java.net.URL. */
  def urlInputStream(url: URL): ManagedResource[BufferedInputStream] = bufferedInputStream(url.openStream)  // TODO - fix error messages to include source URL.
    /** Creates a new FileOutputChannel given a file.
      * Note: This will ensure the parent directory for the file exists before opening the file.
      */
  def fileOuputChannel(source: File): ManagedResource[FileChannel] = file(f => new FileOutputStream(f).getChannel)(source)
  /** Creates a new FileChannel given an input file.
    * Note: This will ensure the parent directory for the file exists before opening.
    */
  def fileInputChannel(source: File): ManagedResource[FileChannel] = file(f => new FileInputStream(f).getChannel)(source)

  private val utf8 = Charset.forName("UTF-8")

  /** Constructs a file writer for a file.  Defaults to UTF-8 encoding if no other encoding is specified. */
  def fileWriter(charset: Charset = utf8, append: Boolean = false)(source: File): ManagedResource[BufferedWriter] =
    file(f => new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, append), charset)))(source)
  /** Constructs a file reader for a file.  Defaults to UTF-8 encoding if no other encoding is specified. */
  def fileReader(charset: Charset)(source: File): ManagedResource[BufferedReader] =
    file(f => new BufferedReader(new InputStreamReader(new FileInputStream(f), charset)))(source)
  /** Constructs a Traversable which will open and read a file's lines every time it is traversed. */
  def fileLines(charset: Charset)(source: File): Traversable[String] =
     fileReader(charset)(source).map(makeBufferedReaderLineTraverser).toTraversable
  /** Constructs a new buffered reader for a URL. */
  def urlReader(charset: Charset)(u: java.net.URL): ManagedResource[BufferedReader] = managed(new BufferedReader(new InputStreamReader(u.openStream, charset)))
  /** Constructs a new JarFile reader. */
  def jarFile(verify: Boolean)(source: File): ManagedResource[JarFile] = file(f => new JarFile(f, verify))(source)
  /** Constructs a new ZipFile reader. */
  def zipFile(source: File): ManagedResource[ZipFile] = file(f => new ZipFile(f))(source)
  /** Creates a new managed Reader which reads from an input stream using the given charset. */
  def streamReader(in: InputStream, charset: Charset): ManagedResource[Reader] = managed(new InputStreamReader(in, charset))
  /** Constructs a new  managed GzipInputStream from a normal IntputStream.
    * Note: Default buffer size is 8192.
    */
  def gzipInputStream(in: => InputStream): ManagedResource[GZIPInputStream] = managed(new GZIPInputStream(in, 8192))
  /** Constructs a new  managed ZipInputStream from a normal InputStream. */
  def zipInputStream(in: => InputStream): ManagedResource[ZipInputStream] = managed(new ZipInputStream(in))
  /** Constructs a new  managed ZipOutputStream from a normal OutputStream. */
  def zipOutputStream(out: => OutputStream): ManagedResource[ZipOutputStream] = managed(new ZipOutputStream(out))
  /** Constructs a new outpustream which ensures the GZIP is "finished" after completing our operation.
    * Note: Default buffer size is 8192.
    */
  def gzipOutputStream(out: => OutputStream): ManagedResource[GZIPOutputStream] = managed(new GZIPOutputStream(out, 8192))
  /** Creates a resource which converts an OutputStream into a JarOutputStream and ensures it is closed. */
  def jarOutputStream(out: => OutputStream): ManagedResource[JarOutputStream] = managed(new JarOutputStream(out))
  /** Creates a resource which converts an InputStream into a JarInputStream and ensures it is closed. */
  def jarInputStream(in: => InputStream): ManagedResource[JarInputStream] = managed(new JarInputStream(in))
  /** Creates a resource which opens/closes for a particular entry in a zip file. */
  def zipEntry(zip: ZipFile)(entry: ZipEntry): ManagedResource[InputStream] = managed(zip.getInputStream(entry))


  /** Creates a new iterator which reads the lines of a file. */
  private def makeBufferedReaderLineTraverser(reader: BufferedReader): TraversableOnce[String] = {
    object traverser extends Traversable[String] {
      def foreach[U](f: String => U): Unit = {
        def read(): Unit =
          reader.readLine match {
            case null => ()
            case line => f(line); read()
          }
        read()
      }
      override def toString = s"BufferedReaderLineIterator($reader)"
    }
    traverser
  }
}
