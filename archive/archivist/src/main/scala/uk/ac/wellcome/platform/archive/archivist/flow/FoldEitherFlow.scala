package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge}

object FoldEitherFlow {
  def apply[L, R, Out](ifLeft: L => Out)(
    ifRight: Flow[R, Out, NotUsed]): Flow[Either[L,R], Out, NotUsed] = {
    Flow.fromGraph(GraphDSL.create(Broadcast[Either[L,R]](2),Merge[Out](2))(Keep.none){implicit builder =>
      import GraphDSL.Implicits._
      (broadcast, merge) => {
        val f = builder.add(ifRight)
        broadcast.out(0).collect{case Right(something) => something} ~> f.in
        f.out ~> merge.in(0)

        broadcast.out(1).collect{case Left(something) => ifLeft(something)} ~> merge.in(1)
        FlowShape(broadcast.in, merge.out)
      }

    })
  }
}
