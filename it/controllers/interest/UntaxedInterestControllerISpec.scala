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

package controllers.interest

import java.util.UUID
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, Clock, InterestDatabaseHelper, ViewHelpers}

class UntaxedInterestControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {

  val amount: BigDecimal = 25

  lazy val id: String = UUID.randomUUID().toString

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val forExampleSelector = "#p1"
    val bulletPointSelector1 = "#main-content > div > div > ul > li:nth-child(1)"
    val bulletPointSelector2 = "#main-content > div > div > ul > li:nth-child(2)"
    val bulletPointSelector3 = "#main-content > div > div > ul > li:nth-child(3)"
    val doNotIncludeSelector = "#p2"
    val continueSelector = "#continue"
    val continueFormSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
  }

  import Selectors._

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val doNotIncludeText: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val forExampleText: String
    val banksAndBuildingsText: String
    val savingsAndCreditText: String
    val peerToPeerText: String
    val yesText: String
    val noText: String
    val continueText: String
    val continueLink: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Interest for 6 April $taxYearEOY to 5 April $taxYear"
    val forExampleText = "This could be interest from:"
    val banksAndBuildingsText = "banks and building societies"
    val savingsAndCreditText = "savings and credit union accounts"
    val peerToPeerText = "peer-to-peer lending"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/untaxed-uk-interest"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Llog ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val forExampleText = "Gallai hyn fod yn llog gan:"
    val banksAndBuildingsText = "banciau a chymdeithasau adeiladu"
    val savingsAndCreditText = "cyfrifon cynilion ac undebau credyd"
    val peerToPeerText = "benthyca cymar i gymar"
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/untaxed-uk-interest"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get untaxed interest from the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = "Did you get untaxed interest from the UK?"
    val doNotIncludeText: String = "Do not include interest you got from an Individual Savings Account (ISA) or gilts."
    val expectedErrorText = "Select yes if you got untaxed UK interest"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get untaxed interest from the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = "Did your client get untaxed interest from the UK?"
    val doNotIncludeText: String = "Do not include interest your client got from an Individual Savings Account (ISA) or gilts."
    val expectedErrorText = "Select yes if your client got untaxed UK interest"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch log y DU sydd heb ei drethu?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedH1 = "A gawsoch log y DU sydd heb ei drethu?"
    val doNotIncludeText: String = "Peidiwch â chynnwys llog a gawsoch gan Gyfrif Cynilo Unigol (ISA) neu giltiau."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch log y DU sydd heb ei drethu"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A chafodd eich cleient llog y DU sydd heb ei drethu?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedH1 = "A chafodd eich cleient llog y DU sydd heb ei drethu?"
    val doNotIncludeText: String = "Peidiwch â chynnwys llog a gafodd eich cleient gan Gyfrif Cynilo Unigol (ISA) neu giltiau."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient llog y DU sydd heb ei drethu"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }


  ".show" when {
    val url = s"$appUrl/$taxYear/interest/untaxed-uk-interest"

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" should {

        "return OK and correctly render the page" when {
          "there is no cya data in session" which {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()

              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(specific.expectedTitle, us.isWelsh)
            welshToggleCheck(us.isWelsh)
            h1Check(specific.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)

            textOnPageCheck(forExampleText, forExampleSelector)
            textOnPageCheck(banksAndBuildingsText, bulletPointSelector1)
            textOnPageCheck(savingsAndCreditText, bulletPointSelector2)
            textOnPageCheck(peerToPeerText, bulletPointSelector3)

            textOnPageCheck(specific.doNotIncludeText, doNotIncludeSelector)

            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "there is cya data in session" which {
            val interestCYA = InterestCYAModel(
              None,
              Some(true),
              Some(false),Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(25.00), createdAt = clock.localDateTimeNow()))
            )

            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertInterestCyaData(Some(interestCYA))

              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(specific.expectedTitle, us.isWelsh)
            welshToggleCheck(us.isWelsh)
            h1Check(specific.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)

            textOnPageCheck(forExampleText, forExampleSelector)
            textOnPageCheck(banksAndBuildingsText, bulletPointSelector1)
            textOnPageCheck(savingsAndCreditText, bulletPointSelector2)
            textOnPageCheck(peerToPeerText, bulletPointSelector3)

            textOnPageCheck(specific.doNotIncludeText, doNotIncludeSelector)

            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }
        }

        "return a redirect to CYA when previous data exists with untaxed accounts" which {
          lazy val result = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(interest = Some(
              Seq(
                InterestModel(
                  "Accounty","1234567890",Some(1),Some(1)
                )
              )
            )), nino, taxYear)
            insertInterestCyaData(None)

            authoriseAgentOrIndividual(us.isAgent)
            urlGet(url, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest")
          }
        }

        "return an UNAUTHORIZED when auth call fails" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(None)

            unauthorisedAgentOrIndividual(us.isAgent)
            urlGet(url, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }

  ".submit" when {

    val url = s"$appUrl/$taxYear/interest/untaxed-uk-interest"

    val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" should {

        "return BAD_REQUEST when yes/no form is empty" which {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(None)

            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url, yesNoFormEmpty, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"has an BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedErrorTitle, us.isWelsh)
          welshToggleCheck(us.isWelsh)
          h1Check(specific.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          errorSummaryCheck(specific.expectedErrorText, errorSummaryHref, us.isWelsh)
          errorAboveElementCheck(specific.expectedErrorText)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueFormSelector)
        }

        "return a redirect to CYA when previous data exists with untaxed accounts" which {
          lazy val result = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(interest = Some(
              Seq(
                InterestModel(
                  "Accounty","1234567890",Some(1),Some(1)
                )
              )
            )), nino, taxYear)
            insertInterestCyaData(None)

            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          "has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest")
          }
        }

        "return SEE_OTHER" when {

          "the yes/no form has the answer YES" when {

            "there is cyaData in session" which {
              lazy val result: WSResponse = {
                dropInterestDB()
                emptyUserDataStub()
                insertInterestCyaData(Some(InterestCYAModel(None, Some(true))))

                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              "has a SEE_OTHER(303) status" in {
                result.status shouldBe SEE_OTHER
              }

              "redirects to the which-account-did-you-get-untaxed-interest-from page" in {

                result.header("Location").get should include(
                  s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/which-account-did-you-get-untaxed-interest-from")
              }
            }

            "there is no cyaData in session" which {
              lazy val result: WSResponse = {
                dropInterestDB()
                emptyUserDataStub()

                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              "has a SEE_OTHER(303) status" in {
                result.status shouldBe SEE_OTHER
              }

              "redirects to the untaxed interest amount page" in {
                result.header("Location").get should include(
                  s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/which-account-did-you-get-untaxed-interest-from")
              }
            }

            "redirects to INTEREST CYA page when the cya model is finished" when {
              lazy val interestCYA = InterestCYAModel(
                None, Some(true), Some(false), Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(amount), createdAt = clock.localDateTimeNow()))
              )

              lazy val result: WSResponse = {
                dropInterestDB()
                emptyUserDataStub()
                insertInterestCyaData(Some(interestCYA))

                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              "has a SEE_OTHER(303) status" in {
                result.status shouldBe SEE_OTHER
              }

              "redirects to the interest CYA page" in {
                result.header("Location") shouldBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest")
              }
            }
          }

          "the yes/no form has the answer NO" when {

            "there is cyaData in session" which {
              lazy val result: WSResponse = {
                dropInterestDB()
                emptyUserDataStub()
                insertInterestCyaData(Some(InterestCYAModel(None, Some(true))))

                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              "has a SEE_OTHER(303) status" in {
                result.status shouldBe SEE_OTHER
              }

              "redirects to the receive tax interest page" in {
                result.headers("Location").head shouldBe s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/taxed-uk-interest"
              }
            }

            "there is no cyaData in session" which {
              lazy val result: WSResponse = {
                dropInterestDB()
                emptyUserDataStub()

                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              "has a SEE_OTHER(303) status" in {
                result.status shouldBe SEE_OTHER
              }

              "redirects to the receive tax interest page" in {
                result.headers("Location").head shouldBe s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/taxed-uk-interest"
              }
            }

            "redirects to INTEREST CYA page when the cya model is finished" when {
              lazy val interestCYA = InterestCYAModel(
                None, Some(true), Some(false), Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(amount), createdAt = clock.localDateTimeNow()))
              )

              lazy val result: WSResponse = {
                dropInterestDB()
                emptyUserDataStub()
                insertInterestCyaData(Some(interestCYA))

                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              "has a SEE_OTHER(303) status" in {
                result.status shouldBe SEE_OTHER
              }

              "redirects to the interest CYA page" in {
                result.header("Location") shouldBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest")
              }
            }
          }

        }

        "returns UNAUTHORIZED when auth call fails" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(None)

            unauthorisedAgentOrIndividual(us.isAgent)
            urlPost(url, yesNoFormEmpty, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }

  }

}
