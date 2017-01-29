package net.aethersanctum.goopygoop

import java.awt.image.BufferedImage
import java.awt.{Color, Graphics2D}
import java.io.File
import javax.imageio.ImageIO


class Rendering(val screenWidth:Int, val screenHeight:Int) {
  val aspectRatio = screenHeight.toDouble / screenWidth

  val image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB)

  val ctx = image.getGraphics.asInstanceOf[Graphics2D]

  def save(filename: String) {
    System.out.println(s"saving $filename")
    ImageIO.write(image, "png", new File(filename))
  }

  def setPixel(x:Int, y:Int, color:Color) {
    ctx.setColor(color)
    ctx.fillRect(x, y, 1, 1)
  }
}

object Rendering {
  def main(args:Array[String]): Unit = {
    val rendering = new Rendering(640, 480)
    for (y:Int <- 0 until rendering.screenHeight; x:Int <- 0 until rendering.screenWidth) {
      val color = if (((x ^ y) & 0x20) > 0) Color.BLACK else Color.WHITE
      rendering.setPixel(x, y, color)
    }
    rendering.save("RenderingTest.png")
  }
}
