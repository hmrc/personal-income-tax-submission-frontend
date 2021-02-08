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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n._
import utils.{UnitTest, ViewTest}
import views.html.templates.{ErrorTemplate, NotFoundTemplate}



class ErrorHandlerSpec extends UnitTest with GuiceOneAppPerSuite with ViewTest {

  val errorTemplate: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]

  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(errorTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)



  val serviceUnavailable: Int = 503
  val internalServerError: Int = 500

  ".handleError" should {

    "when given a 503" should {

      lazy val view = errorHandler.notFoundTemplate
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "display the correct page title" in {

        document.title shouldBe "Page not found - GOV.UK"
      }

    }
  }


}
