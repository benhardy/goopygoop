package net.aethersanctum.goopygoop

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

case class MarchOptions(
    epsilon: Double,
    maxDistance: Double,
    stepRatio: Double,
    maxSteps: Int
)

object MarchOptions {
  val QUICK = MarchOptions(epsilon = 0.001, maxDistance = 100, stepRatio = 0.5, maxSteps = 200)
  val FINE = MarchOptions(epsilon = 0.00001, maxDistance = 1000, stepRatio = 0.1, maxSteps = 1000)
}

class Rendering(val screenWidth:Int, val screenHeight:Int, val march: MarchOptions) {
  val aspectRatio = screenHeight.toDouble / screenWidth

  val image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB)

  def save(filename: String) {
    System.out.println(s"saving $filename")
    ImageIO.write(image, "png", new File(filename))
  }

  def setPixel(x:Int, y:Int, red:Int, green:Int, blue:Int) {
    image.setRGB(x, y, (red << 16) | (green << 8) | blue)
  }

  val RGB_RANGE = 255

  def setPixel(x:Int, y:Int, red:Float, green:Float, blue:Float) {
    setPixel(x, y, (RGB_RANGE * red).toInt, (RGB_RANGE * green).toInt, (RGB_RANGE * blue).toInt)
  }
}

object Rendering {
  def main(args:Array[String]): Unit = {
    val rendering = new Rendering(640, 480, MarchOptions.QUICK)
    for (y:Int <- 0 until rendering.screenHeight; x:Int <- 0 until rendering.screenWidth) {
      val check = ((x ^ y) & 0x20) > 0
      val red = if (check) 0 else (y & 0xff)
      val green = if (check) 0 else 255
      val blue = if (check) 0 else (x & 0xff)
      rendering.setPixel(x, y, red, green, blue)
    }
    rendering.save("RenderingTest.png")
  }
}
