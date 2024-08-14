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

package test.controllers.dividends

import config.STOCK_DIVIDENDS
import controllers.dividends.routes
import models.dividends.{DividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, headers, route, writeableOf_AnyContentAsFormUrlEncoded}
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class DividendsGatewayControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  lazy val tailoringWsClient: WSClient = appWithTailoring.injector.instanceOf[WSClient]

  val dividendsGatewayUrl: String = routes.DividendsGatewayController.show(taxYear).url

  val relativeUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-stocks-and-shares"
  val absoluteUrl: String = appUrl + s"/$taxYear/dividends/dividends-from-stocks-and-shares"

  object Selectors {
    val yesSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
    val noSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"

    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val hintText: String
    val continueText: String
    val yesText: String
    val noText: String
    val caption: String

    val yesRedirectUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val noRedirectUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-income-from-dividends"
    val zeroWarningUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/change-information"

  }


  trait SpecificUserTypeResults {
    val heading: String
    val errorText: String
    val errorTitle: String
  }


  object CommonExpectedResultsEN extends CommonExpectedResults {
    override val hintText = "For example, dividends from UK companies, authorised unit trusts or open ended investment companies."
    override val continueText: String = "Continue"
    override val yesText: String = "Yes"
    override val noText: String = "No"
    override val caption: String = s"Dividends for 6 April ${(taxYear - 1).toString} to 5 April ${taxYear.toString}"
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    override val hintText = "Er enghraifft, difidendau gan gwmnïau, ymddiriedolaethau unedol awdurdodedig neu gwmnïau buddsoddi penagored yn y DU."
    override val continueText: String = "Yn eich blaen"
    override val yesText: String = "Iawn"
    override val noText: String = "Na"
    override val caption: String = s"Difidendau ar gyfer 6 Ebrill ${(taxYear - 1).toString} i 5 Ebrill ${taxYear.toString}"
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Did you get dividends from shares?"
    override val errorText: String = "Select yes if you got dividends from shares"
    override val errorTitle: String = "Error: Did you get dividends from shares?"

  }

  object AgentResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Did your client get dividends from shares?"
    override val errorText: String = "Select yes if your client got dividends from shares"
    override val errorTitle: String = "Error: Did your client get dividends from shares?"

  }

  object IndividualResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A gawsoch ddifidendau o gyfranddaliadau?"
    override val errorText: String = "Dewiswch ‘Iawn’ os cawsoch ddifidendau o gyfranddaliadau"
    override val errorTitle: String = "Gwall: A gawsoch ddifidendau o gyfranddaliadau?"

  }

  object AgentResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A gafodd eich cleient ddifidendau o gyfranddaliadau?"
    override val errorText: String = "Dewiswch ‘Iawn’ os cafodd eich cleient ddifidendau o gyfranddaliadau"
    override val errorTitle: String = "Gwall: A gafodd eich cleient ddifidendau o gyfranddaliadau?"

  }

  private val userScenarios = Seq(
    UserScenario(isWelsh = false, isAgent = false, commonExpectedResults = CommonExpectedResultsEN, Some(IndividualResultsEN)),
    UserScenario(isWelsh = false, isAgent = true, commonExpectedResults = CommonExpectedResultsEN, Some(AgentResultsEN)),
    UserScenario(isWelsh = true, isAgent = false, commonExpectedResults = CommonExpectedResultsCY, Some(IndividualResultsCY)),
    UserScenario(isWelsh = true, isAgent = true, commonExpectedResults = CommonExpectedResultsCY, Some(AgentResultsCY))
  )

  userScenarios.foreach { scenario =>
    lazy val uniqueResults = scenario.specificExpectedResults.get

    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"


    def getDividendsGateway(application: Application): Future[Result] = {
      val headers = Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++ playSessionCookie(scenario.isAgent)
      lazy val request = FakeRequest("GET", dividendsGatewayUrl).withHeaders(headers: _*)

      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request, "{}").get
    }


    def postDividendsGateway(body: Seq[(String, String)], application: Application): Future[Result] = {
      val headers = Seq("Csrf-Token" -> "nocheck") ++
        Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++
        playSessionCookie(scenario.isAgent)
      lazy val request = FakeRequest("POST", dividendsGatewayUrl).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)

      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request).get
    }

    s".show when $testNameWelsh and the user is $testNameAgent" when {

      "the tailoring is turned on" should {

        "display the gateway page" which {
          implicit lazy val application: Application = appWithTailoring

          lazy val result = {
            dropDividendsDB()
            emptyUserDataStub()
            getDividendsGateway(application)
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of OK(200)" in {
            status(result) shouldBe OK
          }

          titleCheck(heading, scenario.isWelsh)
          h1Check(s"$heading $caption")
          captionCheck(caption)
          hintTextCheck(hintText)
          formPostLinkCheck(relativeUrl, Selectors.formSelector)
          textOnPageCheck(yesText, Selectors.yesSelector)
          textOnPageCheck(noText, Selectors.noSelector)
          buttonCheck(continueText)
        }

        "display the gateway page for stock dividends" which {
          implicit lazy val application: Application = appWithStockDividends

          lazy val result = {
            getSessionDataStub()
            dropStockDividendsDB()
            emptyStockDividendsUserDataStub()
            getDividendsGateway(application)
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of OK(200)" in {
            status(result) shouldBe OK
          }

          titleCheck(heading, scenario.isWelsh)
          h1Check(s"$heading $caption")
          captionCheck(caption)
          hintTextCheck(hintText)
          formPostLinkCheck(relativeUrl, Selectors.formSelector)
          textOnPageCheck(yesText, Selectors.yesSelector)
          textOnPageCheck(noText, Selectors.noSelector)
          buttonCheck(continueText)
        }

        "display the gateway page for stock dividends with pre-filled value" which {
          implicit lazy val application: Application = appWithStockDividends

          lazy val result = {
            getSessionDataStub()
            dropStockDividendsDB()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel(gateway = Some(false))))
            getDividendsGateway(application)
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of OK(200)" in {
            status(result) shouldBe OK
          }

          titleCheck(heading, scenario.isWelsh)
          h1Check(s"$heading $caption")
          captionCheck(caption)
          hintTextCheck(hintText)
          formPostLinkCheck(relativeUrl, Selectors.formSelector)
          textOnPageCheck(yesText, Selectors.yesSelector)
          textOnPageCheck(noText, Selectors.noSelector)
          buttonCheck(continueText)
        }

      }

      "the tailoring is turn off" should {

        "redirect the user to the overview page" which {
          implicit lazy val application: Application = app

          lazy val request = {
            dropDividendsDB()
            emptyUserDataStub()
            getDividendsGateway(application)
          }

          "has a status of SEE_OTHER(303)" in {
            status(request) shouldBe SEE_OTHER
          }

          "have the correct redirect location" in {
            headers(request).get("Location").value shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }

        }

      }

    }

    s".submit when $testNameWelsh and the user is $testNameAgent" when {

      "the feature switch is turned on" when {

        "the user submits a yes, when no previous data exists" should {

          "redirect the user to the ReceiveUkDividends page" which {
            implicit lazy val application: Application = appWithTailoring

            lazy val result = {
              dropDividendsDB()
              emptyUserDataStub()
              insertDividendsCyaData(Some(DividendsCheckYourAnswersModel(
                gateway = Some(true)
              )))
              postDividendsGateway(Seq("value" -> "true"), application)
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers("location") shouldBe yesRedirectUrl
            }

          }

        }

        "the user submits a yes, when previous data exists" should {

          "redirect the user to the ReceiveUkDividends page" which {
            implicit lazy val application: Application = appWithTailoring

            lazy val result = {
              getSessionDataStub()
              updateSessionDataStub()
              dropDividendsDB()
              insertDividendsCyaData(Some(DividendsCheckYourAnswersModel(
                gateway = Some(false)
              )))
              emptyUserDataStub()
              postDividendsGateway(Seq("value" -> "true"), application)
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers("Location") shouldBe yesRedirectUrl
            }

          }

          "redirect the user to the DividendsCYA page" which {
            implicit lazy val application: Application = appWithTailoring

            lazy val result = {
              dropDividendsDB()
              insertDividendsCyaData(Some(completeDividendsCYAModel))
              emptyUserDataStub()
              postDividendsGateway(Seq("value" -> "true"), application)
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers("Location") shouldBe routes.DividendsCYAController.show(taxYear).url
            }

          }

          "return a 303 status and redirect to first status page when isFinished is false" which {
            implicit lazy val application: Application = appWithStockDividends

            lazy val result = {
              dropStockDividendsDB()
              emptyStockDividendsUserDataStub()
              insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel()))
              postDividendsGateway(Seq("value" -> "true"), application)
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers
                .get(HeaderNames.LOCATION) shouldBe Some(routes.ReceiveUkDividendsController.show(taxYear).url)
            }
          }

        }

        "the user submits a no" should {

          "redirect the user to the zero warning page" which { //needs to redirect to the zero warning page when it is built
            implicit lazy val application: Application = appWithTailoring

            lazy val result = {
              dropDividendsDB()
              insertDividendsCyaData(
                Some(DividendsCheckYourAnswersModel(
                  ukDividends = Some(true),
                  ukDividendsAmount = Some(100),
                  gateway = Some(true))))
              postDividendsGateway(Seq("value" -> "false"), application)
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers("Location") shouldBe zeroWarningUrl
            }
          }

          "return a 303 status and redirect to cya page with session data" which {
            implicit lazy val application: Application = appWithStockDividends

            lazy val result = {
              dropStockDividendsDB()
              emptyStockDividendsUserDataStub()
              insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))
              postDividendsGateway(Seq("value" -> "false"), application)
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers
                .get(HeaderNames.LOCATION) shouldBe Some(controllers.routes.ZeroingWarningController.show(taxYear, STOCK_DIVIDENDS.stringify).url)
            }
          }

          "return a 303 status and redirect to cya page with no session data" which {
            implicit lazy val application: Application = appWithStockDividends

            lazy val result = {
              dropStockDividendsDB()
              emptyStockDividendsUserDataStub()
              insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel()))
              postDividendsGateway(Seq("value" -> "false"), application)
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers
                .get(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
            }
          }
        }

        "the user submits incorrect data" should {

          "display an error on the form" which {
            implicit lazy val application: Application = appWithTailoring

            lazy val result = {
              dropDividendsDB()
              emptyUserDataStub()
              postDividendsGateway(Seq("value" -> "oops"), application)
            }

            implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

            "has a status of BAD_REQUEST(400)" in {
              status(result) shouldBe BAD_REQUEST
            }

            errorSummaryCheck(errorText, "#value", scenario.isWelsh)
            titleCheck(errorTitle, scenario.isWelsh)
            h1Check(s"$heading $caption")
            captionCheck(caption)
            hintTextCheck(hintText)
            formPostLinkCheck(relativeUrl, Selectors.formSelector)
            textOnPageCheck(yesText, Selectors.yesSelector)
            textOnPageCheck(noText, Selectors.noSelector)
            buttonCheck(continueText)

          }

        }

      }

      "the feature switch is turned off" should {

        "redirect to the overview page" which {
          implicit lazy val application: Application = app

          lazy val request = {
            dropDividendsDB()
            emptyUserDataStub()
            postDividendsGateway(Seq.empty, application)
          }

          "has a status of SEE_OTHER(303)" in {
            status(request) shouldBe SEE_OTHER
          }

          "have the correct redirect location" in {
            headers(request).get("Location").value shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }
      }
    }
  }

}
