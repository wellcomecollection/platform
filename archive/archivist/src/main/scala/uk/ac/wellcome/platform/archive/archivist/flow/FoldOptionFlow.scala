package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge}

object FoldOptionFlow {
  def apply[In, Out](default: Out)(flow: Flow[In, Out, NotUsed]): Flow[Option[In], Out, NotUsed] = {
    Flow.fromGraph(GraphDSL.create(Broadcast[Option[In]](2),Merge[Out](2))(Keep.none){implicit builder =>
      import GraphDSL.Implicits._
      (broadcast, merge) => {
        val f = builder.add(flow)
        broadcast.out(0).collect{case Some(inputStream) => inputStream} ~> f.in
        f.out ~> merge.in(0)

        broadcast.out(1).collect{case None => default} ~> merge.in(1)
        FlowShape(broadcast.in, merge.out)
      }

    })
  }
}
