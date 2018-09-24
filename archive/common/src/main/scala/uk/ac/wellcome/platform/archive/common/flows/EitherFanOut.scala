package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.FanOutShape2
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL}

object EitherFanOut {
  def apply[L, R]() = {
    GraphDSL.create[FanOutShape2[Either[L, R], L, R]]() { implicit builder =>
      import GraphDSL.Implicits._

      val either = builder.add(Flow[Either[L, R]])
      val broadcast = builder.add(Broadcast[Either[L, R]](2))

      either ~> broadcast

      val left = builder.add(Flow[Either[L, R]].collect { case Left(l)   => l })
      val right = builder.add(Flow[Either[L, R]].collect { case Right(r) => r })

      broadcast ~> left
      broadcast ~> right

      new FanOutShape2[Either[L, R], L, R](
        either.in,
        left.out,
        right.out
      )
    }
  }
}
