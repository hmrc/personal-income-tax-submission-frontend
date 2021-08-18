/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import utils.ViewTest

class MessagesSpec extends ViewTest with GuiceOneAppPerSuite {

  lazy val allLanguages: Map[String, Map[String, String]] = app.injector.instanceOf[MessagesApi].messages
  val exclusionKeys = Set(
    "global.error.badRequest400.title",
    "global.error.badRequest400.heading",
    "global.error.badRequest400.message",
    "global.error.pageNotFound404.title",
    "global.error.pageNotFound404.heading",
    "global.error.pageNotFound404.message",
    "global.error.InternalServerError500.title",
    "global.error.InternalServerError500.heading",
    "global.error.InternalServerError500.message",
    "global.error.fallbackClientError4xx.title",
    "global.error.fallbackClientError4xx.message",
    "global.error.fallbackClientError4xx.heading"
  )

  "the messages file must have welsh translations" should {
    "check all keys in the default file other than those in the exclusion list has a corresponding translation" in {
      val defaults = allLanguages("default")
      val welsh = allLanguages("cy")

      defaults.keys.foreach(
        key =>
          if (!exclusionKeys.contains(key))
          {welsh.keys should contain(key)}
      )
    }
  }

  "the messages file" should {

    "not have duplicate values" in {

      // These messages keys are from hmrc libraries, so we cannot avoid the duplicates that occur in there.
      val exclusionKeys = List(
        "betaBar.banner.message.1",
        "betaBar.banner.message.2",
        "betaBar.banner.message.3",
        "phase.banner.link",
        "phase.banner.before",
        "phase.banner.after",
        "global.error.badRequest400.message",
        "global.error.pageNotFound404.message",
        "global.error.fallbackClientError4xx.title",
        "global.error.fallbackClientError4xx.message",
        "global.error.fallbackClientError4xx.heading",
        "error.summary.title",
        "back.text"
      )

      val defaults = allLanguages("default").filter(entry => !exclusionKeys.contains(entry._1))

      def go(keysToExplore: List[(String, String)], result: List[(String, String)]): List[(String, String)] = {
        keysToExplore match {
          case Nil => result
          case h :: t =>
            val (currentMessageKey, currentMessage) = (h._1, h._2)
            val x = defaults.collect {
              case (messageKey, message) if currentMessageKey != messageKey && currentMessage == message => currentMessageKey -> messageKey
            }

            go(t, x.toList ++ result)
        }

      }

      val result = go(defaults.toList, List())

      result.foreach(println)

      result shouldBe List()
    }

  }
}
