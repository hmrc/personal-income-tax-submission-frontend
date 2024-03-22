/*
 * Copyright 2023 HM Revenue & Customs
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

package test.views.helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import test.utils.{IntegrationTest, ViewHelpers}
import views.html.templates.helpers.Heading

class HeadingISpec extends IntegrationTest with ViewHelpers {

  lazy val heading: Heading = app.injector.instanceOf[Heading]

  "Heading template" should {

    "show the caption before the heading" in {
      lazy val view = heading(heading = "heading", caption = Some("caption"))(messages)

      implicit def document: () => Document = () => Jsoup.parse(view.body)

      val headingAndCaption = elementText(s"h1.govuk-heading-l").trim

      headingAndCaption.startsWith("caption") shouldBe true
      headingAndCaption.endsWith("heading") shouldBe true
    }

    "show only the heading when no caption is provided" in {
      lazy val view = heading(heading = "heading", caption = None)(messages)

      implicit def document: () => Document = () => Jsoup.parse(view.body)

      val headingAndCaption = elementText(s"h1.govuk-heading-l").trim

      headingAndCaption shouldBe "heading"
    }

    "add the extra classes in the h1" in {
      lazy val view = heading(heading = "heading", caption = Some("caption"), extraClasses = "extra-class")(messages)

      implicit def document: () => Document = () => Jsoup.parse(view.body)

      val headingAndCaption = document().select(s"h1.govuk-heading-l")

      headingAndCaption.hasClass("extra-class") shouldBe true
    }
  }
}
