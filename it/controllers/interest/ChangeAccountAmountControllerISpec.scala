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

import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class ChangeAccountAmountControllerISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper{

  val taxYear: Int = 2022

  val amount: BigDecimal = 25
  val differentAmount: BigDecimal = 30
  val accountName: String = "HSBC"

  val taxYearMinusOne: Int = taxYear - 1

  def url(newId: String, accountType: String): String = s"$appUrl/$taxYear/interest/change-$accountType-uk-interest?accountId=$newId"

  def urlNoPreFix(newId: String, accountType: String): String =
    s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/change-$accountType-uk-interest?accountId=$newId"

  lazy val id: String = UUID.randomUUID().toString

  val untaxedInterestCyaModel: InterestCYAModel = InterestCYAModel(
    Some(true), Some(false), Seq(InterestAccountModel(Some(id), accountName, Some(amount), None))
  )

  val taxedInterestCyaModel: InterestCYAModel = InterestCYAModel(
    Some(false), Some(true), Seq(InterestAccountModel(Some(id), accountName, None, Some(amount)))
  )

  val taxedCyaSubmitModel: InterestCYAModel = InterestCYAModel(
    Some(false), Some(true), Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", None, Some(amount)))
  )

  val untaxedCyaSubmitModel: InterestCYAModel = InterestCYAModel(
    Some(true), Some(false), Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(amount))),
  )

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val continueButtonSelector = "#continue"
    val continueFormSelector = "#main-content > div > div > form"
    val errorSummarySelector = ".govuk-error-summary"
    val errorSummaryTitleSelector = ".govuk-error-summary__title"
    val errorSummaryTextSelector = ".govuk-error-summary__body"
    val newAmountInputSelector = "#amount"
    val amountInputName = "amount"
    val youToldUsSelector = "#main-content > div > div > form > div > div > label > p"
  }

  trait SpecificExpectedResults {
    val expectedUntaxedTitle: String
    val expectedUntaxedErrorTitle: String
    val expectedTaxedTitle: String
    val expectedTaxedErrorTitle: String
    def expectedErrorEmpty(taxType: String): String
    def expectedErrorOverMax(taxType: String): String
    def expectedErrorInvalid(taxType: String): String
    val expectedUntaxedH1: String
    val expectedTaxedH1: String
    val youToldUsUntaxed: String
    val youToldUsTaxed: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedHintText: String
    val continueText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val expectedHintText = "For example, £193.52"
    val continueText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Llog ar gyfer 6 Ebrill $taxYearMinusOne i 5 Ebrill $taxYear"
    val expectedHintText = "Er enghraifft, £193.52"
    val continueText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedUntaxedTitle = "How much untaxed UK interest did you get?"
    val expectedUntaxedErrorTitle = s"Error: $expectedUntaxedTitle"
    val expectedTaxedTitle = "How much taxed UK interest did you get?"
    val expectedTaxedErrorTitle = s"Error: $expectedTaxedTitle"
    def expectedErrorEmpty(taxType: String): String = s"Enter the amount of $taxType UK interest you got"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    val expectedUntaxedH1 = "HSBC: how much untaxed UK interest did you get?"
    val expectedTaxedH1 = "HSBC: how much taxed UK interest did you get?"
    val youToldUsUntaxed = s"You told us you got £$amount untaxed UK interest. Tell us if this has changed."
    val youToldUsTaxed = s"You told us you got £$amount taxed UK interest. Tell us if this has changed."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedUntaxedTitle = "How much untaxed UK interest did your client get?"
    val expectedUntaxedErrorTitle = s"Error: $expectedUntaxedTitle"
    val expectedTaxedTitle = "How much taxed UK interest did your client get?"
    val expectedTaxedErrorTitle = s"Error: $expectedTaxedTitle"
    def expectedErrorEmpty(taxType: String): String = s"Enter the amount of $taxType UK interest your client got"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    val expectedUntaxedH1 = "HSBC: how much untaxed UK interest did your client get?"
    val expectedTaxedH1 = "HSBC: how much taxed UK interest did your client get?"
    val youToldUsUntaxed = s"You told us your client got £$amount untaxed UK interest. Tell us if this has changed."
    val youToldUsTaxed = s"You told us your client got £$amount taxed UK interest. Tell us if this has changed."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedUntaxedTitle = "Faint o log y DU sydd heb ei drethu a gawsoch?"
    val expectedUntaxedErrorTitle = s"Gwall: $expectedUntaxedTitle"
    val expectedTaxedTitle = "Faint o log y DU a drethwyd a gawsoch?"
    val expectedTaxedErrorTitle = s"Gwall: $expectedTaxedTitle"
    def expectedErrorEmpty(taxType: String): String = s"Nodwch swm y $taxType llog y DU a gawsoch"
    def expectedErrorOverMax(taxType: String): String = s"Mae’n rhaid i swm $taxType llog y DU fod yn llai na £100,000,000,000"
    def expectedErrorInvalid(taxType: String): String = s"Nodwch swm y $taxType llog y DU yn y fformat cywir"
    val expectedUntaxedH1 = "HSBC: Faint o log y DU sydd heb ei drethu a gawsoch?"
    val expectedTaxedH1 = "HSBC: Faint o log y DU a drethwyd a gawsoch?"
    val youToldUsUntaxed = s"Gwnaethoch ddweud wrthym cawsoch £$amount Llog y DU sydd heb ei drethu. Rhowch wybod i ni a yw hyn wedi newid."
    val youToldUsTaxed = s"Gwnaethoch ddweud wrthym cawsoch £$amount Llog y DU a drethwyd. Rhowch wybod i ni a yw hyn wedi newid."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedUntaxedTitle = "Faint o log y DU sydd heb ei drethu a gafodd eich cleient?"
    val expectedUntaxedErrorTitle = s"Gwall: $expectedUntaxedTitle"
    val expectedTaxedTitle = "Faint o log y DU a drethwyd a gafodd eich cleient?"
    val expectedTaxedErrorTitle = s"Gwall: $expectedTaxedTitle"
    def expectedErrorEmpty(taxType: String): String = s"Nodwch swm y $taxType llog y DU a gafodd eich cleient"
    def expectedErrorOverMax(taxType: String): String = s"Mae’n rhaid i swm $taxType llog y DU fod yn llai na £100,000,000,000"
    def expectedErrorInvalid(taxType: String): String = s"Nodwch swm y $taxType llog y DU yn y fformat cywir"
    val expectedUntaxedH1 = "HSBC: Faint o log y DU sydd heb ei drethu a gafodd eich cleient?"
    val expectedTaxedH1 = "HSBC: Faint o log y DU a drethwyd a gafodd eich cleient?"
    val youToldUsUntaxed = s"Gwnaethoch ddweud wrthym cafodd eich cleient £$amount Llog y DU sydd heb ei drethu. Rhowch wybod i ni a yw hyn wedi newid."
    val youToldUsTaxed = s"Gwnaethoch ddweud wrthym cafodd eich cleient £$amount Llog y DU a drethwyd. Rhowch wybod i ni a yw hyn wedi newid."
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

    userScenarios.foreach { us =>

      import Selectors._
      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" should {

        Seq(
          (untaxedInterestCyaModel,InterestModel(accountName, id, None, Some(amount)),/*untaxed =*/true),
          (taxedInterestCyaModel,InterestModel(accountName, id, Some(amount), None),/*untaxed =*/false)
        ) foreach {
          testCase =>
            s"return OK and correctly render the page for ${if(testCase._3) "untaxed" else "taxed"}" when {
              lazy val result = {

                dropInterestDB()
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                authoriseAgentOrIndividual(us.isAgent)
                urlGet(url(id, if(testCase._3) "untaxed" else "taxed"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"has an OK($OK) status" in {
                result.status shouldBe OK
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              titleCheck(if(testCase._3) specific.expectedUntaxedTitle else specific.expectedTaxedTitle, us.isWelsh)
              welshToggleCheck(us.isWelsh)
              h1Check((if(testCase._3) specific.expectedUntaxedH1 else specific.expectedTaxedH1) + " " + expectedCaption)
              textOnPageCheck(if(testCase._3) specific.youToldUsUntaxed else specific.youToldUsTaxed, youToldUsSelector)
              textOnPageCheck(expectedCaption, captionSelector)
              hintTextCheck(expectedHintText)
              inputFieldCheck(amountInputName, newAmountInputSelector)
              buttonCheck(continueText, continueButtonSelector)
              formPostLinkCheck(urlNoPreFix(id, if(testCase._3) "untaxed" else "taxed"), continueFormSelector)
              inputFieldValueCheck("", newAmountInputSelector)
            }
        }

        Seq(
          (InterestCYAModel(
            Some(true), Some(false), Seq(InterestAccountModel(Some(id), accountName, None, None))
          ), InterestModel(accountName, id, None, Some(amount)), /*untaxed =*/ true),
          (InterestCYAModel(
            Some(false), Some(true), Seq(InterestAccountModel(Some(id), accountName, None, None))
          ), InterestModel(accountName, id, Some(amount), None), /*untaxed =*/ false)
        ) foreach {
          testCase =>
            s"return OK and correctly render the page for ${if(testCase._3) "untaxed" else "taxed"} when there is no cya amount" when {
              lazy val result = {

                dropInterestDB()
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                authoriseAgentOrIndividual(us.isAgent)
                urlGet(url(id, if(testCase._3) "untaxed" else "taxed"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"has an OK($OK) status" in {
                result.status shouldBe OK
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              titleCheck(if(testCase._3) specific.expectedUntaxedTitle else specific.expectedTaxedTitle, us.isWelsh)
              welshToggleCheck(us.isWelsh)
              h1Check((if(testCase._3) specific.expectedUntaxedH1 else specific.expectedTaxedH1) + " " + expectedCaption)
              textOnPageCheck(if(testCase._3) specific.youToldUsUntaxed else specific.youToldUsTaxed, youToldUsSelector)
              textOnPageCheck(expectedCaption, captionSelector)
              hintTextCheck(expectedHintText)
              inputFieldCheck(amountInputName, newAmountInputSelector)
              buttonCheck(continueText, continueButtonSelector)
              formPostLinkCheck(urlNoPreFix(id, if(testCase._3) "untaxed" else "taxed"), continueFormSelector)
              inputFieldValueCheck(amount.toString(), newAmountInputSelector)
            }
        }

        Seq(
          (InterestCYAModel(
            Some(true), Some(false), Seq(InterestAccountModel(Some(id), accountName, Some(differentAmount)))
          ),InterestModel(accountName, id, None, Some(amount)),/*untaxed =*/true),
          (InterestCYAModel(
            Some(false), Some(true), Seq(InterestAccountModel(Some(id), accountName, None, Some(differentAmount)))
          ),InterestModel(accountName, id, Some(amount), None),/*untaxed =*/false)
        ) foreach {
          testCase =>

            s"render the ${if (testCase._3) "untaxed" else "taxed"} change amount page with pre-populated amount box" which {

              lazy val result = {
                dropInterestDB()
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                authoriseAgentOrIndividual(us.isAgent)
                urlGet(url(id, if(testCase._3) "untaxed" else "taxed"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              implicit val document: () => Document = () => Jsoup.parse(result.body)

              titleCheck(if(testCase._3) specific.expectedUntaxedTitle else specific.expectedTaxedTitle, us.isWelsh)
              inputFieldValueCheck(differentAmount.toString(), newAmountInputSelector)
            }
        }

        Seq(
          (InterestCYAModel(
            Some(true), Some(false), Seq(InterestAccountModel(Some(id), accountName))
          ),InterestModel(accountName, id, None, None),/*untaxed =*/true),
          (InterestCYAModel(
            Some(false), Some(true), Seq(InterestAccountModel(Some(id), accountName))
          ),InterestModel(accountName, id, None, None),/*untaxed =*/false)
        ) foreach {
          testCase =>

            s"render the ${if (testCase._3) "untaxed" else "taxed"} change amount page without paragraph text" when {

              "there is no amount previously submitted" which {

                lazy val result = {
                  dropInterestDB()
                  insertCyaData(Some(testCase._1))
                  userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(url(id, if (testCase._3) "untaxed" else "taxed"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                implicit val document: () => Document = () => Jsoup.parse(result.body)

                titleCheck(if(testCase._3) specific.expectedUntaxedTitle else specific.expectedTaxedTitle, us.isWelsh)
                textOnPageCheck("", youToldUsSelector)
                inputFieldValueCheck("", newAmountInputSelector)

              }
            }
        }

        Seq(
          /*untaxed =*/true,
          /*untaxed =*/false
        ) foreach {
          testCase =>

            s"redirect to the overview page for ${if (testCase) "untaxed" else "taxed"} accounts" when {

              "there is no prior or cya data" in {

                lazy val result = {
                  dropInterestDB()
                  insertCyaData(None)
                  userDataStub(IncomeSourcesModel(interest = Some(Seq())), nino, taxYear)
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(url(id, if (testCase) "untaxed" else "taxed"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                wireMockServer.resetAll()

                result.status shouldBe SEE_OTHER
                result.headers("Location").head.contains("/update-and-submit-income-tax-return/2022/view") shouldBe true
              }
            }
        }

        Seq(
          (InterestCYAModel(
            Some(true), Some(false), Seq(InterestAccountModel(Some(id), accountName, Some(amount)))
          ),/*untaxed =*/true),
          (InterestCYAModel(
            Some(false), Some(true), Seq(InterestAccountModel(Some(id), accountName, None, Some(amount)))
          ),/*untaxed =*/false)
        ) foreach {
          testCase =>

            s"redirect to ${testCase._2} account summary page" when {

              "there is cya data but no prior data" which {

                lazy val result = {
                  dropInterestDB()
                  insertCyaData(Some(testCase._1))
                  userDataStub(IncomeSourcesModel(interest = Some(Seq())), nino, taxYear)
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(url(id, if (testCase._2) "untaxed" else "taxed"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                "returns a SEE OTHER" in {
                  result.status shouldBe SEE_OTHER
                  result.headers("Location").head shouldBe s"/update-and-submit-income-tax-return/personal-income/2022/" +
                    s"interest/accounts-with-${if (testCase._2) "untaxed" else "taxed"}-uk-interest"
                }
              }
            }
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { us =>

      import Selectors._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" when {

        Seq(
          (untaxedCyaSubmitModel, InterestModel(accountName, "UntaxedId", None, Some(amount)), /*untaxed =*/ true),
          (taxedCyaSubmitModel, InterestModel(accountName, "TaxedId", Some(amount), None), /*untaxed =*/ false)
        ) foreach {
          testCase =>
            s"there is both CYA data and prior in session and input is empty for a ${if (testCase._3) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url(testCase._2.incomeSourceId, if (testCase._3) "untaxed" else "taxed"), Map("amount" -> ""),
                  us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"return a BAD_REQUEST($BAD_REQUEST) status" in {
                result.status shouldBe BAD_REQUEST
              }

              implicit val document: () => Document = () => Jsoup.parse(result.body)

              errorSummaryCheck(specific.expectedErrorEmpty(if (testCase._3) "untaxed" else "taxed"), newAmountInputSelector, us.isWelsh)
              errorAboveElementCheck(specific.expectedErrorEmpty(if (testCase._3) "untaxed" else "taxed"))
            }
        }

        Seq(
          (InterestCYAModel(
            Some(true), Some(true), Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(amount),Some(amount)))
          ), InterestModel(accountName, "UntaxedId",  Some(amount), Some(amount)), /*untaxed =*/ true),
          (InterestCYAModel(
            Some(true), Some(true), Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", Some(amount),Some(amount))),
          ), InterestModel(accountName, "TaxedId", Some(amount),  Some(amount)), /*untaxed =*/ false)
        ) foreach {
          testCase =>
            s"there is both CYA data and prior in session and input is valid for a ${if (testCase._3) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url(testCase._2.incomeSourceId, if (testCase._3) "untaxed" else "taxed"), Map("amount" -> "45645.99"),
                  us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"return a SEE OTHER status" in {
                result.status shouldBe SEE_OTHER
                result.headers("Location").head shouldBe s"/update-and-submit-income-tax-return/personal-income/2022/" +
                  s"interest/accounts-with-${if (testCase._3) "untaxed" else "taxed"}-uk-interest"
                findInterestDb shouldBe Some(InterestCYAModel(
                  Some(true),Some(true),Seq(
                    InterestAccountModel(Some(if (testCase._3) "UntaxedId" else "TaxedId"),
                      s"${if (testCase._3) "Untaxed" else "Taxed"} Account",
                      if (testCase._3) Some(45645.99) else Some(25),
                      if (!testCase._3) Some(45645.99) else Some(25)))))
              }
            }
        }

        Seq(
          (InterestCYAModel(Some(true), Some(true), Seq()), InterestModel(accountName, "UntaxedId",  Some(amount), Some(amount)), /*untaxed =*/ true),
          (InterestCYAModel(Some(true), Some(true), Seq()), InterestModel(accountName, "TaxedId", Some(amount),  Some(amount)), /*untaxed =*/ false)
        ) foreach {
          testCase =>
            s"there is no CYA data but there is prior data and input is valid for a ${if (testCase._3) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                authoriseAgentOrIndividual(us.isAgent)
                urlPost(url(testCase._2.incomeSourceId, if (testCase._3) "untaxed" else "taxed"), Map("amount" -> "45645.99"),
                  us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"return a SEE OTHER status" in {
                result.status shouldBe SEE_OTHER
                result.headers("Location").head shouldBe s"/update-and-submit-income-tax-return/personal-income/2022/" +
                  s"interest/accounts-with-${if (testCase._3) "untaxed" else "taxed"}-uk-interest"
                findInterestDb shouldBe Some(InterestCYAModel(
                  Some(true),Some(true),Seq(
                    InterestAccountModel(Some(if (testCase._3) "UntaxedId" else "TaxedId"),
                      accountName,
                      if (testCase._3) Some(45645.99) else Some(25),
                      if (!testCase._3) Some(45645.99) else Some(25)))))
              }
            }
        }

        Seq(
          (untaxedCyaSubmitModel, InterestModel(accountName, "UntaxedId", None, Some(amount)), /*untaxed =*/ true),
          (taxedCyaSubmitModel, InterestModel(accountName, "TaxedId", Some(amount), None), /*untaxed =*/ false)
        ) foreach {
          testCase =>
            s"there is both CYA data and prior in session and input is invalid for a ${if (testCase._3) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                authoriseAgentOrIndividual(us.isAgent)
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                urlPost(url(testCase._2.incomeSourceId, if (testCase._3) "untaxed" else "taxed"), Map("amount" -> "|"),
                  us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"return a BAD_REQUEST($BAD_REQUEST) status" in {
                result.status shouldBe BAD_REQUEST
              }

              implicit val document: () => Document = () => Jsoup.parse(result.body)

              errorSummaryCheck(specific.expectedErrorInvalid(if (testCase._3) "untaxed" else "taxed"), newAmountInputSelector, us.isWelsh)
              errorAboveElementCheck(specific.expectedErrorInvalid(if (testCase._3) "untaxed" else "taxed"))
            }
        }

        Seq(
          (untaxedCyaSubmitModel, InterestModel(accountName, "UntaxedId", None, Some(amount)), /*untaxed =*/ true),
          (taxedCyaSubmitModel, InterestModel(accountName, "TaxedId", Some(amount), None), /*untaxed =*/ false)
        ) foreach {
          testCase =>
            s"there is both CYA data and prior in session and input is over the max for a ${if (testCase._3) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                authoriseAgentOrIndividual(us.isAgent)
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = Some(Seq(testCase._2))), nino, taxYear)
                urlPost(url(testCase._2.incomeSourceId, if (testCase._3) "untaxed" else "taxed"), Map("amount" -> "99999999999999999999999999999999999999999"),
                  us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"return a BAD_REQUEST($BAD_REQUEST) status" in {
                result.status shouldBe BAD_REQUEST
              }

              implicit val document: () => Document = () => Jsoup.parse(result.body)

              errorSummaryCheck(specific.expectedErrorOverMax(if (testCase._3) "untaxed" else "taxed"), newAmountInputSelector, us.isWelsh)
              errorAboveElementCheck(specific.expectedErrorOverMax(if (testCase._3) "untaxed" else "taxed"))
            }
        }

        Seq(
          (untaxedCyaSubmitModel, /*untaxed =*/ true),
          (taxedCyaSubmitModel, /*untaxed =*/ false)
        ) foreach {
          testCase =>
            s"there is CYA data for a ${if (testCase._2) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                authoriseAgentOrIndividual(us.isAgent)
                insertCyaData(Some(testCase._1))
                userDataStub(IncomeSourcesModel(interest = None), nino, taxYear)
                urlPost(url(testCase._1.accounts.head.id.get, if (testCase._2) "untaxed" else "taxed"),
                  Map("" -> ""),
                  us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"return a SEE OTHER status to account summary page" in {
                result.status shouldBe SEE_OTHER
                result.headers("Location").head shouldBe s"/update-and-submit-income-tax-return/personal-income/2022/" +
                  s"interest/accounts-with-${if (testCase._2) "untaxed" else "taxed"}-uk-interest"
              }
            }
        }

        Seq(
          /*untaxed =*/ true,
          /*untaxed =*/ false
        ) foreach {
          testCase =>
            s"there is no CYA data for a ${if (testCase) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                authoriseAgentOrIndividual(us.isAgent)
                insertCyaData(None)
                emptyUserDataStub()
                urlPost(url("randomid", if (testCase) "untaxed" else "taxed"),
                  Map("" -> ""),
                  us.isWelsh, follow = false, playSessionCookie(us.isAgent))
              }

              s"return a SEE OTHER status to account summary page" in {
                result.status shouldBe SEE_OTHER
                result.headers("Location").head shouldBe s"/update-and-submit-income-tax-return/personal-income/2022/" +
                  s"interest/accounts-with-${if (testCase) "untaxed" else "taxed"}-uk-interest"
              }
            }
        }

        Seq(
          /*untaxed =*/ true,
          /*untaxed =*/ false
        ) foreach {
          testCase =>
            s"authorization fails for a ${if (testCase) "untaxed" else "taxed"} submission" should {
              lazy val result = {

                dropInterestDB()
                unauthorisedAgentOrIndividual(us.isAgent)
                urlPost(url("randomid", if (testCase) "untaxed" else "taxed"),
                  Map("" -> ""),
                  us.isWelsh, follow = true, playSessionCookie(us.isAgent))
              }

              s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
                result.status shouldBe UNAUTHORIZED
              }
            }
        }
      }
    }
  }
}