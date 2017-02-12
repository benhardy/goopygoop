package net.aethersanctum.goopygoop

case class Scene(objects: List[SceneObject]) {
  def declarations = objects.flatMap(_.declarations)
}
