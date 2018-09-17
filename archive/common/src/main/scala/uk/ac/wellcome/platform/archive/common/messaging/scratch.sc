//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.{Await, Future}
//
//import scala.concurrent.duration._
//
//val f = Future {
//  throw new RuntimeException("nope")
//}
//
//Await.result(f, 1 second)

import java.security.MessageDigest

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, PartitionHub, RunnableGraph, Sink, Source, SubFlow, Zip}
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import grizzled.slf4j.Logging

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


case class WholeThing(i: Int)
case class PartThing(partOf: Int, i: Int)

case class Wrapper(partitionKey: Int, result: Try[PartThing])
case class DoneThing(partThings: Seq[Wrapper])

val decider: Supervision.Decider = {
  case e => {
    println(e)
    Supervision.Resume
  }
}

implicit val system = ActorSystem("test")
implicit val materializer = ActorMaterializer(
  ActorMaterializerSettings(system).withSupervisionStrategy(decider)
)

object Failbot {
  def fail(part: PartThing) = Try {
    if(math.random < 0.1) {
      throw new RuntimeException("BLARG")
    }

    part
  }
}
val source = Source(1 to 10)
val flow = Flow[Int]
  .map(WholeThing(_))
  .flatMapMerge(10, t => {
    Source(1 to 10)
      .map(PartThing(t.i, _))
      .map(partThing => Wrapper(t.i,Failbot.fail(partThing)))
  })
  .groupBy(Int.MaxValue, _.partitionKey)
  .fold(DoneThing(Nil))((doneThing, part) => {
    doneThing.copy(partThings = doneThing.partThings :+ part)
  })
  .mergeSubstreams
  .filterNot(_.partThings.exists(_.result.isFailure))
  .map(_.partThings)


val sink: Sink[Seq[Wrapper], Future[Seq[Seq[Wrapper]]]] = Sink.seq

val futureSeq = source.via(flow).toMat(sink)(Keep.right).run()

val seq = Await.result(futureSeq, 30 seconds)