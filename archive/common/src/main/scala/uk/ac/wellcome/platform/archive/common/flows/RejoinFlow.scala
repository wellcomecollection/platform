package uk.ac.wellcome.platform.archive.common.flows

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}

object RejoinFlow {
  def apply[In, Out](rejoinableFlow: Flow[In, Out, NotUsed]) = {
    GraphDSL.create[FlowShape[In, (In, Out)]]() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[In](2))
      val rejoin = builder.add(rejoinableFlow)
      val zip = builder.add(Zip[In, Out])

      broadcast.out(0) ~> rejoin ~> zip.in1
      broadcast ~> zip.in0

      new FlowShape[In, (In, Out)](
        broadcast.in,
        zip.out
      )
    }
  }
}