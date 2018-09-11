package uk.ac.wellcome.platform.merger.rules.physicaldigital

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.test.util.WorksGenerators

class SierraPhysicalDigitalPartitionerTest extends FunSpec with WorksGenerators with Matchers {

  private val partitioner = new SierraPhysicalDigitalPartitioner {}

  describe("filters physical and digital works") {
    it("extracts a physical and digital work") {
      val physicalWork = createSierraPhysicalWork
      val digitalWork = createSierraDigitalWork

      val result = partitioner.partitionWorks(Seq(physicalWork, digitalWork))

      result shouldBe Some(partitioner.Partition(physicalWork, digitalWork, Nil))
    }

    it("extracts a physical, digital and other works") {
      val physicalWork = createSierraPhysicalWork
      val digitalWork = createSierraDigitalWork
      val otherWorks = createUnidentifiedWorks(4)

      val result = partitioner.partitionWorks(Seq(physicalWork, digitalWork) ++ otherWorks)

      result shouldBe Some(partitioner.Partition(physicalWork, digitalWork, otherWorks))
    }

    it("ignores a single physical work") {
      val works = Seq(createSierraPhysicalWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores a single digital work") {
      val works = Seq(createSierraDigitalWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple physical works") {
      val works = (1 to 3).map { _ =>
        createSierraPhysicalWork
      }

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple digital works") {
      val works = (1 to 3).map { _ =>
        createSierraDigitalWork
      }
      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple physical works with a single digital work") {
      val works = (1 to 3).map { _ =>
        createSierraPhysicalWork
      } ++ Seq(createSierraDigitalWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }

    it("ignores multiple digital works with a single physical work") {
      val works = (1 to 3).map { _ =>
        createSierraDigitalWork
      } ++ Seq(createSierraPhysicalWork)

      val result = partitioner.partitionWorks(works)

      result shouldBe None
    }
  }
}
