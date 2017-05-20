package example

import scanvas._
import scanvas.gpu.GLFWWindow

import scala.collection.mutable
import scala.util.Random

object lwjgltest {
  def main(args: Array[String]): Unit = {
    val window = new GLFWWindow(1000, 600, "Skia Scala")
    val canvas = window.canvas

    val hellos = mutable.Buffer.empty[(Float, Float)]
    for (_ <- 1 to 100)
      hellos.append((Random.nextFloat() * 800 + 100, Random.nextFloat() * 400 + 100))

    window.onMouseDown = (x, y, button) => hellos.append((x.toFloat, y.toFloat))

    window.show()

    val textPaint = Paint.blank
      .setAntiAlias(true)
      .setSubpixelText(true)
      .setTypeface(Typeface.fromName("Helvetica", Typeface.Style.Normal))
      .setTextAlign(Paint.TextAlign.Center)
      .setColor(Color.Black)

    val blur = textPaint.clone()
      .setColor(Color.RGBA(0, 0, 0, 0x7f))
      .setMaskFilter(MaskFilter.blur(4))

    var i = 0
    while (!window.shouldClose) {
      import canvas._
      clear(Color.White)
      for ((x, y) <- hellos) {
        save()
        translate(x, y)
        rotateDegrees(i)
        drawText("Hello world", 0, 0, blur)
        drawText("Hello world", 0, 0, textPaint)
        restore()
      }
      flush()

      window.swapBuffers()

      i += 1
    }
  }
}
