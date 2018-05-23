package uk.ac.wellcome.storage.type_classes

import shapeless.ops.hlist.{Align, Intersection}
import shapeless.{HList, LabelledGeneric}

trait Migration[Source, Target] {
  def apply(src: Source): Target
}

object Migration {
  implicit class MigrationOps[Source](src: Source) {
    def migrateTo[Target](implicit migration: Migration[Source, Target]): Target =
      migration.apply(src)
  }

  implicit def genericMigration[Source, Target, SRepr <: HList, TRepr <: HList, Unaligned <: HList](
    implicit
    sourceGen: LabelledGeneric.Aux[Source, SRepr],
    targetGen: LabelledGeneric.Aux[Target, TRepr],
    intersection: Intersection.Aux[SRepr, TRepr, Unaligned],
    align: Align[Unaligned, TRepr]
  ): Migration[Source, Target] = new Migration[Source, Target] {
    def apply(src: Source): Target =
      targetGen.from(align.apply(intersection.apply(sourceGen.to(src))))
  }
}
