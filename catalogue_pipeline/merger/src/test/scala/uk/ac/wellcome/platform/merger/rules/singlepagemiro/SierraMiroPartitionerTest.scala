package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.merger.rules.Partition

class SierraMiroPartitionerTest
    extends FunSpec
    with Matchers
    with WorksGenerators {

  private val partitioner = new SierraMiroPartitioner {}
  private val sierraWork = createUnidentifiedSierraWork
  private val miroWork = createMiroWork
  private val otherWorks = createIsbnWorks(4)

  it("partitions a sierra and miro work") {
    val result = partitioner.partitionWorks(Seq(sierraWork, miroWork))

    result shouldBe Some(Partition(sierraWork, miroWork, Nil))
  }

  it("partitions a Sierra and Miro work, order in sequence") {
    val result = partitioner.partitionWorks(Seq(miroWork, sierraWork))

    result shouldBe Some(Partition(sierraWork, miroWork, Nil))
  }

  it("partitions a Sierra, Miro and other works") {
    val result =
      partitioner.partitionWorks(Seq(sierraWork, miroWork) ++ otherWorks)

    result shouldBe Some(Partition(sierraWork, miroWork, otherWorks))
  }

  it("does not partition a single Sierra work") {
    val result = partitioner.partitionWorks(Seq(sierraWork))

    result shouldBe None
  }

  it("does not partition a single Miro work") {
    val result = partitioner.partitionWorks(Seq(miroWork))

    result shouldBe None
  }

  it("does not partition multiple Sierra works") {
    val works = (1 to 3).map { _ =>
      createSierraPhysicalWork
    }

    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }

  it("does not partition multiple Miro works") {
    val works = (1 to 3).map { _ =>
      createSierraDigitalWork
    }
    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }

  it("does not partition multiple non Miro or Sierra works") {
    val result = partitioner.partitionWorks(otherWorks)

    result shouldBe None
  }

  it("does not partition multiple Sierra works with a single Miro work") {
    val works = (1 to 3).map { _ =>
      createSierraPhysicalWork
    } ++ Seq(createSierraDigitalWork)

    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }

  it("does not partition multiple Miro works with a single Sierra work") {
    val works = (1 to 3).map { _ =>
      createSierraDigitalWork
    } ++ Seq(createSierraPhysicalWork)

    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }
}
