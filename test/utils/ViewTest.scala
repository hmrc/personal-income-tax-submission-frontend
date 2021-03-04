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

import models.User
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Assertion
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents}
import play.api.test.{FakeRequest, Helpers}

trait ViewTest extends UnitTest with GuiceOneAppPerSuite {

  val testBackUrl = "/test-back-url"
  val testCall: Call = Call("POST", "/test-url")
  implicit lazy val user: User[AnyContent] = new User[AnyContent]("1234567890", None, "AA123456A")(FakeRequest())
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())
  implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()

  val serviceName = "Update and submit an Income Tax Return"
  val govUkExtension = "GOV.UK"

  def elementText(selector: String)(implicit document: Document): String = {
    document.select(selector).text()
  }

  def element(selector: String)(implicit document: Document): Element = {
    val elements = document.select(selector)

    if(elements.size() == 0) {
      fail(s"No elements exist with the selector '$selector'")
    }

    elements.first()
  }

  def elementExist(selector: String)(implicit document: Document): Boolean = {
    !document.select(selector).isEmpty
  }

  def assertTitle(title: String)(implicit document: Document): Assertion = {
    elementText("title") shouldBe title
  }

  def assertH1(text: String)(implicit document: Document): Assertion = {
    elementText("h1") shouldBe text
  }

  def assertCaption(text: String, selector: String = ".govuk-caption-l")(implicit document: Document): Assertion = {
    elementText(selector) shouldBe text
  }

  def titleCheck(title: String)(implicit document: Document): Unit = {
    s"has a title of $title" in {
      document.title() shouldBe s"$title - $serviceName - $govUkExtension"
    }
  }

  def hintTextCheck(text: String, selector: String = ".govuk-hint")(implicit document: Document): Unit = {
    s"has the hint text of '$text'" in {
      elementText(selector) shouldBe text
    }
  }

  def h1Check(header: String)(implicit document: Document): Unit = {
    s"have a page heading of '$header'" in {
      document.select(".govuk-heading-l").text() shouldBe header
    }
  }

  def textOnPageCheck(text: String, selector: String)(implicit document: Document): Unit = {
    s"have text on the screen of '$text'" in {
      document.select(selector).text() shouldBe text
    }
  }

  def buttonCheck(text: String, selector: String)(implicit document: Document): Unit = {
    s"have a $text button" which {
      s"has the text '$text'" in {
        document.select(selector).text() shouldBe text
      }
      s"has a class of govuk-button" in {
        document.select(selector).attr("class") shouldBe "govuk-button"
      }
    }
  }

  def linkCheck(text: String, selector: String, href: String)(implicit document: Document): Unit = {
    s"have a $text link" which {
      s"has the text '$text'" in {
        document.select(selector).text() shouldBe text
      }
      s"has a href to '$href'" in {
        document.select(selector).attr("href") shouldBe href
      }
    }
  }

  def inputFieldCheck(name: String, selector: String)(implicit document: Document): Unit = {
    s"has a name of '$name'" in {
      document.select(selector).attr("name") shouldBe name
    }
  }

  def errorSummaryCheck(text: String, href: String)(implicit document: Document): Unit = {
    "contains an error summary" in {
      elementExist(".govuk-error-summary")
    }
    "contains the text 'There is a problem'" in {
      document.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
    }
    s"has a $text error in the error summary" which {
      s"has the text '$text'" in {
        document.select(".govuk-error-summary__body").text() shouldBe text
      }
      s"has a href to '$href'" in {
        document.select(".govuk-error-summary__body > ul > li > a").attr("href") shouldBe href
      }
    }
  }

  def errorAboveElementCheck(text: String)(implicit document: Document): Unit = {
    s"has a $text error above the element" which {
      s"has the text '$text'" in {
        document.select(".govuk-error-message").text() shouldBe s"Error: $text"
      }
    }
  }
}
