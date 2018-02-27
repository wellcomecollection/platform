package uk.ac.wellcome.graph

import java.util.Optional

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import scala.collection.JavaConversions._

case class Node(label: String, connectedTo: List[String])

class Relater(graph: GraphTraversalSource) {

  def getConnectedGraph(str: String): List[String] = {
    val list = graph
      .V()
      .hasLabel(str)
      .emit()
      .repeat(graph.V().both("same-as"))
      .until(graph.V().cyclicPath())
      .label()
      .toSet
      .toList
    list
  }

  def updateNode(node: Node): Unit = {

    val maybeStartNode = graph.V().hasLabel(node.label).tryNext()
    val startNode = maybeStartNode.getOrElse(graph.addV(node.label).next())

    graph.V(startNode.id).outE("same-as").drop().toList

    node.connectedTo.foreach { destLabel =>
      val maybeDestNode = graph.V().hasLabel(destLabel).tryNext()
      val destNode = maybeDestNode.getOrElse(graph.addV(destLabel).next())

      val maybeEdge =
        graph.V(startNode.id).out("same-as").hasLabel(destLabel).tryNext()

      if (maybeEdge.isEmpty) {
        graph
          .V(startNode.id)
          .as("a")
          .V(destNode.id)
          .as("b")
          .addE("same-as")
          .from("a")
          .to("b")
          .iterate()
      }

    }
  }

  implicit def toOption[T](optional: Optional[T]): Option[T] =
    if (optional.isPresent) Some(optional.get()) else None
}
