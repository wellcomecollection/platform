package uk.ac.wellcome.platform.archive.common.flows
import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}

/** This flow combines an Either[L, R] and emits instances of Out
  * for both sides.
  *
  * It starts with the Right flow (which we assume is the happy path),
  * and adds elements from the Left flow if/when they occur.
  *
  *       R --- ifRight ---\
  *                         +---> Out
  *       L --- ifLeft ----/
  *
  */
object FoldEitherFlow {
  def apply[L, R, Out](ifLeft: Flow[L, Out, NotUsed])(
    ifRight: Flow[R, Out, NotUsed]): Flow[Either[L, R], Out, NotUsed] = {
    Flow.fromGraph(
      GraphDSL.create(
        ifLeft,
        ifRight,
        Broadcast[Either[L, R]](2),
        Merge[Out](2))((_, _, _, _) => NotUsed) { implicit builder =>
        import GraphDSL.Implicits._
        (ifLeftFlow, ifRightFlow, broadcast, merge) =>
          {
            broadcast
              .out(0)
              .collect { case Right(something) => something } ~> ifRightFlow.in
            ifRightFlow.out ~> merge.in(0)

            broadcast.out(1).collect {
              case Left(something) => something
            } ~> ifLeftFlow.in

            ifLeftFlow.out ~> merge.in(1)
            FlowShape(broadcast.in, merge.out)
          }

      })
  }
}
