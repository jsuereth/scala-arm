package resource

import java.io.{ BufferedReader, File }

import java.nio.charset.Charset

import scala.collection.Iterable

private[resource] trait UsingCompat { _: Using.type =>

  /** 
   * Returns a `Iterable` which will open 
   * and read a file's lines every time it is traversed. 
   */
  def fileLines(charset: Charset)(source: File): Iterable[String] =
    fileReader(charset)(source).map(
      makeBufferedReaderLineTraverser).toIterable

  /** Creates a new iterator which reads the lines of a file. */
  private def makeBufferedReaderLineTraverser(reader: BufferedReader): IterableOnce[String] = {
    val readerIterator = new Iterator[String] {
      private val initialHasNext: () => Boolean = () => reader.synchronized {
        reader.readLine match {
          case null => {
            _hasNext = () => false
            _next = () => throw new NoSuchElementException()
            false
          }

          case line => {
            _hasNext = () => true

            _next = () => {
              // reset as going forward
              _hasNext = initialHasNext
              _next = initialNext

              line
            }

            true
          }
        }
      }

      private val initialNext: () => String = () => reader.synchronized {
        reader.readLine match {
          case null => {
            _hasNext = () => false
            _next = () => throw new NoSuchElementException()

            throw new NoSuchElementException()
          }

          case line => line
        }
      }

      private var _hasNext = initialHasNext
      private var _next = initialNext

      def hasNext: Boolean = _hasNext()
      def next: String = _next()
    }

    object traverser extends Iterable[String] {
      override def foreach[U](f: String => U): Unit = {
        @annotation.tailrec
        def read(): Unit = reader.readLine match {
          case null => ()
          case line => f(line); read()
        }

        read()
      }

      def iterator = readerIterator

      override def toString = s"BufferedReaderLineIterator($reader)"
    }

    traverser
  }

}
