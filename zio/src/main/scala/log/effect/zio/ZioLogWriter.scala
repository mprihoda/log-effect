package log
package effect
package zio

import java.util.{ logging => jul }

import log.effect.internal.{ EffectSuspension, Id, Show }
import org.{ log4s => l4s }
import _root_.zio.{ IO, RIO, Task, UIO, URIO, ZIO }

object ZioLogWriter {

  import instances._

  val log4sFromLogger: URIO[l4s.Logger, LogWriter[Task]] =
    ZIO.accessM { log4sLogger =>
      LogWriter.from[UIO].runningEffect[Task](ZIO.effectTotal(log4sLogger))
    }

  val log4sFromName: RIO[String, LogWriter[Task]] =
    ZIO.accessM { name =>
      LogWriter.of[Task](ZIO.effect(l4s.getLogger(name)))
    }

  val log4sFromClass: RIO[Class[_], LogWriter[Task]] =
    ZIO.accessM { c =>
      LogWriter.of[Task](ZIO.effect(l4s.getLogger(c)))
    }

  val julFromLogger: URIO[jul.Logger, LogWriter[Task]] =
    ZIO.accessM { julLogger =>
      LogWriter.from[UIO].runningEffect[Task](ZIO.effectTotal(julLogger))
    }

  val julGlobal: Task[LogWriter[Task]] =
    LogWriter.of[Task](IO.effect(jul.Logger.getGlobal))

  val scribeFromName: RIO[String, LogWriter[Task]] =
    ZIO.accessM { name =>
      LogWriter.of[Task](IO.effect(scribe.Logger(name)))
    }

  val scribeFromClass: RIO[Class[_], LogWriter[Task]] =
    ZIO.accessM { c =>
      import scribe._
      LogWriter.of[Task](IO.effect(c.logger))
    }

  val scribeFromLogger: RIO[scribe.Logger, LogWriter[Task]] =
    ZIO.accessM { scribeLogger =>
      LogWriter.from[UIO].runningEffect[Task](ZIO.effectTotal(scribeLogger))
    }

  val consoleLog: LogWriter[Task] =
    LogWriter.from[Id].runningEffect[Task](LogLevels.Trace)

  def consoleLogUpToLevel[LL <: LogLevel](minLevel: LL): LogWriter[Task] =
    LogWriter.from[Id].runningEffect[Task](minLevel)

  val noOpLog: LogWriter[Id] =
    LogWriter.of[Id](())

  val noOpLogF: LogWriter[Task] =
    noOpLog.liftT

  private[this] object instances {

    implicit final private[zio] val taskEffectSuspension: EffectSuspension[Task] =
      new EffectSuspension[Task] {
        def suspend[A](a: =>A): Task[A] = IO.effect(a)
      }

    implicit final private[zio] val uioEffectSuspension: EffectSuspension[UIO] =
      new EffectSuspension[UIO] {
        def suspend[A](a: =>A): UIO[A] = IO.effectTotal(a)
      }

    implicit final private[zio] def functorInstances[R, E]: internal.Functor[ZIO[R, E, *]] =
      new internal.Functor[ZIO[R, E, *]] {
        def fmap[A, B](f: A => B): ZIO[R, E, A] => ZIO[R, E, B] = _ map f
      }

    implicit final class NoOpLogT(private val `_`: LogWriter[Id]) extends AnyVal {
      def liftT: LogWriter[Task] =
        new LogWriter[Task] {
          override def write[A: Show, L <: LogLevel: Show](level: L, a: =>A): Task[Unit] = Task.unit
        }
    }
  }
}
