package urlchecker.client

import java.util.concurrent.Executors

import cats.Parallel
import cats.effect.{ Blocker, Concurrent, ContextShift, IO }
import cats.effect.concurrent.Semaphore
import cats.implicits._
import org.http4s.Uri
import urlchecker.client.Checker.Failure
import urlchecker.client.Checker.Failure._
import urlchecker.client.Client.Response

import scala.concurrent.ExecutionContext


class Client[F[_]: Concurrent : Parallel](uriChecker: Checker[F]) {

  import Client.Status

  def check(uris: List[Uri], concurrency: Long): F[List[Response]] = {
    for {
      semaphore <- Semaphore[F](concurrency)
      result <- uris.parTraverse(withSemaphore(semaphore, uriChecker.check))
    } yield uris
      .zip(result.map(_.fold[Status](f => Status.Failure(makeFailureDescription(f)), _ => Status.Success)))
      .map((Response.apply _).tupled)
  }

  /** Limit number of parallel checks */
  private def withSemaphore[A, B](semaphore: Semaphore[F], f: A => F[B]): A => F[B] = { a =>
    semaphore.acquire *> f(a) <* semaphore.release
  }

  private def makeFailureDescription(failure: Failure): String = failure match {
    case ErrorHttpStatus(status) => s"Error status ${status}"
    case Timeout => "Timeout"
    case UnknownHost => "Unknown host"
  }
}

object Client {

  case class Response(uri: Uri, status: Status)

  sealed trait Status
  object Status {
    case object Success extends Status
    case class Failure(description: String) extends Status
  }

  def default: Client[IO] = {
    val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
    implicit val cs: ContextShift[IO] = IO.contextShift(ec)
    new Client(
      Http4sIOChecker(Blocker.liftExecutionContext(ec))
    )
  }
}