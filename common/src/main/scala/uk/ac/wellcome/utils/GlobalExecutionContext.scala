package uk.ac.wellcome.utils

import scala.concurrent.ExecutionContext

object GlobalExecutionContext {
  // This ensures we have enough threads when running in ECS.  If we use
  // the implicit global execution context, the application is thread-starved
  // and records fall into a hole.  See the discussion on
  // https://github.com/wellcometrust/platform-api/issues/159 for details

  implicit val context =
    ExecutionContext.fromExecutor(new java.util.concurrent.ForkJoinPool(64))
}
