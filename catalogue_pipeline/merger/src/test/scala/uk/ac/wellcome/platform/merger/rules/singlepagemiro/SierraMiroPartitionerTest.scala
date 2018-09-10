package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.test.util.WorksGenerators

class SierraMiroPartitionerTest
    extends FunSpec
    with Matchers
    with WorksGenerators {

  val partitioner = new SierraMiroPartitioner {}

  it("extracts a sierra and miro work") {
    val sierraWork = createSierraWork
    val miroWork = createMiroWork

    val result = partitioner.partitionWorks(Seq(sierraWork, miroWork))

    result shouldBe Some(partitioner.Partition(sierraWork, miroWork, Nil))
  }
}
