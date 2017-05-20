package example

import example.CanvasHelpers._
import kit.RandomImplicits._
import kit._
import org.lwjgl.glfw.GLFW
import scanvas._
import scanvas.gpu.GLFWWindow

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Random

trait Component {
  def receive(world: World, e: Entity): PartialFunction[Any, Unit] = PartialFunction.empty
}

/*abstract class ComponentProps[Props, C <: Component](build: Props => C) {
  self: Props =>
  def instantiate: C = build(this)
}

case class EntityDef(
  id: String,
  name: String,
  componentProps: Seq[ComponentProps[_, _ <: Component]]
)*/

class Entity private(initialComponents: Seq[Component]) {
  private val components = mutable.Buffer.empty[Component] ++ initialComponents

  def component[T: ClassTag]: Option[T] = components.collectFirst {
    case t: T => t
  }

  def has[T: ClassTag]: Boolean = component[T].isDefined

  def apply[T: ClassTag]: T = component[T].get

  def receive(world: World, message: Any) =
    components foreach { _.receive(world, this).applyOrElse(message, (_: Any) => ()) }
}

object Entity {
  /*def fromDefinition(etype: EntityDef): Entity = {
    of(etype.componentProps.map(_.instantiate): _*)
  }*/
  def apply(components: Component*): Entity = {
    new Entity(components)
  }
}

class World {
  private val entities: mutable.Buffer[Entity] = mutable.Buffer.empty[Entity]
  private val deadEntities: mutable.Buffer[Entity] = mutable.Buffer.empty[Entity]

  def entitiesWith[A: ClassTag]: Seq[Entity] = entities.view.filter(e => e.has[A])
  def entitiesWith[A: ClassTag, B: ClassTag]: Seq[Entity] = entities.view.filter(e => e.has[A] && e.has[B])

  def remove(e: Entity): Unit = deadEntities.append(e)
  def add(e: Entity): Entity = { entities.append(e); e }

  def send(e: Entity, m: Any) = {
    e.receive(this, m)
  }
}

case class Extent(
  shape: Polygon,
  var pos: Vec2,
  var rot: Double
) extends Component {
}

case class Velocity(
  var vel: Vec2,
  var angVel: Double
) extends Component {
}

case class Breakable(size: Int) extends Component {
  override def receive(world: World, e: Entity): PartialFunction[Any, Unit] = {
    case Destroy() =>
      if (size > 0) {
        (1 to size) foreach { _ =>
          world.add(Entities.makeAsteroid(e[Extent].pos + Rand.withinCircle(size * 20), size - 1))
        }
      }
  }
}

case class Destroy()
case class Destroyer() extends Component
case class Destroyable() extends Component {
  override def receive(world: World, e: Entity) = {
    case Destroy() =>
      world.remove(e)
  }
}

case class Player() extends Component
trait Drawable {
  def draw(entity: Entity, canvas: Canvas): Unit
}
case class DrawExtent() extends Component with Drawable {
  private val paint = Paint.blank.setStyle(Paint.Style.Stroke).setStrokeWidth(2).setColor(Color.White)
  override def draw(entity: Entity, canvas: Canvas): Unit = {
    val extent = entity[Extent]
    canvas.save()
    canvas.translate(extent.pos)
    canvas.rotate(extent.rot)
    val path = Path.empty.polygon(extent.shape)
    canvas.drawPath(path, paint)
    canvas.restore()
  }
}

case class Input() extends Component {
  override def receive(world: World, e: Entity): PartialFunction[Any, Unit] = {
    case 'fire =>
      if (world.entitiesWith[Destroyer].size < 3) {
        world.add(Entities.makeBullet(e[Extent].pos + Vec2.forAngle(e[Extent].rot) * 15, e[Extent].rot))
      }
    case ('left, dt: Double) =>
      e[Extent].rot -= dt * 2.3
    case ('right, dt: Double) =>
      e[Extent].rot += dt * 2.3
    case ('thrust, dt: Double) =>
      e[Velocity].vel += Vec2.forAngle(e[Extent].rot) * 90 * dt
  }
}

object Entities {
  def makeAsteroid(pos: Vec2, size: Int): Entity = {
    Entity(
      Extent(Asteroids.makeAsteroidShape(size - 1, Random), pos, Rand.angle),
      Velocity(Random.withinCircle(80 / (size - 1)), Random.between(-0.2, 0.2)),
      Destroyable(),
      Breakable(size - 1),
      DrawExtent()
    )
  }

  def makePlayer(pos: Vec2): Entity = {
    Entity(
      Extent(
        shape = Circle2(Vec2(0, 0), 10).toPolygon(3),
        pos,
        rot = 0
      ),
      Velocity(Vec2(0, 0), 0),
      Destroyable(),
      DrawExtent(),
      Input()
    )
  }

  def makeBullet(pos: Vec2, angle: Double): Entity = {
    Entity(
      Extent(
        shape = Polygon(Seq(pos, pos + Vec2.forAngle(angle) * 5)),
        pos,
        angle
      ),
      Velocity(Vec2.forAngle(angle) * 5, 0),
      Destroyer(),
      DrawExtent()
    )
  }
}

trait System {
  def step(world: World, dt: Double): Unit
}

class CollisionSystem extends System {
  def step(world: World, dt: Double): Unit = {
    for (bullet <- world.entitiesWith[Destroyer]) {
      for {
        hit <- world.entitiesWith[Destroyable].find { e =>
          Intersections.intersects(bullet[Extent].shape, e[Extent].shape)
        }
      } {
        world send (hit, Destroy())
        world.remove(bullet)
      }
    }
  }
}


class MovementSystem(screen: AABB) extends System {
  def wrap(p: Vec2): Vec2 =
    Vec2(
      if (p.x < screen.lower.x) p.x + screen.width else if (p.x > screen.upper.x) p.x - screen.width else p.x,
      if (p.y < screen.lower.y) p.y + screen.height else if (p.y > screen.upper.y) p.y - screen.height else p.y
    )

  def step(world: World, dt: Double): Unit = {
    for (e <- world.entitiesWith[Velocity]) {
      e[Extent].pos = wrap(e[Extent].pos + e[Velocity].vel * dt)
      e[Extent].rot += e[Velocity].angVel * dt
    }
  }
}

class InputSystem extends System {
  override def step(world: World, dt: Double): Unit = {
    for (e <- world.entitiesWith[Input]) {
      if (keysDown contains GLFW.GLFW_KEY_LEFT) {
        world send (e, ('left, dt))
      }
      if (keysDown contains GLFW.GLFW_KEY_RIGHT) {
        world send (e, ('right, dt))
      }
      if (keysDown contains GLFW.GLFW_KEY_UP) {
        world send (e, ('thrust, dt))
      }
    }
  }

  private val keysDown = mutable.Set.empty[Int]
  def onKeyDown(world: World, key: Int, scanCode: Int, mods: Int): Unit = {
    keysDown += key

    if (key == GLFW.GLFW_KEY_SPACE) {
      for (e <- world.entitiesWith[Input]) {
        world send (e, 'fire)
      }
    }
  }

  def onKeyUp(world: World, key: Int, scanCode: Int, mods: Int): Unit = {
    keysDown -= key
  }
}

class ComponentyTitleScreen(screen: AABB) {
  private val inputSystem = new InputSystem
  val systems: Seq[System] = Seq(
    new MovementSystem(screen),
    new CollisionSystem,
    inputSystem
  )

  val world = new World

  {
    (0 to 10) foreach { _ =>
      world.add(Entities.makeAsteroid(Random.withinAABB(screen), Random.between(2, 4)))
    }
    world.add(Entities.makePlayer(screen.center))
  }

  def onKeyDown(key: Int, scanCode: Int, mods: Int): Unit = {
    inputSystem.onKeyDown(world, key, scanCode, mods)
  }

  def onKeyUp(key: Int, scanCode: Int, mods: Int): Unit = {
    inputSystem.onKeyUp(world, key, scanCode, mods)
  }

  def step(dt: Double): Unit = {
    systems foreach (_.step(world, dt))
  }

  def draw(canvas: Canvas): Unit = {
    canvas.clear(Color.Black)
    for (e <- world.entitiesWith[Drawable]) {
      e[Drawable].draw(e, canvas)
    }
  }
}


class TitleScreen(screen: AABB) {

  case class Asteroid(size: Int, polygon: Polygon, var pos: Vec2, vel: Vec2, var rot: Double, angVel: Double) {
    def extent: Polygon = polygon.rotateAroundOrigin(rot).translate(pos)
  }

  private val asteroids = mutable.Buffer.empty ++ (0 to 10) map { _ =>
    makeAsteroid(size = Random.between(2, 4), Random)
  }

  private val bullets = mutable.Buffer.empty[Segment2]

  case class Player(var pos: Vec2, var vel: Vec2, var rot: Double) {
    def extent: Polygon = Circle2(Vec2(0, 0), 10).toPolygon(3).rotateAroundOrigin(player.rot).translate(player.pos)
  }

  private val player = Player(screen.center, Vec2(0, 0), 0)
  private var gameOver = false

  private val keysDown = mutable.Set.empty[Int]

  def onKeyDown(key: Int, scanCode: Int, mods: Int): Unit = {
    keysDown += key

    if (key == GLFW.GLFW_KEY_SPACE && bullets.size < 3) {
      bullets.append(Segment2(player.pos + Vec2.forAngle(player.rot) * 10.5, player.pos + Vec2.forAngle(player.rot) * 15.5))
    }
  }

  def onKeyUp(key: Int, scanCode: Int, mods: Int): Unit = {
    keysDown -= key
  }

  def wrap(p: Vec2): Vec2 =
    Vec2(
      if (p.x < screen.lower.x) p.x + screen.width else if (p.x > screen.upper.x) p.x - screen.width else p.x,
      if (p.y < screen.lower.y) p.y + screen.height else if (p.y > screen.upper.y) p.y - screen.height else p.y
    )

  def step(dt: Double): Unit = {
    if (keysDown contains GLFW.GLFW_KEY_LEFT)
      player.rot += 2.3 * dt
    if (keysDown contains GLFW.GLFW_KEY_RIGHT)
      player.rot -= 2.3 * dt
    if (keysDown contains GLFW.GLFW_KEY_UP)
      player.vel += Vec2.forAngle(player.rot) * 90 * dt
    player.pos += player.vel * dt
    asteroids.foreach { a =>
      a.rot += dt * a.angVel
      a.pos = wrap(a.pos + a.vel * dt)
    }

    bullets.indices foreach { i =>
      val b = bullets(i)
      val vel = b.b - b.a
      val pos = wrap(b.a + vel)
      bullets(i) = Segment2(pos, pos + vel)
    }

    for ((b, i) <- bullets.zipWithIndex) {
      val hit = asteroids.indexWhere(a => Intersections.intersects(b, a.extent))
      if (hit >= 0) {
        val ast = asteroids(hit)
        asteroids.remove(hit)
        if (ast.size > 1) {
          asteroids ++= (1 to ast.size) map { _ =>
            makeAsteroid(ast.size - 1, Random).copy(pos = ast.pos + Rand.withinCircle(ast.size * 20))
          }
        }
        bullets.remove(i)
      }
    }

    if (asteroids.exists(a => Intersections.intersects(player.extent, a.extent))) {
      gameOver = true
    }
  }

  def draw(canvas: Canvas): Unit = {
    canvas.clear(Color.Black)
    val astPaint = Paint.blank.setColor(Color.White).setStrokeWidth(2).setStyle(Paint.Style.Stroke)
    for (Asteroid(_, polygon, pos, _, rot, _) <- asteroids) {
      canvas.save()
      canvas.translate(pos)
      canvas.rotate(rot)
      val path = Path.empty.polygon(polygon)
      canvas.drawPath(path, astPaint)
      canvas.restore()
    }

    for (b <- bullets) {
      canvas.drawLine(b, astPaint)
    }

    if (!gameOver) {
      canvas.save()
      val polygon = player.extent
      canvas.drawPath(Path.empty.polygon(polygon), astPaint)
      canvas.drawCircle(Circle2(polygon.points.head, 3), astPaint)
      canvas.restore()
    }
  }

  def makeAsteroid(size: Int, r: Random): Asteroid =
    Asteroid(
      size,
      Asteroids.makeAsteroidShape(size, r),
      Random.withinAABB(screen),
      Random.withinCircle(80 / size),
      Random.angle,
      Random.between(-0.2, 0.2)
    )

}

object Asteroids {
  def makeAsteroidShape(size: Int, r: Random): Polygon = {
    Polygon(
      Circle2(Vec2(0, 0), size * 20 * r.between(0.9, 1.2))
        .toPolygon(r.between(15, 30))
        .points.map(p => p * r.between(0.9, 1.1))
    )
  }
  def main(args: Array[String]): Unit = {
    val window = new GLFWWindow(1024, 768, "Asteroids")

    val screen = new ComponentyTitleScreen(AABB(0, 0, 1024, 768))

    window.onKeyDown = screen.onKeyDown
    window.onKeyUp = screen.onKeyUp

    while (!window.shouldClose) {
      screen.step(1 / 60.0)
      screen.draw(window.canvas)

      window.canvas.flush()

      window.swapBuffers()
    }
  }
}
