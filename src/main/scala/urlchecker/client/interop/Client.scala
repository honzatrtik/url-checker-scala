package urlchecker.client.interop

import java.util.concurrent.CompletionStage

import cats.implicits._
import org.http4s.Uri
import urlchecker.client.Client
import urlchecker.client.Client.{ Response, Status }

import scala.compat.java8.FutureConverters._
import scala.jdk.CollectionConverters._
import scala.util.Try

class Client {

  private final val CONCURRENCY = 10
  private val client = Client.default

  def check(urls: java.util.Collection[String]): CompletionStage[java.util.Collection[UrlStatus]] = {
    val parsedUris = urls
      .asScala
      .toList
      .map(s => s -> Try(Uri.unsafeFromString(s)).toEither)
      .toMap

    val validUris = parsedUris
      .values
      .collect { case Right(uri) => uri }
      .toList

    client
      .check(validUris, CONCURRENCY)
      .map { responses =>
        val responseMap = responses
          .map(response => response.uri.renderString -> response)
          .toMap

        parsedUris
          .keys
          .map { url =>
            responseMap
              .get(url)
              .map(responseToUrlStatus)
              .getOrElse(UrlStatus(url, availability = false, "Invalid uri".some))
          }
          .asJavaCollection
      }
      .unsafeToFuture()
      .toJava


  }

  private def responseToUrlStatus(response: Response): UrlStatus = response match {
    case Response(uri, _: Status.Success.type) =>
      UrlStatus(uri.renderString, availability = true, none)
    case Response(uri, Status.Failure(description)) =>
      UrlStatus(uri.renderString, availability = false, description.some)
  }
}

case class UrlStatus(url: String, availability: Boolean, description: Option[String])


