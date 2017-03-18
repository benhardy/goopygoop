package net.aethersanctum.goopygoop.demo

import net.aethersanctum.goopygoop.{Sphere, _}

object Demo {
  def main(args:Array[String]):Unit = {
    val rendering = new Rendering(500, 400, MarchOptions.QUICK)
    val scene = Scene(
      objects = List(
        Plane(normal = (0.0, 1.0, 0.0), offset = 0.0),
        Sphere(center = (0.0, 1.0, 0.0), radius = 1),
        Sphere(center = (3.0, 1.0, 0.0), radius = 1)
      ),
      cameraPosition = (-1.0, 5.0, -20.0),
      lookAt = (0.0, 0.0, 0.0)
    )
    val runner = new GpuRunner(rendering, scene)
    runner.run
    rendering.save("Demo.png")
  }
}
