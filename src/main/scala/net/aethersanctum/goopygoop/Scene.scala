package net.aethersanctum.goopygoop

case class Scene(
                objects: List[SceneObject],
                cameraPosition: Vector3,
                lookAt: Vector3,
                up: Vector3 = Vector3.Y
                ) {
  val typesPresent = objects.map(_.objectType).toSet
  def declarations = List(
    s"__constant double3 camera_position = { ${cameraPosition.x}, ${cameraPosition.y}, ${cameraPosition.z} };",
    s"__constant double3 look_at = { ${lookAt.x}, ${lookAt.y}, ${lookAt.z} };",
    s"__constant double3 up = { ${up.x}, ${up.y}, ${up.z} };"
  ) ++ objects.flatMap(_.declarations)
}
