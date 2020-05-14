package urlchecker.client

import org.http4s.Uri

trait Checker[F[_]] {
  def check(uri: Uri): F[Either[Checker.Failure, Unit]]
}

object Checker {
  sealed trait Failure
  object Failure {
    final case class ErrorHttpStatus(status: Int) extends Failure
    final case object Timeout extends Failure
    final case object UnknownHost extends Failure
  }
}