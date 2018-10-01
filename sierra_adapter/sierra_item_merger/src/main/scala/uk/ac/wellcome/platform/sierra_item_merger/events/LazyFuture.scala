package uk.ac.wellcome.platform.sierra_item_merger.events

import scala.concurrent.{ExecutionContext, Future}

// We've seen issues in the sierra_item_merger when a single item has lots
// of bib records attached -- we open lots of connections to VHS (and so S3)
// simultaneously, drain the local HTTP connection pool, and error out.
// Items with lots of bibs never complete.  Sadness.
//
// Because we trigger those VHS executions with Future.sequence, this is
// a wrapper for Future that lets us directly cap the number of threads
// executed in Future.sequence.
//
// It's based on code from this answer on Stack Overflow:
// https://stackoverflow.com/q/49924941/1558022

case class LazyFuture[T](f: Unit => Future[T]) {
  def apply(): Future[T] = f(())
}

case object LazyFuture {
  def apply[T](f: => T)(implicit ec: ExecutionContext): LazyFuture[T] =
    LazyFuture(_ => Future(f))

  def apply[T](f: => Future[T]): LazyFuture[T] = LazyFuture(_ => f)
}

object BatchExecutor {
  def execute[T](futures: Seq[LazyFuture[T]])(concurFactor: Int)(implicit ec: ExecutionContext): Future[Seq[T]] =
    futures
      .grouped(concurFactor)
      .foldLeft(Future.successful(List.empty[T])) { (completedFutures, newFutures) =>
        val batch = Future.sequence(newFutures.map { _ () })
        completedFutures.flatMap {
          cf => batch.map { values => cf ++ values }
        }
      }
}
