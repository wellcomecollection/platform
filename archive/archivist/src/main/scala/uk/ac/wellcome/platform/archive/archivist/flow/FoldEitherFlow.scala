package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}

object FoldEitherFlow {
  def apply[L, R, Out](ifLeft: L => Out)(
    ifRight: Flow[R, Out, NotUsed]): Flow[Either[L, R], Out, NotUsed] = {
    Flow.fromGraph(
      GraphDSL.create(ifRight, Broadcast[Either[L, R]](2), Merge[Out](2))(
        (_, _, _) => NotUsed) { implicit builder =>
        import GraphDSL.Implicits._
        (ifRightFlow, broadcast, merge) =>
          {
            broadcast
              .out(0)
              .collect { case Right(something) => something } ~> ifRightFlow.in
            ifRightFlow.out ~> merge.in(0)

            broadcast.out(1).collect {
              case Left(something) => ifLeft(something)
            } ~> merge.in(1)
            FlowShape(broadcast.in, merge.out)
          }

      })
  }
}
