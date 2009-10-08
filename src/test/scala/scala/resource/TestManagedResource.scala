package scala.resource

import _root_.java.{io => jio}

/**
 * This is a basic abstraction for an iterator that fetches new content as needed.
 */
abstract class FetchIterator[T] extends Iterator[T] {
	var fetched = false
	var nextItem : Option[T] = None
	protected def fetchNext(): Option[T]
	override def hasNext = {
		if (!fetched) {
			nextItem = fetchNext()
			fetched = true
		}
		nextItem.isDefined
	}
	override def next() = {
		if (!hasNext) throw new NoSuchElementException("EOF")
		fetched = false
		nextItem.get
	}
}

/** This class creates an iterator from a buffered iterator.  Used to test toTraverable method */
class JavaBufferedReaderLineIterator(br : jio.BufferedReader) extends FetchIterator[String] {
  override def fetchNext() = br.readLine() match {
    case null => None
    case s => Some(s)
  }
}
