package net.aethersanctum.goopygoop

abstract class SceneObject(val objectType: SceneObjectType) {
  def declarations: List[String]
  def distanceInvocation: String
  def normalInvocation: String
  val id: Int = SceneObject.nextId
}

object SceneObject {
  var maxId = 0
  def nextId = {
    maxId = maxId + 1
    maxId
  }
}

trait SceneObjectType {
  def commonDeclarations:List[String]
}

object SceneObjectType {
  def registry: List[SceneObjectType] = List(
    Plane
  )
}

case class Plane(normal: Vector3, offset: Double) extends SceneObject {
  val id = SceneObject.nextId
  override def declarations = List(
    s"__constant double3 plane_${id}_normal = { ${normal.x}, ${normal.y}, ${normal.z} };",
    s"__constant double  plane_${id}_offset = ${offset};"
  )

  override def distanceInvocation = s"plane_distance(point, plane_${id}_normal, plane_${id}_offset)"
}

object Plane extends SceneObjectType {
  def commonDeclarations:List[String] = List(
    "double plane_distance(double3 point, double3 normal, double offset) {",
    "  return length(point * dot(normalize(point), normal)) + offset;",
    "}"
  )
}