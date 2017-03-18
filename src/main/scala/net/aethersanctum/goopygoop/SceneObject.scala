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
  def prototypes:List[String]
  def commonDeclarations:List[String]
}

object SceneObjectType {
  def registry: List[SceneObjectType] = List(
    Plane,
    Sphere
  )
}

case class Plane(normal: Vector3, offset: Double) extends SceneObject(Plane) {
  override def declarations = List(
    s"__constant double3 plane_${id}_normal = { ${normal.x}, ${normal.y}, ${normal.z} };",
    s"__constant double  plane_${id}_offset = ${offset};"
  )

  override def distanceInvocation = s"plane_distance(point, plane_${id}_normal, plane_${id}_offset)"

  override def normalInvocation: String = s"plane_${id}_normal"
}

object Plane extends SceneObjectType {
  def prototypes:List[String] = List(
    "double plane_distance(double3 point, double3 normal, double offset);"
  )

  def commonDeclarations:List[String] = List(
    "double plane_distance(double3 point, double3 normal, double offset) {",
    "  return length(point * dot(normalize(point), normal)) + offset;",
    "}"
  )
}

case class Sphere(center: Vector3, radius: Double) extends SceneObject(Sphere) {
  override def declarations = List(
    s"__constant double3 sphere_${id}_center = { ${center.x}, ${center.y}, ${center.z} };",
    s"__constant double  sphere_${id}_radius = ${radius};"
  )

  override def distanceInvocation = s"sphere_distance(point, sphere_${id}_center, sphere_${id}_radius)"
  override def normalInvocation = s"sphere_normal(point, sphere_${id}_center)"
}

object Sphere extends SceneObjectType {
  def prototypes:List[String] = List(
    "double sphere_distance(double3 point, double3 center, double radius);"
  )
  def commonDeclarations:List[String] = List(
    "double sphere_distance(double3 point, double3 center, double radius) {",
    "  return length(point - center) - radius;",
    "}",
    "double3 sphere_normal(double3 point, double3 center) {",
    "  return normalize(point - center);",
    "}"
  )
}