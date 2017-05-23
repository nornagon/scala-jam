package hexworld

import kit.{AABB, Circle2, Vec2}
import org.lwjgl.glfw.GLFW
import scanvas.{Color, Paint, Path}
import scanvas.gpu.GLFWWindow
import System.err
import scala.collection.mutable

/**
  * Created by nato on 5/19/17.
  */
object hexworld {
  def main(args: Array[String]): Unit = {
    val window = new GLFWWindow(1024, 768, "hexworld")

    val screen = AABB(0, 0, window.width, window.height)
    case class Avatar(var radius:Double, var angle:Double)
    var avatar:Avatar = Avatar(50d,0d)


    val grid = mutable.Map(
      (0,0)-> Color.White,
      (0,1)-> Color.White,
      (1,1)-> Color.White,
      (1,0)-> Color.White,
      (3,2)-> Color.White,
      (2,7)-> Color.White)
    var t = 0.0


    def toggleTri(pos:(Int,Int)): Unit = {
      if (grid.contains(pos))
        grid -= pos
      else
        grid += (pos-> Color.White)
    }

    // geometry calcs
    val radius = 12
    val wiggle_mag = radius/Math.sqrt(15)
    val x_period = 3*radius/2
    val y_period = radius*Math.sqrt(3)/2

    def indicesToCoords(x:Int,y:Int):(Double,Double)={
      val wiggle_dir = if (math.abs((x+y)%2)==1) 1 else -1
      val x_offset = x * x_period + wiggle_dir * wiggle_mag
      val y_offset = y*y_period
      return (x_offset,y_offset)
    }

    def closestToTriangleIndex(x:Double,y:Double): (Int,Int) = {
      val dx:Double = x - screen.center.x.toDouble
      val dy:Double = y - screen.center.y.toDouble - avatar.radius
      System.err.println(dx)
      System.err.println(dy)
      return ((dx/x_period).toInt,(dy/y_period).toInt)
    }

    val keysDown = mutable.Set.empty[Int]
    window.onKeyDown = (key: Int, scanCode: Int, mods: Int) => {
      keysDown += key
    }
    window.onKeyUp = (key: Int, scanCode: Int, mods: Int) => {
      keysDown -= key
    }
    window.onMouseDown = (x:Double, y:Double, button:Int) => {
      toggleTri(closestToTriangleIndex(x,y))
    }

    while (!window.shouldClose) {
      if (keysDown contains GLFW.GLFW_KEY_LEFT) {
        avatar.angle += 30d * 1/60 / (avatar.radius)
      }
      if (keysDown contains GLFW.GLFW_KEY_RIGHT) {
        avatar.angle -= 30d * 1/60 / (avatar.radius)
      }
      if (keysDown contains GLFW.GLFW_KEY_UP) {
        avatar.radius += 25 *1/60.0
      }
      if (keysDown contains GLFW.GLFW_KEY_DOWN) {
        avatar.radius -= 25 *1/60.0
      }
      window.canvas.clear(Color.Black)
      window.canvas.save()
      window.canvas.translate(screen.center.x.toFloat, screen.center.y.toFloat)
      window.canvas.drawRect(-4, -8, 8, 16, Paint.blank.setColor(0xffff0000))
      window.canvas.translate(0, avatar.radius.toFloat)
      window.canvas.rotate(avatar.angle.toFloat)
      for (((x,y),color)<-grid){
        val rotation = if (math.abs((x+y)%2)==1) Math.PI else 0d
        val (x_offset,y_offset) = indicesToCoords(x,y)
        val offset = Vec2(x_offset,y_offset)
        val triangle = Circle2(offset, radius).toPolygon(3, startAngle = rotation)
        val path = Path.empty
        path.moveTo(triangle.points(0).x.toFloat, triangle.points(0).y.toFloat)
        for (point <- triangle.points.drop(1))
          path.lineTo(point.x.toFloat, point.y.toFloat)
        path.close()
        window.canvas.drawPath(path, Paint.blank.setColor(Color.White).setStyle(Paint.Style.StrokeAndFill).setStrokeWidth(1))
      }

      window.canvas.restore()
      window.canvas.flush()
      window.swapBuffers()
      t += 1/60d
    }
  }
}
