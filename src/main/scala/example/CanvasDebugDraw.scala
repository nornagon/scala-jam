package example

import org.bytedeco.javacpp.Skia._
import org.bytedeco.javacpp.liquidfun._
import scanvas._

/** b2Draw implementation backed by a Skia canvas */
class CanvasDebugDraw(canvas: Canvas) extends b2Draw(1) {
  private val paint = Paint.blank.setAntiAlias(true).setColor(Color.Black).setStyle(Paint.Style.Stroke)
  private val transform: sk_matrix_t = new sk_matrix_t()
  sk_matrix_set_identity(transform)

  /** Draw the world with the origin at px coordinates `origin`, scaled s.t. 1 box2d unit = `scale` px */
  def setTransform(origin: (Float, Float), scale: Float): Unit = {
    sk_matrix_set_identity(transform)
    sk_matrix_post_translate(transform, origin._1, origin._2)
    sk_matrix_pre_scale(transform, scale, -scale)
  }

  private def transformed(f: => Unit): Unit = {
    canvas.save()
    canvas.concat(transform)
    f
    canvas.restore()
  }

  override def DrawPolygon(verts: b2Vec2, n: Int, color: b2Color): Unit = {
    val path = Path.empty
    path.moveTo(verts.x(), verts.y())
    for (i <- 1 until n) {
      verts.position(i)
      path.lineTo(verts.x(), verts.y())
    }
    path.close()
    paint.setColor(Color.RGB(color.r(), color.g(), color.b()))
    paint.setStyle(Paint.Style.Stroke)
    transformed {
      canvas.drawPath(path, paint)
    }
  }

  override def DrawSolidPolygon(verts: b2Vec2, n: Int, color: b2Color): Unit = {
    val path = Path.empty
    path.moveTo(verts.x(), verts.y())
    for (i <- 1 until n) {
      verts.position(i)
      path.lineTo(verts.x(), verts.y())
    }
    path.close()
    paint.setColor(Color.RGB(color.r(), color.g(), color.b()))
    paint.setStyle(Paint.Style.Fill)
    transformed {
      canvas.drawPath(path, paint)
    }
  }

  override def DrawCircle(center: b2Vec2, radius: Float, color: b2Color): Unit = {
    paint.setColor(Color.RGB(color.r(), color.g(), color.b()))
    paint.setStyle(Paint.Style.Stroke)
    transformed {
      canvas.drawCircle(center.x(), center.y(), radius, paint)
    }
  }

  override def DrawSolidCircle(center: b2Vec2, radius: Float, axis: b2Vec2, color: b2Color): Unit = {
    paint.setColor(Color.RGB(color.r(), color.g(), color.b()))
    paint.setStyle(Paint.Style.Stroke)
    transformed {
      canvas.drawCircle(center.x(), center.y(), radius, paint)
      canvas.drawLine(center.x(), center.y(), center.x() + axis.x() * radius, center.y() + axis.y() * radius, paint)
    }
  }

  override def DrawParticles(centers: b2Vec2, radius: Float, colors: b2ParticleColor, count: Int): Unit = {
    if (colors == null) {
      paint.setColor(Color.Black)
    }
    transformed {
      for (i <- 0 until count) {
        centers.position(i)
        if (colors != null) {
          colors.position(i)
          paint.setColor(Color.RGB(colors.r(), colors.g(), colors.b()))
        }
        canvas.drawPoint(centers.x(), centers.y(), paint)
      }
    }
  }

  override def DrawSegment(p1: b2Vec2, p2: b2Vec2, color: b2Color): Unit = {
    paint.setColor(Color.RGB(color.r(), color.g(), color.b()))
    transformed {
      canvas.drawLine(p1.x(), p1.y(), p2.x(), p2.y(), paint)
    }
  }

  override def DrawTransform(xf: b2Transform): Unit = {

  }
}
