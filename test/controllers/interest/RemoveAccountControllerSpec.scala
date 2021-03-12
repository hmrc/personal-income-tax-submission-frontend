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

package controllers.interest

import common.{InterestTaxTypes, SessionValues}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.SEE_OTHER
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UnitTestWithApp
import views.html.interest.RemoveAccountView

import scala.concurrent.Future

class RemoveAccountControllerSpec extends UnitTestWithApp{

  implicit def wrapOption[T](input: T): Option[T] = Some(input)

  lazy val controller = new RemoveAccountController(
    mockMessagesControllerComponents,
    app.injector.instanceOf[RemoveAccountView],
    authorisedAction
  )(mockAppConfig)

  val taxYear = 2022
  val untaxedId1 = "UntaxedId1"
  val untaxedId2 = "UntaxedId2"
  val taxedId1 = "TaxedId1"
  val taxedId2 = "TaxedId2"

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  lazy val priorDataModel = Json.arr(
    Json.obj(
      "accountName" -> "Untaxed Account",
      "incomeSourceId" -> "UntaxedId1",
      "untaxedUkInterest" -> 100.01
    ),
    Json.obj(
      "accountName" -> "Taxed Account",
      "incomeSourceId" -> "TaxedId1",
      "taxedUkInterest" -> 9001.01
    )
  )

  lazy val untaxedInterestCyaModel: InterestCYAModel = InterestCYAModel(
    Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId1"), "Untaxed Account", 100.01))),
    Some(false), None
  )

  lazy val taxedInterestCyaModel: InterestCYAModel = InterestCYAModel(
    Some(false), None,
    Some(true), Some(Seq(InterestAccountModel(Some("TaxedId1"), "Taxed Account", 9001.01)))
  )

  lazy val missingTaxedInterestCyaModelData: InterestCYAModel = InterestCYAModel(
    Some(false), None,
    Some(true), Some(Seq(InterestAccountModel(None, "Taxed Account", 900.00)))
  )

  ".show" should {

    "with a tax type of untaxed" should {

      "return a result" which {

        s"has an OK($OK) status" in new TestWithAuth {
          val result: Future[Result] = controller.show(taxYear, UNTAXED, untaxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString))

          status(result) shouldBe OK
        }
      }
    }

    "with a tax type of taxed" should {

      "return a result" which {

        s"has an OK($OK) status" in new TestWithAuth {
          val result: Future[Result] = controller.show(taxYear, TAXED, taxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString))

          status(result) shouldBe OK
        }
      }
    }

    "with no cya data in session" should {

      "redirect to the overview page" which {
        lazy val result: Future[Result] = controller.show(taxYear, UNTAXED, untaxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes))

        s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }
      }
    }

    "if the account data is the wrong type" should {

      "redirect to the untaxed interest page when missing untaxed data" which {
        lazy val result: Future[Result] = controller.show(taxYear, UNTAXED, untaxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
          .withSession(SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString))

        s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
        }
      }


      "redirect to the taxed interest page when missing taxed data" which {
        lazy val result: Future[Result] = controller.show(taxYear, TAXED, taxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
          .withSession(SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString))

        s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
        }
      }
    }

    "if there is  prior submission data" should {

        "redirect to the taxed interest controller page" when {

          "we try to remove a taxed interest account" which {
            lazy val result: Future[Result] = controller.show(taxYear, TAXED, taxedId1)(fakeRequest
              .withSession(SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString,
                SessionValues.INTEREST_PRIOR_SUB -> priorDataModel.toString))

            "has a status of SEE_OTHER" in new TestWithAuth {
              status(result) shouldBe SEE_OTHER
            }
            "has the correct redirect URL" in {
              redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
            }
          }

          "we try to remove an untaxed interest account" which {
            lazy val result: Future[Result] = controller.show(taxYear, UNTAXED, untaxedId1)(fakeRequest
              .withSession(SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString,
                SessionValues.INTEREST_PRIOR_SUB -> priorDataModel.toString))

            "has a status of SEE_OTHER" in new TestWithAuth {
              status(result) shouldBe SEE_OTHER
            }
            "has the correct redirect URL" in {
              redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
            }
          }
        }

      "Not redirect" when {

        "The prior submission data is empty" which {

          lazy val result: Future[Result] = controller.show(taxYear, TAXED, taxedId1)(fakeRequest
            .withSession(SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString,
              SessionValues.INTEREST_PRIOR_SUB -> Json.arr().toString))

          "has a status of OK" in new TestWithAuth {
            status(result) shouldBe OK
          }
        }
      }
    }

    "if the account is missing but the model exists" should {

      "redirect to the taxed interest controller page" when {

        "there is missing taxed account data within the model" which {
          lazy val result: Future[Result] = controller.show(taxYear, TAXED, taxedId1)(fakeRequest
            .withSession(SessionValues.INTEREST_CYA -> missingTaxedInterestCyaModelData.asJsonString))

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }
        }
      }
    }

    "Redirect to the tax year error " when {

      "an invalid tax year has been added to the url" in new TestWithAuth() {

        val mockAppConfFeatureSwitch: AppConfig = new AppConfig(mock[ServicesConfig]){
          override lazy val defaultTaxYear: Int = 2022
          override lazy val taxYearErrorFeature = true
        }

        val authorisedActionFeatureSwitch = new AuthorisedAction(mockAppConfFeatureSwitch,
          agentAuthErrorPageView)(mockAuthService, stubMessagesControllerComponents())

        lazy val featureSwitchController = new RemoveAccountController(
          mockMessagesControllerComponents,
          app.injector.instanceOf[RemoveAccountView],
          authorisedActionFeatureSwitch
        )(mockAppConfFeatureSwitch)

        val invalidTaxYear = 2023
        lazy val result: Future[Result] = featureSwitchController.show(invalidTaxYear, TAXED, untaxedId1)(fakeRequest)

        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show().url

      }
    }

  }

  ".submit" should {

    "with a tax type of untaxed" should {

      def getCyaModel(result: Future[Result]): InterestCYAModel = {
        Json.parse(await(result).session.get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
      }

      "redirect to the untaxed interest accounts page" when {

        "yes is selected with 2 untaxed accounts" which {

          lazy val result = controller.submit(taxYear, UNTAXED, untaxedId2)(fakeRequest
            .withFormUrlEncodedBody(
              YesNoForm.yesNo -> YesNoForm.yes
            )
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                true,
                Seq(
                  InterestAccountModel("UntaxedId1", "Untaxed Account 1", 100.01, None),
                  InterestAccountModel("UntaxedId2", "Untaxed Account 2", 200.99, None)
                ),
                false,
                None
              ).asJsonString
            ))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
          }

          "has updated the CYA model" in {
            getCyaModel(result).untaxedUkAccounts shouldBe Some(Seq(InterestAccountModel("UntaxedId1", "Untaxed Account 1", 100.01, None)))
          }
        }

        "no is selected" which {

          lazy val result = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest
            .withFormUrlEncodedBody(
              YesNoForm.yesNo -> YesNoForm.no
            )
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                true,
                Seq(
                  InterestAccountModel("UntaxedId1", "Untaxed Account 1", 100.01, None)
                ),
                false,
                None
              ).asJsonString
            ))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
          }

          "has not removed the untaxed account in the CYA model" in {
            getCyaModel(result).untaxedUkAccounts shouldBe Some(Seq(InterestAccountModel("UntaxedId1", "Untaxed Account 1", 100.01, None)))
          }
        }
      }

      "redirect to the taxed interest page" when {

        "yes is selected with 1 untaxed account" which {

          lazy val result = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest
            .withFormUrlEncodedBody(
              YesNoForm.yesNo -> YesNoForm.yes
            )
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                true,
                Seq(
                  InterestAccountModel("UntaxedId1", "Untaxed Account 1", 100.01, None)
                ),
                false,
                None
              ).asJsonString
            ))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }

          "has updated the CYA model" in {
            getCyaModel(result).untaxedUkAccounts shouldBe Some(Seq())
          }
        }
      }

      "redirect to the overview page" when {

        "there is no CYA data" which {

          lazy val result = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          ))

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }
      }

      "redirect to the untaxed interest page" when {

        "session is missing untaxed data" which {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                true,
                Seq(),
                true,
                Seq(
                  InterestAccountModel("TaxedId1", "Taxed Account 1", 100.01, None)
                )
              ).asJsonString
            ))

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
          }
        }
      }

      "redirect to the cya page" when {

        "the final untaxed account is removed when prior tax accounts exist" which {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                true,
                Seq(
                  InterestAccountModel(untaxedId1, "Some Account", 100.00)
                ),
                true,
                Seq()
              ).asJsonString,
              SessionValues.INTEREST_PRIOR_SUB -> Json.stringify(Json.arr(
                Json.obj(
                  "accountName" -> "I'mma Taxed Account",
                  "incomeSourceId" -> "asdfghjkl",
                  "taxedUkInterest" -> 200.00
                )
              ))
            )
          )

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.InterestCYAController.show(taxYear).url
          }
        }
      }

      "return a bad request" when {

        "there is an issue with the form submission" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest
            .withSession(SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString))

          status(result) shouldBe BAD_REQUEST
        }
      }

      "redirect to the overview page" when {

        "there is no cya data in session with form errors" which {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest)

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }
      }

      "redirect to the untaxed interest controller page" when {
        "there is missing account data in session with form errors" which {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId1)(fakeRequest
            .withSession(SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString))

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
          }
        }
      }

    }

    "with a tax type of taxed" should {

      def getCyaModel(result: Future[Result]): InterestCYAModel = {
        Json.parse(await(result).session.get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
      }

      "redirect to the taxed interest accounts page" when {

        "yes is selected with 2 taxed accounts" which {

          lazy val result = controller.submit(taxYear, TAXED, taxedId2)(fakeRequest
            .withFormUrlEncodedBody(
              YesNoForm.yesNo -> YesNoForm.yes
            )
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                false,
                None,
                true,
                Seq(
                  InterestAccountModel("TaxedId1", "Taxed Account 1", 250.99, None),
                  InterestAccountModel("TaxedId2", "Taxed Account 2", 500.01, None)
                )
              ).asJsonString
            ))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
          }

          "has updated the CYA model" in {
            getCyaModel(result).taxedUkAccounts shouldBe Some(Seq(InterestAccountModel("TaxedId1", "Taxed Account 1", 250.99, None)))
          }
        }

        "no is selected" which {

          lazy val result = controller.submit(taxYear, TAXED, taxedId1)(fakeRequest
            .withFormUrlEncodedBody(
              YesNoForm.yesNo -> YesNoForm.no
            )
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                false,
                None,
                true,
                Seq(
                  InterestAccountModel("TaxedId1", "Taxed Account 1", 250.99, None)
                )
              ).asJsonString
            ))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
          }

          "has not removed the taxed account in the CYA model" in {
            getCyaModel(result).taxedUkAccounts shouldBe Some(Seq(InterestAccountModel("TaxedId1", "Taxed Account 1", 250.99, None)))
          }
        }
      }

      "redirect to the taxed interest page" when {

        "yes is selected with 1 untaxed account" which {

          lazy val result = controller.submit(taxYear, TAXED, taxedId1)(fakeRequest
            .withFormUrlEncodedBody(
              YesNoForm.yesNo -> YesNoForm.yes
            )
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                false,
                None,
                true,
                Seq(
                  InterestAccountModel("TaxedId1", "Taxed Account 1", 250.99, None)
                )
              ).asJsonString
            ))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.InterestCYAController.show(taxYear).url
          }

          "has updated the CYA model" in {
            getCyaModel(result).taxedUkAccounts shouldBe Some(Seq())
          }
        }
      }

      "redirect to the overview page" when {

        "there is no CYA data" which {

          lazy val result = controller.submit(taxYear, TAXED, taxedId1)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          ))

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }
      }

      "redirect to the taxed interest page" when {

        "session is missing taxed data" which {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId1)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(
              SessionValues.INTEREST_CYA -> InterestCYAModel(
                true,
                Seq(
                  InterestAccountModel("UntaxedId1", "Untaxed Account 1", 100.01, None)
                ),
                true,
                Seq()
              ).asJsonString
            ))

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }
        }
      }

      "return a bad request" when {

        "there is an issue with the form submission" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId1)(fakeRequest
            .withSession(SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString))

          status(result) shouldBe BAD_REQUEST
        }
      }

      "redirect to the overview page" when {

        "there is no cya data in session with form errors" which {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId1)(fakeRequest)

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }
      }

      "redirect to the taxed interest controller page" when {

        "there is missing account data in session with form errors" which {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId1)(fakeRequest
            .withSession(SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString))

          s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }
        }
      }

    }

  }

  ".getTaxAccounts" should {

    val taxedAccount = Seq(InterestAccountModel(Some("taxedId"), "Taxed Account", 9023.11))
    val untaxedAccount = Seq(InterestAccountModel(Some("untaxedId"), "Untaxed Account", 0.03))

    "return taxed accounts" when {

      "taxType is taxed" in {
        val cyaData = InterestCYAModel(Some(true), Some(untaxedAccount), Some(true), Some(taxedAccount))

        controller.getTaxAccounts(TAXED, cyaData) shouldBe Some(taxedAccount)
      }
    }

    "return untaxed accounts" when {

      "taxType is untaxed" in {
        val cyaData = InterestCYAModel(Some(true), Some(untaxedAccount), Some(true), Some(taxedAccount))

        controller.getTaxAccounts(UNTAXED, cyaData) shouldBe Some(untaxedAccount)
      }
    }

  }

  ".missingAccountRedirect" should {

    "redirect to the [taxed redirect] page" when {

      "taxType is taxed" which {
        lazy val result = Future.successful(controller.missingAccountsRedirect(TAXED, taxYear))

        s"has a status of SEE_OTHER($SEE_OTHER)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
        }
      }

    }

    "redirect to the [untaxed redirect] page" when {

      "taxType is untaxed" which {
        lazy val result = Future.successful(controller.missingAccountsRedirect(UNTAXED, taxYear))

        s"has a status of SEE_OTHER($SEE_OTHER)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
        }
      }
    }

  }

    ".isLastAccount" should {

      "have taxed typed as TAXED" when {
        "is true" in {
          val result = controller.isLastAccount(taxType = TAXED, priorSubmission = None,
            taxAccounts = Seq(InterestAccountModel(Some("taxedId"), "Taxed Account", 9023.11)))

          result shouldBe true
        }
        "is false with prior submission" in {
          val result = controller.isLastAccount(taxType = TAXED, priorSubmission = Some(
            InterestPriorSubmission(hasUntaxed = false, hasTaxed = true, Some(Seq(InterestAccountModel(
                Some("qwerty"),"TSB Account", 500.00, priorType = Some(InterestTaxTypes.TAXED)))))),
            taxAccounts = Seq(InterestAccountModel(Some("taxedId"), "Taxed Account", 9023.11)))

          result shouldBe false
        }
        "is false with no prior submission" in {
          val result = controller.isLastAccount(taxType = TAXED, priorSubmission = None,
            taxAccounts = Seq(InterestAccountModel(Some("taxedId"), "Taxed Account", 9023.11),
              InterestAccountModel(Some("taxedId2"), "Taxed Account 2", 2000.20)))

          result shouldBe false
        }
      }

      "have taxed typed as UNTAXED" when {
        "is true" in {
          val result = controller.isLastAccount(taxType = UNTAXED, priorSubmission = None,
            taxAccounts = Seq(InterestAccountModel(Some("untaxedId"), "Untaxed Account", 9023.11)))

          result shouldBe true
        }
        "is false with prior submission" in {
          val result = controller.isLastAccount(taxType = UNTAXED, priorSubmission = Some(
            InterestPriorSubmission(hasUntaxed = true, hasTaxed = false, Some(Seq(InterestAccountModel(
              Some("qwerty"),"TSB Account", 500.00, priorType = Some(InterestTaxTypes.UNTAXED)))))),
            taxAccounts = Seq(InterestAccountModel(Some("untaxedId"), "Untaxed Account", 9023.11)))

          result shouldBe false
        }
        "is false with no prior submission" in {
          val result = controller.isLastAccount(taxType = UNTAXED, priorSubmission = None,
            taxAccounts = Seq(InterestAccountModel(Some("untaxedId"), "Untaxed Account", 9023.11),
              InterestAccountModel(Some("untaxedId2"), "Untaxed Account 2", 2000.20)))

          result shouldBe false
        }
      }
    }

}
