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

package test.controllers

import controllers.ZeroingWarningController
import models.dividends.{DividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.StockDividendsPriorDataModel
import models.savings.SavingsIncomeCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.PrivateMethodTester
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import test.utils._

class ZeroingWarningControllerISpec extends IntegrationTest
  with DividendsDatabaseHelper
  with InterestDatabaseHelper
  with SavingsDatabaseHelper
  with GiftAidDatabaseHelper
  with ViewHelpers
  with PrivateMethodTester  {

  private def url(journeyKey: String, needExplicit: Boolean = true) = {
    (if (needExplicit) appUrl else "/update-and-submit-income-tax-return/personal-income") +
      s"/$taxYearEOY/$journeyKey/change-information"
  }

  private trait CommonResults {
    def caption(journeyKey: String): String

    val p2Text: String

    val confirmText: String
    val cancelText: String
  }

  private trait SpecificResults {
    val title: String
    val p1Text: String
  }

  private object CommonResultsEn extends CommonResults {
    private val captionDividends: String = s"Dividends for 6 April $taxYearEndOfYearMinusOne to 5 April $taxYearEOY"
    private val captionInterest: String = s"Interest for 6 April $taxYearEndOfYearMinusOne to 5 April $taxYearEOY"
    private val captionGiftAid: String = s"Donations to charity for 6 April $taxYearEndOfYearMinusOne to 5 April $taxYearEOY"

    override def caption(journeyKey: String): String = {
      journeyKey match {
        case "dividends" => captionDividends
        case "interest" => captionInterest
        case "charity" => captionGiftAid
      }
    }

    override val p2Text: String = "You will still see some previous information but all the amounts will be set to £0."

    override val confirmText: String = "Confirm"
    override val cancelText: String = "Cancel"
  }

  private object CommonResultsCy extends CommonResults {
    private val captionDividends: String = s"Difidendau ar gyfer 6 Ebrill $taxYearEndOfYearMinusOne i 5 Ebrill $taxYearEOY"
    private val captionInterest: String = s"Llog ar gyfer 6 Ebrill $taxYearEndOfYearMinusOne i 5 Ebrill $taxYearEOY"
    private val captionGiftAid: String = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEndOfYearMinusOne i 5 Ebrill $taxYearEOY"
    override def caption(journeyKey: String): String = {
      journeyKey match {
        case "dividends" => captionDividends
        case "interest" => captionInterest
        case "charity" => captionGiftAid
      }
    }

    override val p2Text: String = "Byddwch yn dal i weld rhywfaint o wybodaeth flaenorol, ond bydd yr holl symiau’n cael eu gosod i £0."

    override val confirmText: String = "Cadarnhau"
    override val cancelText: String = "Canslo"
  }

  private object SpecificIndividualEn extends SpecificResults {
    override val title: String = "This will change information on your Income Tax Return"
    override val p1Text: String = "We cannot remove all the information due to technical reasons. We will change your " +
      "current information so that this section will not affect your return."
  }

  private object SpecificIndividualCy extends SpecificResults {
    override val title: String = "Bydd hyn yn newid yr wybodaeth ar eich Ffurflen Dreth Incwm"
    override val p1Text: String = "Ni allwn dynnu’r holl wybodaeth oherwydd rhesymau technegol. Byddwn yn newid eich " +
      "gwybodaeth gyfredol fel na fydd yr adran hon yn effeithio ar eich Ffurflen Dreth."
  }

  private object SpecificAgentEn extends SpecificResults {
    override val title: String = "This will change information on your client’s Income Tax Return"
    override val p1Text: String = "We cannot remove all the information due to technical reasons. We will change the " +
      "current information so that this section will not affect your client’s return."
  }

  private object SpecificAgentCy extends SpecificResults {
    override val title: String = "Bydd hyn yn newid yr wybodaeth ar Ffurflen Dreth Incwm eich cleient"
    override val p1Text: String = "Ni allwn dynnu’r holl wybodaeth oherwydd rhesymau technegol. Byddwn yn newid " +
      "gwybodaeth gyfredol eich cleient fel na fydd yr adran hon yn effeithio ar ei Ffurflen Dreth."
  }

  private object Selectors {
    val p1Selector = "#main-content > div > div > p:nth-child(2)"
    val p2Selector = "#main-content > div > div > p:nth-child(3)"

    val cancelLinkSelector = "#main-content > div > div > form > div > a"
  }

  private val userScenarios = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonResultsEn, Some(SpecificIndividualEn)),
    UserScenario(isWelsh = false, isAgent = true, CommonResultsEn, Some(SpecificAgentEn)),
    UserScenario(isWelsh = true, isAgent = false, CommonResultsCy, Some(SpecificIndividualCy)),
    UserScenario(isWelsh = true, isAgent = true, CommonResultsCy, Some(SpecificAgentCy))
  )

  val priorStockDividends: StockDividendsPriorDataModel = StockDividendsPriorDataModel(
    ukDividendsAmount = Some(50.00),
    otherUkDividendsAmount = Some(50.00),
    stockDividendsAmount = Some(50.00),
    redeemableSharesAmount = Some(50.00),
    closeCompanyLoansWrittenOffAmount = Some(50.00)
  )

  ".show" when {

    userScenarios.foreach { scenario =>

      s"the user is ${if (scenario.isAgent) "an agent" else "an individual"} and is viewing the " +
        s"page in ${if (scenario.isWelsh) "Welsh" else "English"}" should {

        "redirect the user to the overview page" when {

          "the tailoring feature switch is turned off" which {
            lazy val result: WSResponse = {
              dropDividendsDB()
              dropInterestDB()
              dropGiftAidDB()

              authoriseAgentOrIndividual(scenario.isAgent)

              urlGet(url("interest"), scenario.isWelsh, follow = false, playSessionCookie(scenario.isAgent))
            }

            "should have a status of SEE_OTHER(303)" in {
              result.status shouldBe SEE_OTHER
            }

            "should have redirected to the correct location" in {
              result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
            }

          }

        }

        "show the page" when {
          Seq("interest", "dividends", "charity").foreach { journeyKey =>
            s"the feature switch is turned on and the journey key is $journeyKey" which {
              lazy val specificResults = scenario.specificExpectedResults.get

              import scenario.commonExpectedResults._
              import specificResults._

              lazy val result = {
                dropDividendsDB()
                dropInterestDB()
                dropGiftAidDB()

                authoriseAgentOrIndividual(scenario.isAgent)

                val headers = (if(scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq()) ++ playSessionCookie(
                  agent = scenario.isAgent,
                  isEoy = true
                )

                val request = FakeRequest("GET", url(journeyKey, needExplicit = false)).withHeaders(headers: _*)

                route(appWithTailoring, request, "").get
              }

              lazy val cancelLink = {
                journeyKey match {
                  case "dividends" => s"/update-and-submit-income-tax-return/personal-income/$taxYearEOY/dividends/dividends-from-stocks-and-shares"
                  case "interest" => s"/update-and-submit-income-tax-return/personal-income/$taxYearEOY/interest/interest-from-UK"
                  case "charity" => s"/update-and-submit-income-tax-return/personal-income/$taxYearEOY/charity/charity-donations-to-charity"
                }
              }

              implicit def document: () => Document = () => Jsoup.parse(bodyOf(result))

              "has a status of OK(200)" in {
                status(result) shouldBe OK
              }

              titleCheck(title, scenario.isWelsh)
              captionCheck(caption(journeyKey))
              textOnPageCheck(p1Text, Selectors.p1Selector)
              textOnPageCheck(p2Text, Selectors.p2Selector)
              buttonCheck(confirmText)
              linkCheck(cancelText, Selectors.cancelLinkSelector, cancelLink)
            }
          }
        }

      }
    }
  }

  ".submit" should {

    "redirect to the overview page" when {

      "the feature switch is off" which {
        lazy val result = {
          dropDividendsDB()
          dropInterestDB()
          dropGiftAidDB()

          authoriseIndividual()

          urlGet(url("interest"), follow = false, headers = playSessionCookie())
        }

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct redirect location" in {
          result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
        }
      }

      "there is no CYA data" which {
        lazy val result = {
          dropInterestDB()
          emptyUserDataStub(taxYear = taxYearEOY)
          authoriseIndividual()

          val request = FakeRequest(
            "POST",
            url("interest", needExplicit = false)
          ).withHeaders(playSessionCookie(isEoy = true) ++ Seq("Csrf-Token" -> "nocheck"): _*)

          route(appWithTailoring, request, "{}").get
        }

        "has a status of SEE_OTHER(303)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has a redirect URL for the overview page" in {
          await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
        }
      }

    }

    "redirect to the interest CYA page" when {

      "the feature switch is on and session data exists" which {
        lazy val result = {
          dropInterestDB()
          insertInterestCyaData(Some(InterestCYAModel(
            gateway = Some(false)
          )), taxYearEOY)
          emptyUserDataStub(taxYear = taxYearEOY)

          authoriseIndividual()

          val request = FakeRequest(
            "POST",
            url("interest", needExplicit = false)
          ).withHeaders(playSessionCookie(isEoy = true) ++ Seq("Csrf-Token" -> "nocheck"): _*)

          route(appWithTailoring, request, "{}").get
        }

        "has a status of SEE_OTHER(303)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has a redirect location set to the interest cya page" in {
          await(result).header.headers("Location") shouldBe s"/update-and-submit-income-tax-return/personal-income/$taxYearEOY/interest/check-interest"
        }
      }

    }

    "redirect to the dividends CYA page" when {

      "the feature switch is on and session data exists" which {
        val ukDividendsAmount: BigDecimal = 1000
        val otherDividendsAmount: BigDecimal = 0

        lazy val result = {
          dropInterestDB()
          insertDividendsCyaData(
            Some(DividendsCheckYourAnswersModel(Some(true), Some(true), Some(ukDividendsAmount), Some(true), Some(otherDividendsAmount))),
            taxYearEOY,
            Some(mtditid),
            None
          )
          emptyUserDataStub(taxYear = taxYearEOY)

          authoriseIndividual()

          val request = FakeRequest(
            "POST",
            url("dividends", needExplicit = false)
          ).withHeaders(playSessionCookie(isEoy = true) ++ Seq("Csrf-Token" -> "nocheck"): _*)

          route(appWithTailoring, request, "{}").get
        }

        "has a status of SEE_OTHER(303)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has a redirect location set to the dividends cya page" in {
          await(result).header.headers("Location") shouldBe
            s"/update-and-submit-income-tax-return/personal-income/$taxYearEOY/dividends/check-income-from-dividends"
        }
      }

    }
  }

  "redirect to the stock dividends CYA page" when {

    "the feature switch is on and session data exists" which {
      lazy val result = {
        dropStockDividendsDB()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel.copy(gateway = Some(true))), taxYearEOY)
        authoriseIndividual()

        val request = FakeRequest(
          "POST",
          url("stock-dividends", needExplicit = false)
        ).withHeaders(playSessionCookie(isEoy = true) ++ Seq("Csrf-Token" -> "nocheck"): _*)

        route(appWithStockDividends, request, "{}").get
      }

      "has a status of SEE_OTHER(303)" in {
        status(result) shouldBe SEE_OTHER
      }

      "has a redirect location set to the dividends summary page" in {
        await(result).header.headers("Location") shouldBe
          s"/update-and-submit-income-tax-return/personal-income/$taxYearEOY/dividends/check-income-from-dividends"
      }
    }

  }

  "redirect to the Savings CYA page" when {

    "the feature switch is on and session data exists" which {
      lazy val result = {
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(Some(completeSavingsCYAModel.copy(gateway = Some(true))), taxYearEOY)
        authoriseIndividual()

        val request = FakeRequest(
          "POST",
          url("savings", needExplicit = false)
        ).withHeaders(playSessionCookie(isEoy = true) ++ Seq("Csrf-Token" -> "nocheck"): _*)

        route(appWithInterestSavings, request, "{}").get
      }

      "has a status of SEE_OTHER(303)" in {
        status(result) shouldBe SEE_OTHER
      }

      "has a redirect location set to the savings summary page" in {
        await(result).header.headers("Location") shouldBe
          s"/update-and-submit-income-tax-return/personal-income/$taxYearEOY/interest/check-interest-from-securities"
      }
    }

  }

  ".zeroInterestData" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]

    val privateZeroInterestData = PrivateMethod[InterestCYAModel](Symbol("zeroInterestData"))

    "zero data that exists in prior, and remove that which doesn't" in {
      val defaultAmount = 100000

      val cya = InterestCYAModel(Some(false), Some(true), Some(true), Seq(
        InterestAccountModel(Some("anId"), "This is an account", taxedAmount = Some(defaultAmount)),
        InterestAccountModel(Some("anId2"), "This is an account", untaxedAmount = Some(defaultAmount)),
        InterestAccountModel(None, "This is an account", taxedAmount = Some(defaultAmount), uniqueSessionId = Some("anId3"))
      ))

      val expectedCya = InterestCYAModel(Some(false), Some(true), Some(true), Seq(
        InterestAccountModel(Some("anId"), "This is an account", taxedAmount = Some(0)),
        InterestAccountModel(Some("anId2"), "This is an account", untaxedAmount = Some(0))
      ))

      controller invokePrivate privateZeroInterestData(cya, Seq("anId", "anId2")) shouldBe expectedCya
    }

    "set the 'do you have' fields to false if no more accounts remain for them" in {
      val defaultAmount = 100000

      val cya = InterestCYAModel(Some(false), Some(true), Some(true), Seq(
        InterestAccountModel(None, "This is an account", taxedAmount = Some(defaultAmount), uniqueSessionId = Some("anId")),
        InterestAccountModel(None, "This is an account", untaxedAmount = Some(defaultAmount), uniqueSessionId = Some("anId2")),
        InterestAccountModel(None, "This is an account", taxedAmount = Some(defaultAmount), uniqueSessionId = Some("anId3"))
      ))

      val expectedCya = InterestCYAModel(Some(false), Some(false), Some(false), Seq())

      controller invokePrivate privateZeroInterestData(cya, Seq("anId", "anId2")) shouldBe expectedCya
    }
  }

  ".zeroDividendsData" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]
    val privateZeroDividendsData = PrivateMethod[InterestCYAModel](Symbol("zeroDividendsData"))

    "return a Dividends Check Your Answers Model with an updated ukDividendsAmount value when ukDividends flag is set to true" in {
      val ukDividendsAmount: BigDecimal = 1000

      val cya = DividendsCheckYourAnswersModel(None, Some(true), Some(ukDividendsAmount), None, None)
      val expectedCya = DividendsCheckYourAnswersModel(None, Some(true), Some(0), None, None)

      controller invokePrivate privateZeroDividendsData(cya) shouldBe expectedCya
    }

    "return a Dividends Check Your Answers Model with an updated ukDividendsAmount value when ukDividends flag is set to false" in {
      val ukDividendsAmount: BigDecimal = 0

      val cya = DividendsCheckYourAnswersModel(None, Some(false), Some(ukDividendsAmount), None, None)
      val expectedCya = DividendsCheckYourAnswersModel(None, Some(false), None, None, None)

      controller invokePrivate privateZeroDividendsData(cya) shouldBe expectedCya
    }

    "return a Dividends Check Your Answers Model with an updated otherUkDividendsAmount value when otherUkDividends flag is set to true" in {
      val otherDividendsAmount: BigDecimal = 1000

      val cya = DividendsCheckYourAnswersModel(None, None, None, Some(true), Some(otherDividendsAmount))
      val expectedCya = DividendsCheckYourAnswersModel(None, None, None, Some(true), Some(0))

      controller invokePrivate privateZeroDividendsData(cya) shouldBe expectedCya
    }

    "return a Dividends Check Your Answers Model with an updated otherUkDividendsAmount value when otherUkDividends flag is set to false" in {
      val otherDividendsAmount: BigDecimal = 0

      val cya = DividendsCheckYourAnswersModel(None, None, None, Some(false), Some(otherDividendsAmount))
      val expectedCya = DividendsCheckYourAnswersModel(None, None, None, Some(false), None)

      controller invokePrivate privateZeroDividendsData(cya) shouldBe expectedCya
    }
  }

  ".zeroStockDividendsData" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]

    val privateZeroStockDividendsData = PrivateMethod[StockDividendsCheckYourAnswersModel](Symbol("zeroStockDividendsData"))

    "zero all data that exists in prior" in {
      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), completeStockDividendsCYAModel) shouldBe completeStockDividendsCYAModel
    }

    "zero Uk Dividends data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(ukDividends = None, ukDividendsAmount = None)

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        ukDividends = Some(true),
        ukDividendsAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Other Uk Dividends data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(otherUkDividends = None, otherUkDividendsAmount = None)

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        otherUkDividends = Some(true),
        otherUkDividendsAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Uk Dividends and Other Uk Dividends data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(
        ukDividends = None, ukDividendsAmount = None, otherUkDividends = None, otherUkDividendsAmount = None
      )

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        ukDividends = Some(true),
        ukDividendsAmount = Some(0),
        otherUkDividends = Some(true),
        otherUkDividendsAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Stock Dividends data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(stockDividends = None, stockDividendsAmount = None)

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        stockDividends = Some(true),
        stockDividendsAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Redeemable Shares data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(redeemableShares = None, redeemableSharesAmount = None)

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        redeemableShares = Some(true),
        redeemableSharesAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends),updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Close Company Loans Written Off data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(closeCompanyLoansWrittenOff = None, closeCompanyLoansWrittenOffAmount = None)

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        closeCompanyLoansWrittenOff = Some(true),
        closeCompanyLoansWrittenOffAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Stock Dividends, Redeemable Shares and  Close Company Loans Written Off data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(
        stockDividends = None, stockDividendsAmount = None,
        redeemableShares = None, redeemableSharesAmount = None,
        closeCompanyLoansWrittenOff = None, closeCompanyLoansWrittenOffAmount = None
      )

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        stockDividends = Some(true),
        stockDividendsAmount = Some(0),
        redeemableShares = Some(true),
        redeemableSharesAmount = Some(0),
        closeCompanyLoansWrittenOff = Some(true),
        closeCompanyLoansWrittenOffAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Uk Dividends and Close Company Loans Written Off data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(
        ukDividends = None, ukDividendsAmount = None,
        closeCompanyLoansWrittenOff = None, closeCompanyLoansWrittenOffAmount = None
      )

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        ukDividends = Some(true),
        ukDividendsAmount = Some(0),
        closeCompanyLoansWrittenOff = Some(true),
        closeCompanyLoansWrittenOffAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }

    "zero Other Uk Dividends, Stock Dividends and Redeemable Shares data when it has Prior data and no CYA data" in {
      val updatedStockDividendsCYAModel = completeStockDividendsCYAModel.copy(
        otherUkDividends = None, otherUkDividendsAmount = None,
        stockDividends = None, stockDividendsAmount = None,
        redeemableShares = None, redeemableSharesAmount = None
      )

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        otherUkDividends = Some(true),
        otherUkDividendsAmount = Some(0),
        stockDividends = Some(true),
        stockDividendsAmount = Some(0),
        redeemableShares = Some(true),
        redeemableSharesAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsData(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }
  }

  ".zeroDividendsAnswers" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]

    val privateZeroDividendsAnswers = PrivateMethod[StockDividendsCheckYourAnswersModel](Symbol("zeroDividendsAnswers"))

    "zero data that exists in prior except Uk Dividends and Other Uk Dividends amounts which has no value set" in {
      val updatedStockDividendsCYAModel = StockDividendsCheckYourAnswersModel(gateway = Some(true))

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        ukDividends = Some(true), ukDividendsAmount = Some(0), otherUkDividends = Some(true), otherUkDividendsAmount = Some(0)
      )

      controller invokePrivate privateZeroDividendsAnswers(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }
  }

  ".zeroStockDividendsAnswers" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]

    val privateZeroStockDividendsAnswers = PrivateMethod[StockDividendsCheckYourAnswersModel](Symbol("zeroStockDividendsAnswers"))

    "zero data that exists in prior except Stock Dividends, Redeemable Shares and Close Company Loans Written Off amounts which have no value set" in {
      val updatedStockDividendsCYAModel = StockDividendsCheckYourAnswersModel(
        gateway = Some(true), ukDividends = Some(true), ukDividendsAmount = Some(0),
        otherUkDividends = Some(true), otherUkDividendsAmount = Some(0)
      )

      val expectedStockDividendsCya = updatedStockDividendsCYAModel.copy(
        stockDividends = Some(true), stockDividendsAmount = Some(0),
        redeemableShares = Some(true), redeemableSharesAmount = Some(0),
        closeCompanyLoansWrittenOff = Some(true), closeCompanyLoansWrittenOffAmount = Some(0)
      )

      controller invokePrivate privateZeroStockDividendsAnswers(Some(priorStockDividends), updatedStockDividendsCYAModel) shouldBe expectedStockDividendsCya
    }
  }

  ".updateAmount" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]

    val privateUpdateAmount = PrivateMethod[Option[BigDecimal]](Symbol("updateAmount"))

    "return same Uk Dividends Amount if both Prior and CYA data" in {
      val priorAmount = Some(50.00)
      val cyaAmount = Some(50.00)
      val expectedAmount = priorAmount

      controller invokePrivate privateUpdateAmount(priorAmount, cyaAmount) shouldBe expectedAmount
    }

    "return zero amount when Prior amount exists and CYA amount is None" in {
      val priorAmount = Some(50.00)
      val cyaAmount = None
      val expectedAmount = Some(0)

      controller invokePrivate privateUpdateAmount(priorAmount, cyaAmount) shouldBe expectedAmount
    }

    "return CYA amount when amount when CYA amount exists and Prior amount is None" in {
      val priorAmount = None
      val cyaAmount = Some(50.00)
      val expectedAmount = Some(50.00)

      controller invokePrivate privateUpdateAmount(priorAmount, cyaAmount) shouldBe expectedAmount
    }

    "return Prior amount when both CYA and Prior amounts are None" in {
      val priorAmount = None
      val cyaAmount = None
      val expectedAmount = cyaAmount

      controller invokePrivate privateUpdateAmount(priorAmount, cyaAmount) shouldBe expectedAmount
    }
  }

  ".updateQuestion" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]

    val privateUpdateQuestion = PrivateMethod[Option[BigDecimal]](Symbol("updateQuestion"))

    "return Some(true) if the Prior Amount exists" in {
      val priorAmount = Some(50.00)
      val expectedAmount = Some(true)

      controller invokePrivate privateUpdateQuestion(priorAmount) shouldBe expectedAmount
    }

    "return Some(false) if the Prior Amount is not present" in {
      val priorAmount = None
      val expectedAmount = Some(false)

      controller invokePrivate privateUpdateQuestion(priorAmount) shouldBe expectedAmount
    }
  }

  ".zeroSavingsData" should {
    lazy val controller = app.injector.instanceOf[ZeroingWarningController]

    val privateZeroSavingsData = PrivateMethod[SavingsIncomeCYAModel](Symbol("zeroSavingsData"))

    "zero all the data that exists in prior" in {
      val expectedSavingsCya = completeSavingsCYAModel.copy(grossAmount = Some(0), taxTakenOffAmount = Some(0))

      controller invokePrivate privateZeroSavingsData(completeSavingsCYAModel) shouldBe expectedSavingsCya
    }

    "zero the data that exists in prior except Tax Taken Off Amount which has no value set" in {
      val updatedSavingsCYAModel =
        completeSavingsCYAModel.copy(taxTakenOff = Some(false), taxTakenOffAmount = None)

      val expectedSavingsCya =
        completeSavingsCYAModel.copy(grossAmount = Some(0), taxTakenOff = Some(false), taxTakenOffAmount = None)

      controller invokePrivate privateZeroSavingsData(updatedSavingsCYAModel) shouldBe expectedSavingsCya
    }

    "zero the data that exists in prior when Gross Amount has more than zero and Tax Taken Off Amount is zero" in {
      val updatedSavingsCYAModel =
        completeSavingsCYAModel.copy(taxTakenOff = Some(true), taxTakenOffAmount = Some(0))

      val expectedSavingsCya =
        completeSavingsCYAModel.copy(grossAmount = Some(0), taxTakenOff = Some(true), taxTakenOffAmount = Some(0))

      controller invokePrivate privateZeroSavingsData(updatedSavingsCYAModel) shouldBe expectedSavingsCya
    }

    "zero the data that exists in prior when Gross Amount is zero and Tax Taken Off Amount is more than zero" in {
      val updatedSavingsCYAModel =
        completeSavingsCYAModel.copy(grossAmount = Some(0))

      val expectedSavingsCya =
        completeSavingsCYAModel.copy(grossAmount = Some(0), taxTakenOff = Some(true), taxTakenOffAmount = Some(0))

      controller invokePrivate privateZeroSavingsData(updatedSavingsCYAModel) shouldBe expectedSavingsCya
    }

  }

}
