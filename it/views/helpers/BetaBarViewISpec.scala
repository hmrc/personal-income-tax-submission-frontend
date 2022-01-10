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

package views.helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import utils.{IntegrationTest, ViewHelpers}
import views.html.templates.helpers.BetaBar

class BetaBarViewISpec extends IntegrationTest with ViewHelpers {

  lazy val betaBar: BetaBar = app.injector.instanceOf[BetaBar]

  private val aTagSelector = "a"

  "BetaBarView" when {

    "provided with an implicit appConfig" should {

      "use appConfig.feedbackUrl in the beta banner link" which {

        "contains the correct href value when the service is accessed as an individual" in {

          lazy val view = betaBar(isAgent = false)(FakeRequest(), messages, appConfig)
          implicit def document: () => Document = () => Jsoup.parse(view.body)
          implicit val isAgent: Boolean = false

          element(aTagSelector).attr("href") shouldBe appConfig.betaFeedbackUrl
        }

        "contains the correct href value the service is accessed as an agent" in {

          lazy val view = betaBar(isAgent = true)(FakeRequest(), messages, appConfig)
          implicit def document: () => Document = () => Jsoup.parse(view.body)
          implicit val isAgent: Boolean = true

          element(aTagSelector).attr("href") shouldBe appConfig.betaFeedbackUrl
        }
      }
    }
  }
}
