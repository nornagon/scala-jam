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
    case class Avatar(var radius:Double, var angle:Double) {
      var inventory = 100
    }
    var avatar:Avatar = Avatar(50d,0d)


    val grid = mutable.Map(
      (0,0)-> Color.White,
      (0,1)-> Color.White,
      (1,1)-> Color.White,
      (1,0)-> Color.White,
      (3,2)-> Color.White,
      (2,7)-> Color.White)
    var t = 0.0


    def adjacent(pos:(Int,Int)): List[(Int,Int)] = {
      val xp = pos._1
      val yp = pos._2
      val polarity = math.abs(xp + yp)%2
      val checkval = if(xp<0) {0} else {1}
      if (polarity==checkval) {
        return List((xp,yp+1),(xp,yp-1),(xp+xp.signum,yp))
      } else {
        return List((xp,yp+1),(xp,yp-1),(xp-xp.signum,yp))
      }
    }

    def toggle(pos:(Int,Int)): Unit = {
      val depth = 5
      if (grid.contains(pos))
        mine(pos,depth,OFF)
      else
        mine(pos,depth,ON)
    }

    def mine(pos:(Int,Int),depth:Int, state:TriState):Unit = {
      if (depth==0) return {}
      setTri(pos,state)
      for (p<-adjacent(pos)) {
        mine(p,depth-1,state)
      }
      return {}
    }

    trait TriState {}
    case object ON extends TriState
    case object OFF extends TriState

    def setTri(pos:(Int,Int),state:TriState): Unit = {
      state match {
        case ON => {
          if (!grid.contains(pos) & avatar.inventory > 0) {
            grid += (pos -> Color.White)
            avatar.inventory -= 1
          }
        }
        case OFF => {
          if (grid.contains(pos)) {
            grid -= pos
          avatar.inventory += 1
          }
        }
      }
    }

    // geometry calcs
    val radius = 10
    val wiggle_mag = radius/Math.sqrt(15)
    val x_period = 3*radius/2
    val y_period = radius*Math.sqrt(3)/2

    def indicesToCoords(x:Int,y:Int):(Double,Double)={
      val wiggle_dir = if (math.abs((x+y)%2)==1) 1 else -1
      val x_offset = x * x_period + wiggle_dir * wiggle_mag - x_period / 2
      val y_offset = y*y_period
      return (x_offset,y_offset)
    }

    def xform(v:Vec2): Vec2 ={
      val avatarvec = Vec2(avatar.radius*math.sin(avatar.angle),avatar.radius*math.cos(avatar.angle))
      val x_0 = v.x - screen.center.x.toDouble
      val y_0 = v.y - screen.center.y.toDouble
      val x_rot = x_0*math.cos(avatar.angle) + y_0*math.sin(avatar.angle) - avatarvec.x
      val y_rot = y_0*math.cos(avatar.angle) - x_0*math.sin(avatar.angle) - avatarvec.y
//      System.err.print("=============================\n\n")
//      System.err.print("Out x: " + x_rot.toString() + "\n")
//      System.err.print("Out y: " + y_rot.toString() + "\n\n")
      return Vec2(x_rot,y_rot)
    }

    def closestToTriangleIndex(x:Double,y:Double): (Int,Int) = {
      // get the window pixel coordinates translated into our rotated /translated game frame.
      val clickpoint = xform(Vec2(x,y))
      // figure out what column we're in

      val x_val = if (clickpoint.x<0) {
        (clickpoint.x / x_period)
      } else {
        ((clickpoint.x + x_period) / x_period)
      }

      val y_val = if (clickpoint.y<0) {
        clickpoint.y / y_period
      } else {
        clickpoint.y / y_period
      }


      val x_column = x_val.toInt
      val x_fraction = math.abs(x_val-x_column)

      val y_fraction = math.abs(y_val - y_val.toInt)
      val polarity = math.abs((x_val.toInt+y_val.toInt)%2)

      val polcheck = if(x_val>0) {1} else {0}
      val y_column_ind = if(polarity==polcheck) {
        if (y_fraction > x_fraction) {1} else {0}
      } else {
        if (y_fraction > 1 - x_fraction) {1} else {0}
      }

      val y_row_options = List(y_val.toInt,y_val.toInt + y_val.signum)
//
//
//      System.err.println("xval : " + x_val.toString())
//      System.err.println("yval : " + y_val.toString())
//      System.err.println("pol  : " + polarity.toString())
//      System.err.println("xfrac: " + x_fraction.toString())
//      System.err.println("yfrac: " + y_fraction.toString())
//      System.err.println("y_row_opts : " + y_row_options.toString())

      return (x_column,y_row_options(y_column_ind))
    }

    val keysDown = mutable.Set.empty[Int]
    window.onKeyDown = (key: Int, scanCode: Int, mods: Int) => {
      keysDown += key
    }
    window.onKeyUp = (key: Int, scanCode: Int, mods: Int) => {
      keysDown -= key
    }
    window.onMouseDown = (x:Double, y:Double, button:Int) => {
      toggle(closestToTriangleIndex(x,y))
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
