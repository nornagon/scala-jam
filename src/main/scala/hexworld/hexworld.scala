package hexworld

import kit._
import org.bytedeco.javacpp.liquidfun._
import org.lwjgl.glfw.GLFW
import scanvas.gpu.GLFWWindow
import scanvas.{Color, Paint, Path}

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

    val world = new b2World(new b2Vec2(0, 0))
    val staticBodyDef = new b2BodyDef()
    staticBodyDef.`type`(b2_staticBody)
    staticBodyDef._position(new b2Vec2(0, 0))
    val staticBody = world.CreateBody(staticBodyDef)

    val avatarBodyDef = new b2BodyDef()
    avatarBodyDef.`type`(b2_dynamicBody)
    avatarBodyDef._position(new b2Vec2(0, 20))
    avatarBodyDef.fixedRotation(true)
    val avatarBody = world.CreateBody(avatarBodyDef)
    val avatarShape = new b2PolygonShape()
    avatarShape.SetAsBox(5, 10)
    val avatarFixtureDef = new b2FixtureDef()
    avatarFixtureDef.density(0.1f)
    avatarFixtureDef.shape(avatarShape)
    avatarBody.CreateFixture(avatarFixtureDef)

    val grid = mutable.Map(
      (0,0)-> Color.White,
      (0,1)-> Color.White,
      (1,1)-> Color.White,
      (1,0)-> Color.White,
      (3,2)-> Color.White,
      (10,5)-> Color.White,
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

    def xform(v:Vec2): Vec2 = {
      val avatarvec = Vec2(avatar.radius * math.sin(avatar.angle), avatar.radius * math.cos(avatar.angle))
      val x_0 = v.x - screen.center.x.toDouble
      val y_0 = v.y - screen.center.y.toDouble
      val x_rot = x_0 * math.cos(avatar.angle) + y_0 * math.sin(avatar.angle) - avatarvec.x
      val y_rot = y_0 * math.cos(avatar.angle) - x_0 * math.sin(avatar.angle) - avatarvec.y
      //      System.err.print("=============================\n\n")
      //      System.err.print("Out x: " + x_rot.toString() + "\n")
      //      System.err.print("Out y: " + y_rot.toString() + "\n\n")
      return Vec2(x_rot, y_rot)
    }


    def triangleAtCoords(x: Int, y: Int): Polygon = {
      val rotation = if (math.abs((x+y)%2)==1) Math.PI else 0d
      val (x_offset,y_offset) = indicesToCoords(x,y)
      val offset = Vec2(x_offset,y_offset)
      Circle2(offset, radius).toPolygon(3, startAngle = rotation)
    }

    def addTriangleToWorld(pos: (Int, Int)): Unit = {
      val triDef = new b2FixtureDef()
      val triShape = new b2PolygonShape()
      val points = triangleAtCoords(pos._1, pos._2).points
      val verts = new b2Vec2(points.size)
      for ((p, i) <- points.zipWithIndex) {
        verts.position(i).x(p.x.toFloat).y(p.y.toFloat)
      }
      triShape.Set(verts.position(0), points.size)
      triDef.shape(triShape)
      staticBody.CreateFixture(triDef)
    }

    grid.keys foreach addTriangleToWorld

    def toggleTri(pos:(Int,Int)): Unit = {
      if (grid.contains(pos))
        grid -= pos
        // TODO: remove from b2World
      else {
        grid += (pos -> Color.White)
        addTriangleToWorld(pos)
      }
    }

    /** Matrix which converts screen coordinates to world coordinates. */
    def worldToScreen: Mat33 = {
      val avatarPos = Vec2(avatarBody.GetPositionX(), avatarBody.GetPositionY())
      // (3) ...then put the center of the world in the middle of the screen.
      Mat33.translate(screen.center) *
        // (2) ...then rotate the world around them so their feet are down...
        Mat33.rotate(avatarBody.GetAngle()) *
        // (1) First put the avatar at (0, 0)...
        Mat33.translate(-avatarPos)
    }

    def screenToWorld: Mat33 = {
      val avatarPos = Vec2(avatarBody.GetPositionX(), avatarBody.GetPositionY())
      // (3) ...then put the avatar in the middle of the screen.
      Mat33.translate(avatarPos) *
      // (2) ...then rotate the world around them so their feet are down...
        Mat33.rotate(-avatarBody.GetAngle()) *
        // (1) First put the avatar's position at (0, 0), the top left corner...
        Mat33.translate(Vec2(0,0)-screen.center)
    }

    def closestToTriangleIndex(x:Double,y:Double): (Int,Int) = {
      // get the window pixel coordinates translated into our rotated /translated game frame.
      val clickpoint = screenToWorld * Vec2(x,y)
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
      val y_row = y_row_options(y_column_ind)
      //
      val avatarPos = Vec2(avatarBody.GetPositionX(), avatarBody.GetPositionY())

      System.err.println("raw x : " + x.toString())
      System.err.println("raw y : " + y.toString())
      System.err.println("world x : " + clickpoint.x.toString())
      System.err.println("world y : " + clickpoint.y.toString())
      System.err.println("screen.center x : " + screen.center.x.toString())
      System.err.println("screen.center y : " + screen.center.y.toString())

      System.err.println("xcol : " + x_column.toString())
      System.err.println("yrow : " + y_row.toString())
//      System.err.println("pol  : " + polarity.toString())
//      System.err.println("xfrac: " + x_fraction.toString())
//      System.err.println("yfrac: " + y_fraction.toString())
//      System.err.println("y_row_opts : " + y_row_options.toString())

      return (x_column,y_row)
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

      //////// Physics /////////
      world.Step(1/30f, 5, 9)
      val avatarPos = Vec2(avatarBody.GetPositionX(), avatarBody.GetPositionY())
      val planetCenter = Vec2(0, 0)
      val angle = if ((planetCenter -> avatarPos).lengthSquared > 1) (planetCenter -> avatarPos).toAngle else 0
      val power = 2000
      if (keysDown contains GLFW.GLFW_KEY_UP) {
        val vec = Vec2.forAngle(angle) * power
        avatarBody.ApplyForceToCenter(new b2Vec2(vec.x.toFloat, vec.y.toFloat), true)
      }
      if (keysDown contains GLFW.GLFW_KEY_DOWN) {
        val vec = Vec2.forAngle(angle) * -power
        avatarBody.ApplyForceToCenter(new b2Vec2(vec.x.toFloat, vec.y.toFloat), true)
      }
      if (keysDown contains GLFW.GLFW_KEY_RIGHT) {
        val vec = Vec2.forAngle(angle + Math.PI/2) * power
        avatarBody.ApplyForceToCenter(new b2Vec2(vec.x.toFloat, vec.y.toFloat), true)
      }
      if (keysDown contains GLFW.GLFW_KEY_LEFT) {
        val vec = Vec2.forAngle(angle - Math.PI/2) * power
        avatarBody.ApplyForceToCenter(new b2Vec2(vec.x.toFloat, vec.y.toFloat), true)
      }
      // Orient the player's feet towards (0,0) (or head, not sure which because symmetry)
      avatarBody.SetTransform(avatarBody.GetPosition(), (angle + Math.PI/2).toFloat)
      val gravityDir = avatarPos -> planetCenter
      if (gravityDir.lengthSquared > 1) { // if they're really close to (0,0) it isn't clear where "down" is
        val gravity = gravityDir.normed * 100
        avatarBody.ApplyForceToCenter(new b2Vec2(gravity.x.toFloat, gravity.y.toFloat), true)
      }

      //////// Graphics /////////
      window.canvas.clear(Color.Black)
      val avatarAngle = avatarBody.GetAngle()
      val avatarX = avatarBody.GetPositionX()
      val avatarY = avatarBody.GetPositionY()
      window.canvas.save()
      val s2w = worldToScreen
      window.canvas.concat(
        s2w.a.toFloat, s2w.b.toFloat, s2w.c.toFloat,
        s2w.d.toFloat, s2w.e.toFloat, s2w.f.toFloat
      )
      window.canvas.save()
      window.canvas.translate(avatarX, avatarY)
      window.canvas.rotate(avatarAngle)
      window.canvas.drawRect(-5f, -10, 10, 20, Paint.blank.setColor(0xffff0000))
      window.canvas.restore()
      for (((x,y),color)<-grid){
        val triangle = triangleAtCoords(x, y)
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
