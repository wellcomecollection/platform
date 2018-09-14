import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent._
import scala.concurrent.duration._


case class WholeThing(i: Int)
case class PartThing(partOf: Int, i: Int)
case class DoneThing(partThings: Seq[PartThing])

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
  def fail(part: PartThing) = {
    if(math.random < 0.25) {
      throw new RuntimeException("BLARG")
    }

    part
  }
}

val source = Source(1 to 10)
val flow = Flow[Int]
  .map(WholeThing(_))
  .flatMapMerge(10, t => {
    Source(1 to 10).map(PartThing(t.i, _)).map(Failbot.fail)
  })
  .groupBy(Int.MaxValue, _.partOf)
  .fold(DoneThing(Nil))((doneThing, part) => {
    doneThing.copy(partThings = doneThing.partThings :+ part)
  })
  .mergeSubstreams
  //.map(o => o.partThings.head.partOf)

val sink: Sink[DoneThing, Future[immutable.Seq[DoneThing]]] = Sink.seq

val futureSeq = source.via(flow).toMat(sink)(Keep.right).run()

val seq = Await.result(futureSeq, 30 seconds)