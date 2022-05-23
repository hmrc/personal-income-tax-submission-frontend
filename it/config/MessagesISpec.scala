/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.i18n.MessagesApi
import utils.{IntegrationTest, ViewHelpers}

class MessagesISpec extends IntegrationTest with ViewHelpers {

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
    "global.error.fallbackClientError4xx.heading",
    "this.section.is",
    "language.day.plural",
    "language.day.singular",
    "back.text",
    "common.error.summary.title",
    "phase.banner.before",
    "betaBar.banner.message.1",
    "betaBar.banner.message.2",
    "betaBar.banner.message.3",
    "radios.yesnoitems.yes",
    "radios.yesnoitems.no"
  )
  val welshDuplicated = Set(
    "charity.overseas-gift-aid-summary.title.single.individual",
    "charity.overseas-gift-aid-summary.title.single.agent",
    "charity.shares-and-land-summary.title.single.agent",
    "error.agent.title",
    "charity.amount-via-gift-aid.individual.p",
    "charity.shares-and-land-summary.title.multiple.individual",
    "charity.overseas-gift-aid-summary.title.multiple.agent",
    "charity.common.gift-aid.doNotInclude.agent",
    "charity.amount-via-gift-aid.agent.p",
    "charity.overseas-gift-aid-summary.title.multiple.individual",
    "charity.shares-and-land-summary.title.single.individual",
    "charity.common.gift-aid.doNotInclude.individual",
    "charity.shares-and-land-summary.title.multiple.agent",
    "error.summary.title"
  )


  val defaults = allLanguages("default")
  val welsh = allLanguages("cy")
  
  "the messages file must have welsh translations" should {
    "check all keys in the default file other than those in the exclusion list has a corresponding translation" in {
      val filteredKeys = defaults.keys.filterNot(welsh.contains)
      val filteredWelshKeys = welsh.keys.filterNot(defaults.contains)
      
      (filteredKeys.isEmpty, filteredWelshKeys.isEmpty) match {
        case (true, true) => succeed
        case (filteredEmpty, welshEmpty) =>
          val filteredMessage = if(!filteredEmpty) Some("The Welsh language file is missing the following keys that are present in the English file: " + filteredKeys.mkString(", ")) else None
          val filteredWelshMessage = if(!welshEmpty) Some("The English language file is missing the following keys that are present in the Welsh file: " + filteredWelshKeys.mkString(", ")) else None
          val errorMessage = Seq(filteredMessage, filteredWelshMessage).flatten.mkString("\n")
          
          fail(errorMessage)
      }
    }
  }

  "the english messages file" should {

    "have no duplicate messages(values)" in {
      val messages: List[(String, String)] = defaults.filter(entry => !exclusionKeys.contains(entry._1)).toList

      val result = checkMessagesAreUnique(messages, messages, Set())

      result shouldBe Set()

    }
  }

  "the welsh messages file" should {
    "have no duplicate messages(values)" in {

      val exclusionKeysWithWelsh = exclusionKeys ++ welshDuplicated

      val messages: List[(String, String)] = welsh.filter(entry => !exclusionKeysWithWelsh.contains(entry._1)).toList

      val result = checkMessagesAreUnique(messages, messages, Set())

      result shouldBe Set()
    }
  }
}
