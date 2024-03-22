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

package test.controllers.interest

import models.interest.InterestCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import play.mvc.Http.HeaderNames
import test.utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class InterestGatewayControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val valueHref = "#value"
    val continueSelector = "#continue"
    val continueFormSelector = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val yesText: String
    val noText: String
    val continueText: String
    val continueLink: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Interest for 6 April $taxYearEOY to 5 April $taxYear"
    val yesText: String = "Yes"
    val noText: String = "No"
    val continueText: String = "Continue"
    val continueLink: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/interest-from-UK"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Llog ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesText: String = "Iawn"
    val noText: String = "Na"
    val continueText: String = "Yn eich blaen"
    val continueLink: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/interest-from-UK"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did you get any interest from the UK?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "Did you get any interest from the UK?"
    val expectedErrorText: String = "Select yes if you got interest from the UK"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "A gawsoch unrhyw log gan y DU?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1: String = "A gawsoch unrhyw log gan y DU?"
    val expectedErrorText: String = "Dewiswch ‘Iawn’ os cawsoch log gan y DU"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client get any interest from the UK?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedH1: String = "Did your client get any interest from the UK?"
    val expectedErrorText: String = "Select yes if your client got interest from the UK"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "A gafodd eich cleient unrhyw log gan y DU?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1: String = "A gafodd eich cleient unrhyw log gan y DU?"
    val expectedErrorText: String = "Dewiswch ‘Iawn’ os cafodd eich cleient log gan y DU"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  val url: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/interest-from-UK"
  val fullUrl: String = appUrl + s"/$taxYear/interest/interest-from-UK"

  val untaxedInterestUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/untaxed-uk-interest"
  val interestCYAUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest"

  ".show" when {

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specificResults = us.specificExpectedResults.get

      s"the user is ${agentTest(us.isAgent)} and the request is ${welshTest(us.isWelsh)}" when {

        "the tailoring feature switch is turned on" should {

          "display the gateway page" which {

            lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
            lazy val request = FakeRequest("GET", url).withHeaders(headers: _*)

            lazy val result = {
              authoriseAgentOrIndividual(us.isAgent)
              dropInterestDB()
              emptyUserDataStub()
              route(appWithTailoring, request, "{}").get
            }

            implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

            "has an OK(200) status" in {
              status(result) shouldBe OK
            }

            titleCheck(specificResults.expectedTitle, us.isWelsh)
            welshToggleCheck(us.isWelsh)
            h1Check(specificResults.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, Selectors.captionSelector)

            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, Selectors.continueSelector)
            formPostLinkCheck(continueLink, Selectors.continueFormSelector)

          }
        }
      }
    }

    "the tailoring feature switch is turned off" should {

      "redirect the user back to the overview page" which {

        lazy val result = {
          authoriseAgentOrIndividual(user.isAgent)
          dropInterestDB()
          emptyUserDataStub()
          urlGet(fullUrl, follow = false, headers = playSessionCookie(user.isAgent))
        }

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct redirect location" in {
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }
    }
  }


  ".submit" when {
    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specificResults = us.specificExpectedResults.get

      s"the user is ${agentTest(us.isAgent)} and the request is ${welshTest(us.isWelsh)}" when {
        lazy val headers = playSessionCookie(us.isAgent) ++ Map(csrfContent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("POST", url).withHeaders(headers: _*)

        "the feature switch is on and the user submits incorrect data" should {

          "display an error" which {

            lazy val result = {
              authoriseAgentOrIndividual(us.isAgent)
              dropInterestDB()
              emptyUserDataStub()
              route(appWithTailoring, request, Map("value" -> Seq("error"))).get
            }

            implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

            "has a status of BAD_REQUEST" in {
              status(result) shouldBe BAD_REQUEST
            }

            errorSummaryCheck(specificResults.expectedErrorText, "#value", us.isWelsh)
            titleCheck(specificResults.expectedErrorTitle, us.isWelsh)
            welshToggleCheck(us.isWelsh)
            h1Check(specificResults.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, Selectors.captionSelector)

            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, Selectors.continueSelector)
            formPostLinkCheck(continueLink, Selectors.continueFormSelector)

          }
        }
      }
    }

    "the feature switch is on" should {

      lazy val headers = playSessionCookie(user.isAgent) ++ Map(csrfContent)
      lazy val request = FakeRequest("POST", url).withHeaders(headers: _*)


      "redirect the user to the untaxed interest radio button page when the user submits 'Yes' and no previous data exists" which {

        lazy val result = {
          authoriseAgentOrIndividual(user.isAgent)
          dropInterestDB()
          emptyUserDataStub()
          route(appWithTailoring, request, Map("value" -> Seq("true"))).get
        }

        "has the status SEE_OTHER (303)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect location" in {
          await(result).header.headers("location") shouldBe untaxedInterestUrl
        }
      }

      "the user submits 'Yes' and previous data exists" should {

        "redirect the user to the untaxed interest page" which {

          lazy val result = {
            authoriseAgentOrIndividual(user.isAgent)
            dropInterestDB()
            insertInterestCyaData(Some(InterestCYAModel(gateway = Some(false))))
            emptyUserDataStub()
            route(appWithTailoring, request, Map("value" -> Seq("true"))).get
          }

          "has a status of SEE_OTHER (303" in {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect location" in {
            await(result).header.headers("location") shouldBe untaxedInterestUrl
          }
        }
      }

      "the user submits a 'No" should {

        "redirect the user back to the CYA page" which { //needs to redirect to the zero warning page when it is built

          lazy val result = {
            authoriseAgentOrIndividual(user.isAgent)
            dropInterestDB()
            emptyUserDataStub()
            route(appWithTailoring, request, Map("value" -> Seq("false"))).get
          }

          "has a status of SEE_OTHER (303)" in {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect location" in {
            await(result).header.headers("location") shouldBe interestCYAUrl
          }
        }
      }
    }


    "the feature switch is off" should {

      "redirect to the overview page" which {

        lazy val result = {
          authoriseAgentOrIndividual(user.isAgent)
          dropInterestDB()
          emptyUserDataStub()
          urlPost(fullUrl, follow = false, headers = playSessionCookie(user.isAgent), body = "{}")
        }

        "has a status of SEE_OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct redirect location" in {
          result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }
    }
  }
}
