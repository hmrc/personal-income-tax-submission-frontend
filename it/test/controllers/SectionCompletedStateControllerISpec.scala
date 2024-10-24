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

package controllers

import models.mongo.JourneyStatus.{Completed, InProgress}
import models.mongo.{JourneyAnswers, JourneyStatus}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsFormUrlEncoded}
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import java.time.Instant

class SectionCompletedStateControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  val sectionCompletedUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/donations-using-gift-aid/section-completed"
  val invalidSectionCompletedUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/invalid-journey/section-completed"

  val absoluteUrl: String = appUrl + sectionCompletedUrl
  val journeyName = "donations-using-gift-aid"

  def completedSectionUrl(journey: String, taxYear: Int) = s"/income-tax-gift-aid/income-tax/journey-answers/$journey/$taxYear"

  object Selectors {
    val yesSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
    val noSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"

    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedRadioHeading: String
    val expectedErrorText: String
    val yesText: String
    val noText: String
    val expectedButtonText: String
    val expectedHelpLinkText: String
    val expectedHint: String
  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    override val expectedTitle: String = "Have you finished this section?"
    override val expectedErrorTitle: String = "Error: Have you finished this section?"
    override val expectedRadioHeading: String = "Have you finished this section?"
    override val yesText: String = "Yes"
    override val noText: String = "No"
    override val expectedErrorText: String = "Select if you’ve completed this section"
    override val expectedButtonText: String = "Continue"
    override val expectedHelpLinkText: String = "Get help with this page"
    override val expectedHint: String = "You’ll still be able to go back and review the information that you’ve given us."
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    override val expectedTitle: String = "A ydych wedi gorffen yr adran hon?"
    override val expectedErrorTitle: String = "Gwall: A ydych wedi gorffen yr adran hon?"
    override val expectedRadioHeading: String = "A ydych wedi gorffen yr adran hon?"
    override val yesText: String = "Iawn"
    override val noText: String = "Na"
    override val expectedButtonText: String = "Yn eich blaen"
    override val expectedHelpLinkText: String = "Help gyda’r dudalen hon"
    override val expectedErrorText: String = "Dewiswch a ydych wedi llenwi’r adran hon"
    override val expectedHint: String = "Byddwch yn dal i allu mynd yn ôl ac adolygu’r wybodaeth rydych wedi’i rhoi i ni."
  }


  private val userScenarios = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedResultsEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedResultsCY)
  )

  userScenarios.foreach { scenario =>
    import scenario.commonExpectedResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"

    s".show when $testNameWelsh" when {

      s"display the gateway page in $testNameWelsh" which {

        lazy val headers = playSessionCookie() ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", sectionCompletedUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseIndividual()
          route(appWithTailoring, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        welshToggleCheck(scenario.isWelsh)
        h1Check(s"${expectedTitle}")
        hintTextCheck(expectedHint)
        formPostLinkCheck(sectionCompletedUrl, Selectors.formSelector)
        textOnPageCheck(yesText, Selectors.yesSelector)
        textOnPageCheck(noText, Selectors.noSelector)
        buttonCheck(expectedButtonText)
      }

      s"the journey status is Completed for $testNameWelsh" should {

        lazy val headers = playSessionCookie() ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", sectionCompletedUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseIndividual()
          val status: JourneyStatus = Completed
          val model = JourneyAnswers(mtditid, taxYear, journeyName, Json.obj({
            "status" -> status
          }), Instant.now)

          stubGet(completedSectionUrl(journeyName, taxYear), OK, Json.toJson(model).toString())

          route(appWithTailoring, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

      }

      s"the journey status is InProgress for $testNameWelsh" should {

        lazy val headers = playSessionCookie() ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", sectionCompletedUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseIndividual()
          val status: JourneyStatus = InProgress
          val model = JourneyAnswers(mtditid, taxYear, journeyName, Json.obj({
            "status" -> status
          }), Instant.now)

          stubGet(completedSectionUrl(journeyName, taxYear), OK, Json.toJson(model).toString)

          route(appWithTailoring, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }
      }

      s"there is invalid status data for the journey status in $testNameWelsh" should {

        lazy val headers = playSessionCookie() ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", sectionCompletedUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseIndividual()
          val status: JourneyStatus = InProgress
          val model = JourneyAnswers(mtditid, taxYear, journeyName, Json.obj({
            "status" -> "invalidStatus"
          }), Instant.now)

          stubGet(completedSectionUrl(journeyName, taxYear), OK, Json.toJson(model).toString)

          route(appWithTailoring, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }
      }

      "the user requested data for incorrect journey, then the page" should {

        "display an error" which {
          lazy val headers = playSessionCookie() ++ Map(csrfContent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", invalidSectionCompletedUrl)
            .withHeaders(headers: _*)


          lazy val result = {
            authoriseAgentOrIndividual(isAgent = false)
            val status: JourneyStatus = InProgress
            val model = JourneyAnswers(mtditid, taxYear, "donations-using-gift-aid", Json.obj({
              "status" -> status
            }), Instant.now)

            stubGet(completedSectionUrl("donations-using-gift-aid",taxYear), OK, Json.toJson(model).toString())

            route(appWithTailoring, request).get
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of BAD_REQUEST" in {
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

        }
      }
    }

      s".submit" when {

        s"the request is $testNameWelsh" should {

          "the user submit-test incorrect journey, then the page" should {

            "display an error" which {
              lazy val headers = playSessionCookie() ++ Map(csrfContent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
              lazy val request = FakeRequest("POST", invalidSectionCompletedUrl)
                .withHeaders(headers: _*).withFormUrlEncodedBody(("value","true"))


              lazy val result = {
                authoriseAgentOrIndividual(isAgent = false)
                val status: JourneyStatus = InProgress
                val model = JourneyAnswers(mtditid, taxYear, "invalidName", Json.obj({
                  "status" -> status
                }), Instant.now)

//                stubPost(completedSectionUrl("invalidName", taxYear), OK, Json.toJson(model).toString())

                route(appWithTailoring, request).get
              }

              implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

              "has a status of BAD_REQUEST" in {
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }

            }
          }

          "redirect to the common task list page after user submits response as 'No'" which {
            lazy val result = {
              authoriseAgentOrIndividual(isAgent = false)
              dropStockDividendsDB()
              emptyUserDataStub()
              emptyStockDividendsUserDataStub()
              val request = FakeRequest("POST", sectionCompletedUrl,
                Headers.apply(playSessionCookie() :+ ("Csrf-Token" -> "nocheck"): _*), "{}")

              await(route(appWithTailoring, request, Map("value" -> Seq("false"))).get)
            }

            "has a status of SEE_OTHER(303)" in {
              result.header.status shouldBe SEE_OTHER
            }

            "have the correct redirect location" in {
              result.header.headers("location") shouldBe appConfig.commonTaskListUrl(taxYear)
            }
          }

          "redirect to the common task list page after user submits response as 'Yes'" which {
            lazy val result = {
              authoriseAgentOrIndividual(isAgent = false)

              val request = FakeRequest("POST", sectionCompletedUrl,
                Headers.apply(playSessionCookie() :+ ("Csrf-Token" -> "nocheck"): _*), "{}")

              await(route(appWithTailoring, request, Map("value" -> Seq("true"))).get)
            }

            "has a status of SEE_OTHER(303)" in {
              result.header.status shouldBe SEE_OTHER
            }

            "have the correct redirect location" in {
              result.header.headers("location") shouldBe appConfig.commonTaskListUrl(taxYear)
            }
          }
        }
      }
    }
}
