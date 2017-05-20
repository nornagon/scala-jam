package example

import org.bytedeco.javacpp.liquidfun._
import scanvas._
import scanvas.gpu.GLFWWindow


object skialf {
  def main(args: Array[String]): Unit = {
    val window = new GLFWWindow(1000, 600, "Skia + LiquidFun")
    val canvas = window.canvas

    window.show()

    val world = new b2World(0f, -10f)
    val ground = world.CreateBody(new b2BodyDef())

    val bd = new b2BodyDef()
      .`type`(b2_dynamicBody)
      .allowSleep(false)
    bd.SetPosition(0, 1)
    val body = world.CreateBody(bd)
    val shape = new b2PolygonShape()
    shape.SetAsBox(0.05f, 1.0f, new b2Vec2(2f, 0f), 0.0f)
    body.CreateFixture(shape, 5f)
    shape.SetAsBox(0.05f, 1.0f, new b2Vec2(-2f, 0f), 0.0f)
    body.CreateFixture(shape, 5f)
    shape.SetAsBox(2f, 0.05f, new b2Vec2(0f, 1f), 0.0f)
    body.CreateFixture(shape, 5f)
    shape.SetAsBox(2f, 0.05f, new b2Vec2(0f, -1f), 0.0f)
    body.CreateFixture(shape, 5f)

    val jd = new b2RevoluteJointDef()
    jd.bodyA(ground)
    jd.bodyB(body)
    jd.localAnchorA().Set(0f, 1f)
    jd.localAnchorB().Set(0f, 0f)
    jd.referenceAngle(0f)
    jd.motorSpeed((0.05 * math.Pi).toFloat)
    jd.maxMotorTorque(1e7f)
    jd.enableMotor(true)
    val joint = new b2RevoluteJoint(world.CreateJoint(jd))

    val pdef = new b2ParticleSystemDef()
    val psys = world.CreateParticleSystem(pdef)
    psys.SetGravityScale(0.4f)
    psys.SetDensity(1.2f)

    psys.SetRadius(0.025f)
    psys.SetDamping(0.2f)

    val pgdef = new b2ParticleGroupDef
    //pgdef.flags(b2_staticPressureParticle)
    //pgdef.flags(b2_elasticParticle)

    val sh = new b2PolygonShape()
    sh.SetAsBox(0.9f, 0.9f, new b2Vec2(0f, 1f), 0)
    pgdef.shape(sh)
    val pg = psys.CreateParticleGroup(pgdef)

    val pPaint = Paint.blank.setColor(Color.RGBA(50, 80, 190, 180)).setAntiAlias(true)

    val dd = new CanvasDebugDraw(canvas)
    dd.setTransform(origin = (window.width / 2, window.height / 2), scale = 100)
    dd.SetFlags(b2Draw.e_shapeBit)
    world.SetDebugDraw(dd)

    var time = 0f

    while (!window.shouldClose) {
      import canvas._
      world.Step(0.016f, 4, 3)
      time += 1/60.0f
      joint.SetMotorSpeed((0.05 * math.cos(time) * math.Pi).toFloat)

      clear(Color.White)

      save()
      val pos = psys.GetPositionBuffer()
      pos.limit(psys.GetParticleCount())
      val vel = psys.GetVelocityBuffer()
      vel.limit(psys.GetParticleCount())
      for (i <- 0 until psys.GetParticleCount()) {
        pos.position(i)
        vel.position(i)
        val x = pos.x() * 100 + window.width / 2
        val y = window.height - pos.y() * 100 - window.height / 2
        pPaint.setColor(Color.RGBA(50, 80, 190, 180 - math.min(180, vel.Length()*20).toInt))
        
        drawCircle(x, y, 3 - math.min(vel.Length(), 2), pPaint)
      }
      restore()

      world.DrawDebugData()

      flush()

      window.swapBuffers()
    }
  }
}

