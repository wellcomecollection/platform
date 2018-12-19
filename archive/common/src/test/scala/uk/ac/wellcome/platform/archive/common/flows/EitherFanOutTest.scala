package uk.ac.wellcome.platform.archive.common.flows

import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.fixtures.Akka

class EitherFanOutTest
    extends FunSpec
    with Akka
    with Matchers
    with ScalaFutures {

  it("sorts Right from Left") {
    withMaterializer { implicit materializer =>
      val students = List(
        Right("Hermione"),
        Left("Goyle"),
        Right("Harry"),
        Left("Snape")
      )

      val rightFlow = Flow[String]

      val sortingHat = Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val r = builder.add(rightFlow)

        val e = builder.add(EitherFanOut[String, String]())
        val s = builder.add(Sink.ignore)

        e.out0 ~> s
        e.out1 ~> r

        FlowShape(e.in, r.out)
      })

      val source = Source.fromIterator(() => students.toIterator)
      val sink = Sink.seq[String]

      val eventualResult = source
        .via(sortingHat)
        .async
        .runWith(sink)

      whenReady(eventualResult) { result =>
        result.toList shouldBe students.collect {
          case Right(o) => o
        }
      }
    }
  }

  it("sorts Left from Right") {
    withMaterializer { implicit materializer =>
      val students = List(
        Right("Hermione"),
        Left("Goyle"),
        Right("Harry"),
        Left("Snape")
      )

      val leftFlow = Flow[String]

      val sortingHat = Flow.fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val l = builder.add(leftFlow)

        val e = builder.add(EitherFanOut[String, String]())
        val s = builder.add(Sink.ignore)

        e.out0 ~> l
        e.out1 ~> s

        FlowShape(e.in, l.out)
      })

      val source = Source.fromIterator(() => students.toIterator)
      val sink = Sink.seq[String]

      val eventualResult = source
        .via(sortingHat)
        .async
        .runWith(sink)

      whenReady(eventualResult) { result =>
        result.toList shouldBe students.collect {
          case Left(o) => o
        }
      }
    }
  }
}
