package resource

import java.io.{ BufferedReader, File }

import java.nio.charset.Charset

import scala.collection.Traversable

private[resource] trait UsingCompat { _: Using.type =>

  /** 
   * Returns a `Traversable` which will open 
   * and read a file's lines every time it is traversed. 
   */
  def fileLines(charset: Charset)(source: File): Traversable[String] =
    fileReader(charset)(source).map(
      makeBufferedReaderLineTraverser).toTraversable

  /** Creates a new iterator which reads the lines of a file. */
  private def makeBufferedReaderLineTraverser(reader: BufferedReader): TraversableOnce[String] = {
    object traverser extends Traversable[String] {
      override def foreach[U](f: String => U): Unit = {
        @annotation.tailrec
        def read(): Unit = reader.readLine match {
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
