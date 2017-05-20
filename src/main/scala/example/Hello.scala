package example
import processing.core._

class Hello extends PApplet {
  override def settings(): Unit = { size(300, 300) }
  override def setup(): Unit = { fill(120, 50, 240) }
  override def draw(): Unit = {
    fill(120, 50, 240)
    ellipse(width/2, height/2, 30, 30)
  }
}

object Hello {
  /*def main(args: Array[String]): Unit = {
    PApplet.main("example.Hello")
  }*/
}
