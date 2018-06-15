package uk.ac.wellcome.platform.matcher.lockable
import grizzled.slf4j.Logging
import javax.inject.Inject
import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

class DynamoLockingService @Inject()(dynamoRowLockDao: DynamoRowLockDao)(
  implicit context: ExecutionContext) extends Logging {

  def withLocks[T](ids: Set[String])(f: => Future[T]): Future[T] = {
    val contextGuid = randomUUID.toString
    val identifiers: Set[Identifier] = ids.map(Identifier)
    debug(s"Locking identifiers $identifiers")
    val eventuallyExecutedWithLock = for {
      _ <- Future.sequence(identifiers.map(dynamoRowLockDao.lockRow(_, contextGuid)))
      result <- f
    } yield {
      debug(s"Released locked identifiers $identifiers")
      result
    }
    eventuallyExecutedWithLock.transformWith { triedResult =>
      dynamoRowLockDao.unlockRow(contextGuid).flatMap(
        _ => Future.fromTry(triedResult))
    }
  }
}
