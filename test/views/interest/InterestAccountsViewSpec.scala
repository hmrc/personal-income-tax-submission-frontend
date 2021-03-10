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

package views.interest

import models.interest.InterestAccountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.interest.InterestAccountsView

class InterestAccountsViewSpec extends ViewTest {

  lazy val view: InterestAccountsView = app.injector.instanceOf[InterestAccountsView]

  val taxYear = 2020
  val taxYearMinusOne = taxYear -1

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  val accountRow: Int => String = rowNumber => s".govuk-summary-list__row:nth-child($rowNumber)"
  val accountRowName: Int => String = rowNumber => accountRow(rowNumber) + " > dd:nth-child(1)"
  val accountRowChange: Int => String = rowNumber => accountRow(rowNumber) + " > dd:nth-child(2) > a"
  val accountRowRemove: Int => String = rowNumber => accountRow(rowNumber) + " > dd:nth-child(3)"
  val captionSelector = ".govuk-caption-l"
  val accountRowChangePriorSubmission: Int => String = rowNumber => accountRow(rowNumber) + " > dd:nth-child(3) > a"
  val continueSelector = "#continue"

  val changeUntaxedHref = "/income-through-software/return/personal-income/2020/interest/untaxed-uk-interest-details/qwerty"
  val changePriorUntaxedHref = "/income-through-software/return/personal-income/2020/interest/change-untaxed-interest-account?accountId=azerty"
  val changeTaxedHref = "/income-through-software/return/personal-income/2020/interest/taxed-uk-interest-details/qwerty"
  val changePriorTaxedHref = "/income-through-software/return/personal-income/2020/interest/change-taxed-interest-account?accountId=azerty"
  val removeUntaxedHref = "/income-through-software/return/personal-income/2020/interest/remove-untaxed-interest-account?accountId=qwerty"
  val removeTaxedHref = "/income-through-software/return/personal-income/2020/interest/remove-taxed-interest-account?accountId=qwerty"

  val untaxedH1Singular = "UK untaxed interest account"
  val untaxedH1Plural = "UK untaxed interest accounts"
  val taxedH1Singular = "UK taxed interest account"
  val taxedH1Plural = "UK taxed interest accounts"
  val untaxedTitleSingle = "UK untaxed interest account"
  val untaxedTitlePlural = "UK untaxed interest accounts"
  val taxedTitleSingle = "UK taxed interest account"
  val taxedTitlePlural = "UK taxed interest accounts"
  val captionText = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val changeText = "Change"
  val removeText = "Remove"
  val addAnotherAccountText = "Add another account"
  val continueText = "Continue"

  "InterestAccountsView when untaxed" should {

    "render with 1 row" when {

      "there is a single untaxed account passed in that is not a prior submission" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), UNTAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitleSingle)
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))
        linkCheck(changeText, accountRowChange(1),changeUntaxedHref)

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            document.select(accountRowRemove(1)).text() shouldBe removeText
          }
          s"has the href '$removeUntaxedHref'" in {
            document.select(accountRowRemove(1) + " > a").attr("href") shouldBe removeUntaxedHref
          }
        }

        buttonCheck(continueText, continueSelector)
      }

      "there is a single untaxed account passed in that is a prior submission" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(Some("azerty"), "Bank of UK", 9001.00)
        ), UNTAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitleSingle)
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)

        textOnPageCheck( "Bank of UK", accountRowName(1))
        linkCheck(changeText, accountRowChangePriorSubmission(1),changePriorUntaxedHref)
        buttonCheck(continueText, continueSelector)
      }
    }

    "render with 2 rows" when {

      "there are two accounts passed in, one new account and one prior" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9000.01, Some("qwerty")),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), UNTAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitlePlural)
        h1Check(untaxedH1Plural)
        textOnPageCheck(captionText, captionSelector)

        "have an area for the first row" which {
          textOnPageCheck("Bank of UK", accountRowName(1))
          linkCheck(changeText, accountRowChange(1), changeUntaxedHref)

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              document.select(accountRowRemove(1)).text() shouldBe removeText
            }
            s"has the href '$removeUntaxedHref'" in {
              document.select(accountRowRemove(1) + " > a").attr("href") shouldBe removeUntaxedHref
            }
          }
        }

        "have an area for the second row" which {
          textOnPageCheck("Bank of EU", accountRowName(2))
          linkCheck(changeText, accountRowChangePriorSubmission(2), changePriorUntaxedHref)
        }
        buttonCheck(continueText, continueSelector)
      }
    }
  }

  "InterestAccountsView when taxed" should {

    "render with 1 row" when {

      "there is a single taxed account passed in that is not a prior submission" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), TAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitleSingle)
        textOnPageCheck(captionText, captionSelector)
        h1Check(taxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))
        linkCheck(changeText, accountRowChange(1), changeTaxedHref)

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            document.select(accountRowRemove(1)).text() shouldBe removeText
          }
         s"has the href '$removeTaxedHref'" in {
            document.select(accountRowRemove(1) + " > a").attr("href") shouldBe removeTaxedHref
          }
        }

        buttonCheck(continueText, continueSelector)
      }

      "there is a single taxed account passed in that is a prior submission" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(Some("azerty"), "Bank of UK", 9001.00)
        ), TAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitleSingle)
        textOnPageCheck(captionText, captionSelector)
        h1Check(taxedH1Singular)

        textOnPageCheck( "Bank of UK", accountRowName(1))
        linkCheck(changeText, accountRowChangePriorSubmission(1), changePriorTaxedHref)
        buttonCheck(continueText, continueSelector)
      }
    }

    "render with 2 rows" when {

      "there are two accounts passed in, one new account and one prior" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9000.01, Some("qwerty")),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), TAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitlePlural)
        h1Check(taxedH1Plural)
        textOnPageCheck(captionText, captionSelector)

        "have an area for the first row" which {
          textOnPageCheck("Bank of UK", accountRowName(1))
          linkCheck(changeText, accountRowChange(1), changeTaxedHref)

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              document.select(accountRowRemove(1)).text() shouldBe removeText
            }
            s"has the href '$removeTaxedHref'" in {
              document.select(accountRowRemove(1) + " > a").attr("href") shouldBe removeTaxedHref
            }
          }
        }

        "have an area for the second row" which {
          textOnPageCheck("Bank of EU", accountRowName(2))
          linkCheck(changeText, accountRowChangePriorSubmission(2), changePriorTaxedHref)
        }
        buttonCheck(continueText, continueSelector)
      }
    }
  }

}
