package uk.ac.wellcome.type_classes

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record.Updater
import shapeless.record._

trait VersionUpdater[T] {
  def updateVersion(versioned: T, newVersion: Int): T
}

object VersionUpdater {
  val w = Witness(Symbol("version"))
  type version = w.T

  type versionField = FieldType[version, Int]

  def apply[A](implicit enc: VersionUpdater[A]): VersionUpdater[A] =
    enc

  def createVersionUpdater[T](f: (T,Int) => T): VersionUpdater[T] = new VersionUpdater[T] {
      def updateVersion(t: T, version: Int) = f(t,version)
  }

  implicit def elementVersionUpdater[L <: HList](implicit updater: Updater.Aux[L, versionField, L]) = createVersionUpdater[L] {(t: L, newVersion: Int) =>
    t.updated('version, newVersion)
  }

  implicit def hlistVersionUpdater[L <: HList, H](implicit versionGetter: VersionUpdater[L]) = createVersionUpdater[H :: L]{ (t: H :: L, newVersion: Int) =>
    t.head :: versionGetter.updateVersion(t.tail,newVersion)
  }

  implicit def productVersionUpdater[C, T](implicit gen: LabelledGeneric.Aux[C,T], versionGetter: VersionUpdater[T]) = createVersionUpdater[C]{(t: C, newVersion: Int)=>
    gen.from(versionGetter.updateVersion(gen.to(t), newVersion))
  }
}