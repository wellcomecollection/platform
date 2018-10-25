package uk.ac.wellcome.platform.archive.common.flows

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Zip}
import uk.ac.wellcome.platform.archive.common.progress.models.FailedEvent

import scala.util.Try

/** This process takes a function (In => Try[Out]), and flattens the
  * result into an Either[FailedEvent[In], Out].
  *
  * It partitions the flow into Left/Right, with the failures on
  * one side (and in particular, the _reason_ for the failures), and
  * the successes on the other.
  *
  */
object EitherFlow {

  def apply[In, Out](
    f: In => Try[Out]): Flow[In, Either[FailedEvent[In], Out], NotUsed] = {

    val broadcastInFlow = Broadcast[In](2)
    val processFlow = Flow[In].map(in => f(in).toEither)
    val eitherFlow = EitherFanOut[Throwable, Out]()

    val wrapProcessFlow =
      Flow[(In, (Option[Throwable], Option[Out]))].map {
        case (in, (Some(e), None))  => Left(FailedEvent(e, in))
        case (_, (None, Some(out))) => Right(out)
      }

    val mergeFlow = Merge[(Option[Throwable], Option[Out])](2)
    val zipFlow = Zip[In, (Option[Throwable], Option[Out])]

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcastIn = builder.add(broadcastInFlow)
      val process = builder.add(processFlow)
      val either = builder.add(eitherFlow)
      val merge = builder.add(mergeFlow)
      val zip = builder.add(zipFlow)
      val wrapProcess = builder.add(wrapProcessFlow)

      broadcastIn.out(0) ~> process ~> either.in

      either.out0.map(error => (Some(error), None)) ~> merge.in(0)
      either.out1.map(out => (None, Some(out))) ~> merge.in(1)

      broadcastIn.out(1) ~> zip.in0
      merge.out ~> zip.in1

      zip.out ~> wrapProcess

      FlowShape(broadcastIn.in, wrapProcess.out)
    })

  }
}
