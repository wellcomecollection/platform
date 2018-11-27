package uk.ac.wellcome.platform.merger.rules.physicaldigital

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.merger.rules.Partition

class SierraPhysicalDigitalPartitionerTest
    extends FunSpec
    with WorksGenerators
    with Matchers {

  private val partitioner = new SierraPhysicalDigitalPartitioner {}
  private val physicalWork = createSierraPhysicalWork
  private val digitalWork = createSierraDigitalWork
  private val otherWorks = createUnidentifiedWorks(4)

  it("partitions a physical and digital work") {
    val result = partitioner.partitionWorks(Seq(physicalWork, digitalWork))

    result shouldBe Some(Partition(physicalWork, digitalWork, Nil))
  }

  it("partitions a physical and digital work, order in sequence") {
    val result = partitioner.partitionWorks(Seq(digitalWork, physicalWork))

    result shouldBe Some(Partition(physicalWork, digitalWork, Nil))
  }

  it("partitions a physical, digital and other works") {
    val result =
      partitioner.partitionWorks(Seq(physicalWork, digitalWork) ++ otherWorks)

    result shouldBe Some(Partition(physicalWork, digitalWork, otherWorks))
  }

  it("does not partition a single physical work") {
    val result = partitioner.partitionWorks(Seq(physicalWork))

    result shouldBe None
  }

  it("does not partition a single digital work") {
    val result = partitioner.partitionWorks(Seq(digitalWork))

    result shouldBe None
  }

  it("does not partition multiple physical works") {
    val works = (1 to 3).map { _ =>
      createSierraPhysicalWork
    }

    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }

  it("does not partition multiple digital works") {
    val works = (1 to 3).map { _ =>
      createSierraDigitalWork
    }
    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }

  it("does not partition multiple non digital or physical works") {
    val result = partitioner.partitionWorks(otherWorks)

    result shouldBe None
  }

  it("does not partition multiple physical works with a single digital work") {
    val works = (1 to 3).map { _ =>
      createSierraPhysicalWork
    } ++ Seq(createSierraDigitalWork)

    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }

  it("does not partition multiple digital works with a single physical work") {
    val works = (1 to 3).map { _ =>
      createSierraDigitalWork
    } ++ Seq(createSierraPhysicalWork)

    val result = partitioner.partitionWorks(works)

    result shouldBe None
  }
}
