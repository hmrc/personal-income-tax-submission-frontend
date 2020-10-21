

package helpers

import java.net.URLEncoder

import play.api.http.SecretConfiguration
import play.api.libs.crypto.DefaultCookieSigner
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, PlainText}

object PlaySessionCookieBaker {
  private val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="
  private val cookieSigner = new DefaultCookieSigner(SecretConfiguration(cookieKey))

  private def cookieValue(sessionData: Map[String, String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      PlainText(cookieSigner.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }

  def bakeSessionCookie(additionalData: Map[String, String] = Map()): String = {
    cookieValue(additionalData)
  }
}
