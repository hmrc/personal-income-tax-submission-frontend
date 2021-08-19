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

package utils

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.WireMockHelper
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.HeaderNames
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}


trait ViewHelpers { self: AnyWordSpecLike with Matchers with WireMockHelper =>

  val serviceName = "Update and submit an Income Tax Return"
  val govUkExtension = "GOV.UK"
  val serviceNameWelsh = "Diweddaru a chyflwyno Ffurflen Dreth Incwm"

  val ENGLISH = "English"
  val WELSH = "Welsh"

  def errorPrefix(isWelsh: Boolean): String = if(isWelsh) "Gwall: " else "Error: "

  def welshTest(isWelsh: Boolean): String = if (isWelsh) "Welsh" else "English"
  def agentTest(isAgent: Boolean): String = if (isAgent) "Agent" else "Individual"

  def authIndividual(nino: Boolean): StubMapping = if(nino) authoriseIndividual() else authoriseIndividual(None)
  def authoriseAgentOrIndividual(isAgent: Boolean, nino: Boolean = true): StubMapping = if (isAgent) authoriseAgent() else authIndividual(nino)
  def unauthorisedAgentOrIndividual(isAgent: Boolean): StubMapping = if (isAgent) authoriseAgentUnauthorized() else authoriseIndividualUnauthorized()

  case class UserScenario[CommonExpectedResults,SpecificExpectedResults](isWelsh: Boolean,
                                                                         isAgent: Boolean,
                                                                         commonExpectedResults: CommonExpectedResults,
                                                                         specificExpectedResults: Option[SpecificExpectedResults] = None)

  //TODO UNCOMMENT WHEN DIVIDENDS & INTEREST MOVED TO USE UserScenarios
//  val userScenarios: Seq[UserScenario[_, _]]

  def element(selector: String)(implicit document: () => Document): Element = {
    val elements = document().select(selector)

    if(elements.size() == 0) {
      fail(s"No elements exist with the selector '$selector'")
    }

    elements.first()
  }

  def elementText(selector: String)(implicit document: () => Document): String = {
    document().select(selector).text()
  }

  def elementExist(selector: String)(implicit document: () => Document): Boolean = {
    !document().select(selector).isEmpty
  }

  def elementExtinct(selector: String)(implicit document: () => Document): Unit = {
    s"does not display element with selector: $selector" in {
      document().select(selector).isEmpty shouldBe true
    }
  }

  def titleCheck(title: String, isWelsh: Boolean)(implicit document: () => Document): Unit = {
    s"has a title of $title" in {
      document().title() shouldBe s"$title - ${if(isWelsh) serviceNameWelsh else serviceName} - $govUkExtension"
    }
  }

  def hintTextCheck(text: String, selector: String = ".govuk-hint")(implicit document: () => Document): Unit = {
    s"has the hint text of '$text'" in {
      elementText(selector) shouldBe text
    }
  }

  def h1Check(header: String, size: String = "l")(implicit document: () => Document): Unit = {
    s"have a page heading of '$header'" in {
      document().select(s".govuk-heading-$size").text() shouldBe header
    }
  }

  def captionCheck(caption: String, selector: String = ".govuk-caption-l")(implicit document: () => Document): Unit = {
    s"have the caption of '$caption'" in {
      document().select(selector).text() shouldBe caption
    }
  }

  def textOnPageCheck(text: String, selector: String)(implicit document: () => Document): Unit = {
    s"have text on the screen of '$text'" in {
      document().select(selector).text() shouldBe text
    }
  }

  def formGetLinkCheck(text: String, selector: String)(implicit document: () => Document): Unit = {
    s"have a form with a GET action of '$text'" in {
      document().select(selector).attr("action") shouldBe text
      document().select(selector).attr("method") shouldBe "GET"
    }
  }

  def formPostLinkCheck(text: String, selector: String)(implicit document: () => Document): Unit = {
    s"have a form with a POST action of '$text'" in {
      document().select(selector).attr("action") shouldBe text
      document().select(selector).attr("method") shouldBe "POST"
    }
  }

  def buttonCheck(text: String, selector: String = ".govuk-button", href: Option[String] = None)(implicit document: () => Document): Unit = {
    s"have a $text button" which {
      s"has the text '$text'" in {
        document().select(selector).text() shouldBe text
      }
      s"has a class of govuk-button" in {
        document().select(selector).attr("class") should include("govuk-button")
      }

      if(href.isDefined) {
        s"has a href to '${href.get}'" in {
          document().select(selector).attr("href") shouldBe href.get
        }
      }

    }
  }

  def radioButtonCheck(text: String, radioNumber: Int)(implicit document: () => Document): Unit = {
    s"have a $text radio button" which {
      s"is of type radio button" in {
        val selector = ".govuk-radios__item > input"
        document().select(selector).get(radioNumber - 1).attr("type") shouldBe "radio"
      }
      s"has the text $text" in {
        val selector = ".govuk-radios__item > label"
        document().select(selector).get(radioNumber - 1).text() shouldBe text
      }
    }
  }

  def linkCheck(text: String, selector: String, href: String)(implicit document: () => Document): Unit = {
    s"have a $text link" which {
      s"has the text '$text'" in {
        document().select(selector).text() shouldBe text
      }
      s"has a href to '$href'" in {
        document().select(selector).attr("href") shouldBe href
      }
    }
  }

  def inputFieldCheck(name: String, selector: String)(implicit document: () => Document): Unit = {
    s"has a name of '$name'" in {
      document().select(selector).attr("name") shouldBe name
    }
  }

  def inputFieldValueCheck(value: String, selector: String)(implicit document: () => Document): Unit = {
    s"'$selector' has a value of '$value'" in {
      document().select(selector).attr("value") shouldBe value
    }
  }

  def taskListCheck(itemList: Seq[(String, String, String)], isWelsh: Boolean)(implicit document: () => Document): Unit = {
    val change = if(isWelsh) "Newid" else "Change"
    val remove = if(isWelsh) "Tynnu" else "Remove"
    for(i <- 1 to itemList.length){
      s"display a task list row for entry number $i" which {
        s"displays the correct name for entry number $i" in {
          document().select(s"ul.hmrc-add-to-a-list > li:nth-child($i) > .hmrc-add-to-a-list__identifier")
            .text() shouldBe itemList(i-1)._1
        }
        s"displays the change link and has the correct hidden-change-text for entry number $i" in {
          document().select(s"ul.hmrc-add-to-a-list > li:nth-child($i) > .hmrc-add-to-a-list__change > a > span:nth-child(1)").text() shouldBe change
          document().select(s"ul.hmrc-add-to-a-list > li:nth-child($i) > .hmrc-add-to-a-list__change > a > .govuk-visually-hidden")
            .text() shouldBe itemList(i-1)._2
        }
        s"displays the remove link and has the correct hidden-remove-text for entry number $i" in {
          document().select(s"ul.hmrc-add-to-a-list > li:nth-child($i) > .hmrc-add-to-a-list__remove > a > span:nth-child(1)").text() shouldBe remove
          document().select(s"ul.hmrc-add-to-a-list > li:nth-child($i) > .hmrc-add-to-a-list__remove > a > .govuk-visually-hidden")
            .text() shouldBe itemList(i-1)._3
        }
      }
    }
  }

  def errorSummaryCheck(text: String, href: String, isWelsh: Boolean)(implicit document: () => Document): Unit = {
    "contains an error summary" in {
      elementExist(".govuk-error-summary")
    }
    "contains the text 'There is a problem'" in {
      if(isWelsh) {
        document().select(".govuk-error-summary__title").text() shouldBe "Mae problem wedi codi"
      } else {
        document().select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      }
    }
    s"has a $text error in the error summary" which {
      s"has the text '$text'" in {
        document().select(".govuk-error-summary__body").text() shouldBe text
      }
      s"has a href to '$href'" in {
        document().select(".govuk-error-summary__body > ul > li > a").attr("href") shouldBe href
      }
    }
  }

  def multipleErrorCheck(errors: List[(String, String)], isWelsh: Boolean)(implicit document: () => Document): Unit = {

    "contains an error summary" in {
      elementExist(".govuk-error-summary")
    }
    "contains the text 'There is a problem'" in {
      if(isWelsh) {
        document().select(".govuk-error-summary__title").text() shouldBe "Mae problem wedi codi"
      } else {
        document().select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      }
    }

    for(error <- errors) {
      val index = errors.indexOf(error) + 1
      val selector = s".govuk-error-summary__body > ul > li:nth-child($index) > a"

      s"has a ${error._1} error in the error summary" which {
        s"has the text '${error._1}'" in {
          document().select(selector).text() shouldBe error._1
        }
        s"has a href to '${error._2}'" in {
          document().select(selector).attr("href") shouldBe error._2
        }
      }
    }
  }

  def errorAboveElementCheck(text: String)(implicit document: () => Document): Unit = {
    s"has a $text error above the element" which {
      s"has the text '$text'" in {
        document().select(".govuk-error-message").text() shouldBe s"Error: $text"
      }
    }
  }

  def noErrorsCheck()(implicit document: () => Document): Unit = {
    "there is no error summary" in {
      elementExist(".govuk-error-summary") shouldBe false
    }
    "there is no error above the form" in {
      elementExist(".govuk-error-message") shouldBe false
    }
  }

  def welshToggleCheck(isWelsh: Boolean)(implicit document: () => Document): Unit ={
    welshToggleCheck(if(isWelsh) WELSH else ENGLISH)
  }

  def welshToggleCheck(activeLanguage: String)(implicit document: () => Document): Unit = {
    val otherLanguage = if (activeLanguage == "English") "Welsh" else "English"

    def selector = Map("English" -> 0, "Welsh" -> 1)

    def linkLanguage = Map("English" -> "English", "Welsh" -> "Cymraeg")

    def linkText = Map("English" -> "Change the language to English English",
      "Welsh" -> "Newid yr iaith ir Gymraeg Cymraeg")

    s"have the language toggle already set to $activeLanguage" which {
      s"has the text '$activeLanguage" in {
        document().select(".hmrc-language-select__list-item").get(selector(activeLanguage)).text() shouldBe linkLanguage(activeLanguage)
      }
    }
    s"has a link to change the language to $otherLanguage" which {
      s"has the text '${linkText(otherLanguage)}" in {
        document().select(".hmrc-language-select__list-item").get(selector(otherLanguage)).text() shouldBe linkText(otherLanguage)
      }
      s"has a link to change the language" in {
        document().select(".hmrc-language-select__list-item > a").attr("href") shouldBe
          s"/income-through-software/return/personal-income/language/${linkLanguage(otherLanguage).toLowerCase}"
      }
    }
  }

}
