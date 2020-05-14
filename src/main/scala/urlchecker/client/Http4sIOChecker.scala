package urlchecker.client

import java.net.UnknownHostException

import cats.effect.{ Blocker, ContextShift, IO }
import cats.implicits._
import org.http4s.client.{ JavaNetClientBuilder, _ }
import org.http4s.{ Method, Request, Uri }
import Checker.Failure
import Checker.Failure.{ ErrorHttpStatus, Timeout, UnknownHost }
import urlchecker.client.Checker.Failure
import urlchecker.client.Checker.Failure.{ Timeout, UnknownHost }

import scala.concurrent.TimeoutException

class Http4sIOChecker(client: Client[IO]) extends Checker[IO] {
  def check(uri: Uri): IO[Either[Checker.Failure, Unit]] = {
    client
      .fetch(Request[IO](Method.HEAD, uri)) { response =>
        // We could handle redirect here via redirect middleware
        Either
          .cond[Failure, Unit](response.status.isSuccess, (), ErrorHttpStatus(response.status.code))
          .pure[IO]
      }
      .recover {
        case _: TimeoutException => Timeout.asLeft
        case _: UnknownHostException => UnknownHost.asLeft
      }
  }
}


object Http4sIOChecker {
  def apply(blocker: Blocker)(implicit cs: ContextShift[IO]): Http4sIOChecker = {
    val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create
    new Http4sIOChecker(httpClient)
  }
}