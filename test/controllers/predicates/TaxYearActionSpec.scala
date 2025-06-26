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

package controllers.predicates

import common.SessionValues
import config.{AppConfig, ErrorHandler}
import models.User
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import play.api.test.Helpers.stubMessagesControllerComponents
import utils.UnitTest

class TaxYearActionSpec extends UnitTest {
  val validTaxYearList: Seq[Int] = Seq(2021, 2022, 2023)
  val validTaxYear: Int = 2022
  val invalidTaxYear: Int = 3000

  implicit lazy val mockedConfig: AppConfig = mock[AppConfig]
  implicit lazy val cc: MessagesApi = mockControllerComponents.messagesApi
  implicit lazy val mockedErrorHandler: ErrorHandler = mock[ErrorHandler]

  def taxYearAction(taxYear: Int, reset: Boolean = true): TaxYearAction = new TaxYearAction(taxYear, reset)(mockedConfig, stubMessagesControllerComponents())

  "TaxYearAction.refine" should {

    "return a Right(request)" when {

      "the tax year is within validTaxYearList, and matches that in session if the feature switch is on" in {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          (() => mockedConfig.taxYearErrorFeature).expects().returning(true)

          await(taxYearAction(validTaxYear).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is equal to the session value if the feature switch is off" in {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> (validTaxYear + 1).toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          (() => mockedConfig.taxYearErrorFeature).expects().returning(false)

          await(taxYearAction(validTaxYear + 1).refine(userRequest))
        }

        result.isRight shouldBe true
      }

      "the tax year is different to the session value if the missing tax year reset is false" in {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> (validTaxYear).toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          (() => mockedConfig.taxYearErrorFeature).expects().returning(false)

          await(taxYearAction(validTaxYear + -1, reset = false).refine(userRequest))
        }

        result.isRight shouldBe true
      }

    }

    "return a Right(result) with the Valid Tax Year List In Session" when {

      "the tax year is different from that in session and the feature switch is off" which {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> (validTaxYear).toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          (() => mockedConfig.taxYearErrorFeature).expects().returning(false)
          mockedConfig.incomeTaxSubmissionOverviewUrl _ expects * returning "/overview"

          taxYearAction(validTaxYear + 1).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the overview page redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe "/overview"
        }

        "has an updated tax year session value" in {
          await(result.map(_.left.toOption.get)).session.get(SessionValues.TAX_YEAR).get shouldBe (validTaxYear + 1).toString
        }
      }

      "the tax year is outside of validTaxYearList while the feature switch is on" which {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> (validTaxYear + 4).toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          (() => mockedConfig.taxYearErrorFeature).expects().returning(true)

          taxYearAction(validTaxYear + 4).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the TaxYearError redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe controllers.routes.TaxYearErrorController.show().url
        }

      }
    }

    "return a Left(result)" when {

      "the VALID_TAX_YEARS session value is not present" which {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> s"$validTaxYear")
        )

        lazy val result = {
          mockedConfig.incomeTaxSubmissionStartUrl _ expects * returning "/start"

          taxYearAction(validTaxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the start page redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe "/start"
        }

      }

      "the tax year is outside of validTaxYearList while the feature switch is on" which {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          (() => mockedConfig.taxYearErrorFeature).expects().returning(true)

          taxYearAction(invalidTaxYear).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the TaxYearError redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe controllers.routes.TaxYearErrorController.show().url
        }
      }

      "the tax year is within the validTaxYearList but the missing tax year reset is true" which {
        lazy val userRequest = User("1234567890", None, "AA123456A", "individual", sessionId)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> validTaxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result = {
          (() => mockedConfig.taxYearErrorFeature).expects().returning(true)
          mockedConfig.incomeTaxSubmissionOverviewUrl _ expects * returning "/overview"

          taxYearAction(validTaxYear - 1).refine(userRequest)
        }

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the overview page redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe "/overview"
        }

        "has the updated TAX_YEAR session value" in {
          await(result.map(_.left.toOption.get)).session.get(SessionValues.TAX_YEAR).get shouldBe (validTaxYear - 1).toString
        }
      }
    }
  }
}
