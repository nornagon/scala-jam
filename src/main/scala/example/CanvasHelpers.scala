package example

import kit.{Circle2, Polygon, Segment2, Vec2}
import scanvas.{Canvas, Paint, Path}

object CanvasHelpers {
  implicit class RichCanvas(canvas: Canvas) {
    def translate(v: Vec2): Unit = canvas.translate(v.x.toFloat, v.y.toFloat)
    def drawLine(a: Vec2, b: Vec2, paint: Paint): Unit =
      canvas.drawLine(a.x.toFloat, a.y.toFloat, b.x.toFloat, b.y.toFloat, paint)
    def drawLine(s: Segment2, paint: Paint): Unit =
      canvas.drawLine(s.a.x.toFloat, s.a.y.toFloat, s.b.x.toFloat, s.b.y.toFloat, paint)
    def drawCircle(c: Circle2, paint: Paint): Unit = canvas.drawCircle(c.c.x.toFloat, c.c.y.toFloat, c.r.toFloat, paint)
    def rotate(rot: Double): Unit = canvas.rotate(rot.toFloat)
  }

  implicit class RichPath(path: Path) {
    def moveTo(v: Vec2): Path = path.moveTo(v.x.toFloat, v.y.toFloat)
    def lineTo(v: Vec2): Path = path.lineTo(v.x.toFloat, v.y.toFloat)

    def polygon(p: Polygon): Path = {
      if (p.points.isEmpty) return path
      path.moveTo(p.points.head)
      if (p.points.size <= 1) return path
      for (point <- p.points) path.lineTo(point)
      path.close()
      path
    }
  }
}
