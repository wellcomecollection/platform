package uk.ac.wellcome.graph

import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV1d0
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper
import org.janusgraph.core.JanusGraphFactory
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

class RelaterTest
    extends FunSpec
    with Matchers
    with Eventually
    with BeforeAndAfterEach {
  private val graph = buildGraph(buildCluster())

  override def beforeEach(): Unit = {
    graph.V().drop().iterate()
    graph.E().drop().iterate()
    graph.E().toList shouldBe empty
    graph.V().toList shouldBe empty

    graph.tx.commit()
    super.beforeEach()
  }
  val relater = new Relater(graph)

  it("adds a new node with edges") {

    relater.updateNode(Node("A", List("B", "C")))

    executeOnGraph { graph =>
      val vertices = graph.V().label().toList
      vertices should contain theSameElementsAs List("A", "B", "C")

      val connectedNodes =
        graph.V().hasLabel("A").out("same-as").label().toList

      connectedNodes should contain theSameElementsAs List("B", "C")
    }
  }

  it("updates a node adding new edges") {
    executeOnGraph { graph =>
      val vertexA = graph.addV("A").next()
      val vertexB = graph.addV("B").next()
      graph
        .V(vertexA.id)
        .as("a")
        .V(vertexB.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
    }

    val connectedNodes = graph.V().hasLabel("A").out("same-as").label().toList

    connectedNodes should contain theSameElementsAs List("B")

    relater.updateNode(Node("A", List("B", "C")))

    executeOnGraph { graph =>
      val vertices = graph.V().label().toList
      vertices should contain theSameElementsAs List("A", "B", "C")

      val connectedNodes =
        graph.V().hasLabel("A").out("same-as").label().toList

      connectedNodes should contain theSameElementsAs List("B", "C")
    }
  }

  it("updates a node removing edges") {
    executeOnGraph { graph =>
      val vertexA = graph.addV("A").next()
      val vertexB = graph.addV("B").next()
      val vertexC = graph.addV("C").next()
      graph
        .V(vertexA.id)
        .as("a")
        .V(vertexB.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
      graph
        .V(vertexA.id)
        .as("a")
        .V(vertexC.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
    }

    val connectedNodes = graph.V().hasLabel("A").out("same-as").label().toList

    connectedNodes should contain theSameElementsAs List("B", "C")

    relater.updateNode(Node("A", List("D")))

    executeOnGraph { graph =>
      val vertices = graph.V().label().toList
      vertices should contain theSameElementsAs List("A", "B", "C", "D")

      val connectedNodes =
        graph.V().hasLabel("A").out("same-as").label().toList

      connectedNodes should contain theSameElementsAs List("D")
    }
  }

  it("return the connected graph that a node is part of") {
    executeOnGraph { graph =>
      val vertexA = graph.addV("A").next()
      val vertexB = graph.addV("B").next()
      val vertexC = graph.addV("C").next()
      val vertexD = graph.addV("D").next()
      graph
        .V(vertexA.id)
        .as("a")
        .V(vertexB.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
      graph
        .V(vertexC.id)
        .as("a")
        .V(vertexA.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
      graph
        .V(vertexD.id)
        .as("a")
        .V(vertexC.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
    }

    relater.getConnectedGraph("A") should contain theSameElementsAs List("A",
                                                                         "B",
                                                                         "C",
                                                                         "D")
  }

  it("handles connected graphs with cycles") {
    executeOnGraph { graph =>
      val vertexA = graph.addV("A").next()
      val vertexB = graph.addV("B").next()
      val vertexC = graph.addV("C").next()
      val vertexD = graph.addV("D").next()
      graph
        .V(vertexA.id)
        .as("a")
        .V(vertexB.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
      graph
        .V(vertexC.id)
        .as("a")
        .V(vertexA.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
      graph
        .V(vertexD.id)
        .as("a")
        .V(vertexC.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
      graph
        .V(vertexB.id)
        .as("a")
        .V(vertexD.id)
        .as("b")
        .addE("same-as")
        .from("a")
        .to("b")
        .iterate()
    }

    relater.getConnectedGraph("A") should contain theSameElementsAs List("A",
                                                                         "B",
                                                                         "C",
                                                                         "D")
  }

  it("return the node as a single connected graph") {
    executeOnGraph { graph =>
      graph.addV("A").next()
    }

    relater.getConnectedGraph("A") should contain theSameElementsAs List("A")
  }

  private def executeOnGraph(f: GraphTraversalSource => Any): Any = {
    val cluster: Cluster = buildCluster()
    val graph = buildGraph(cluster)
    val result = f(graph)
    graph.close()
    cluster.close()
    result
  }

  private def buildGraph(cluster: Cluster) = {
    JanusGraphFactory
      .open("inmemory")
      .traversal()
      .withRemote(DriverRemoteConnection.using(cluster))
  }

  private def buildCluster() = {
    val serializer = new GryoMessageSerializerV1d0(
      GryoMapper.build().addRegistry(JanusGraphIoRegistry.getInstance()))
    Cluster
      .build()
      .addContactPoint("localhost")
      .port(45679)
      .serializer(serializer)
      .create()
  }
}
