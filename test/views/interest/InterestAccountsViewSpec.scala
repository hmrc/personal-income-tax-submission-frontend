/*
 * Copyright 2020 HM Revenue & Customs
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

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  object Selectors {
    val accountRow: Int => String = rowNumber => s".govuk-summary-list__row:nth-child($rowNumber)"
    val accountRowName: Int => String = rowNumber => accountRow(rowNumber) + " > dd:nth-child(1)"
    val accountRowChange: Int => String = rowNumber => accountRow(rowNumber) + " > dd:nth-child(2) > a"
    val accountRowRemove: Int => String = rowNumber => accountRow(rowNumber) + " > dd:nth-child(3)"
  }

  object ExpectedValues {
    val h1Singular = "You have added 1 account"
    val h1Plural: Int => String = amount => s"You have added $amount accounts"

    val caption = "Interest for 06 April 2019 to 05 April 2020"

    val change = "Change"
    val remove = "Remove"

    val addAnotherAccount = "Add another account"

    val titleSingle = "You have added 1 account"
    val titlePlural = "You have added 2 accounts"
  }

  "InterestAccountsView when untaxed" should {

    "render with 1 row" when {

      "there is a single account passed in" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(Some("qwerty"), "Bank of UK", 9001.00)
        ), UNTAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        "has the correct title" in {
          assertTitle(ExpectedValues.titleSingle)
        }

        "has the correct caption" in {
          assertCaption(ExpectedValues.caption)
        }

        "has the correct h1" in {
          assertH1(ExpectedValues.h1Singular)
        }

        "has a single account row" in {
          elementExist(Selectors.accountRow(1)) shouldBe true
          elementExist(Selectors.accountRow(2)) shouldBe false
        }

        "the row should have the correct account name" in {
          elementText(Selectors.accountRowName(1)) shouldBe "Bank of UK"
        }

        "the row should have a change link" which {

          "has the correct text" in {
            elementText(Selectors.accountRowChange(1)) shouldBe ExpectedValues.change
          }

          "has the correct link" in {
            element(Selectors.accountRowChange(1)).attr("href") shouldBe controllers.interest.routes.UntaxedInterestAmountController
              .show(taxYear, Some("qwerty")).url
          }

        }

        "the row should have a remove link" which {

          "has the correct text" in {
            elementText(Selectors.accountRowRemove(1)) shouldBe ExpectedValues.remove
          }

          //TODO this will need doing during when the functionality exist
          "has the correct link" in pending
        }

      }

    }

    "render with 2 rows" when {

      "there are two accounts passed in" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(Some("qwerty"), "Bank of UK", 9000.01),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), UNTAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        "has the correct title" in {
          assertTitle(ExpectedValues.titlePlural)
        }

        "has the correct caption" in {
          assertCaption(ExpectedValues.caption)
        }

        "has the correct h1" in {
          assertH1(ExpectedValues.h1Plural(2))
        }

        "has two account rows" in {
          elementExist(Selectors.accountRow(1)) shouldBe true
          elementExist(Selectors.accountRow(2)) shouldBe true
          elementExist(Selectors.accountRow(3)) shouldBe false
        }

        "the first row" should {
          "have the correct account name" in {
            elementText(Selectors.accountRowName(1)) shouldBe "Bank of UK"
          }

          "the row should have a change link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowChange(1)) shouldBe ExpectedValues.change
            }

            "has the correct link" in {
              element(Selectors.accountRowChange(1)).attr("href") shouldBe
                controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, Some("qwerty")).url
            }

          }

          "the row should have a remove link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowRemove(1)) shouldBe ExpectedValues.remove
            }

            //TODO this will need doing during when the functionality exist
            "has the correct link" in pending
          }
        }

        "the second row" should {
          "have the correct account name" in {
            elementText(Selectors.accountRowName(2)) shouldBe "Bank of EU"
          }

          "the row should have a change link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowChange(2)) shouldBe ExpectedValues.change
            }

            "has the correct link" in {
              element(Selectors.accountRowChange(2)).attr("href") shouldBe
                controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, Some("azerty")).url
            }

          }

          "the row should have a remove link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowRemove(2)) shouldBe ExpectedValues.remove
            }

            //TODO this will need doing during when the functionality exist
            "has the correct link" in pending
          }
        }

      }

    }

  }

  "InterestAccountsView when taxed" should {

    "render with 1 row" when {

      "there is a single account passed in" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(Some("qwerty"), "Bank of UK", 9001.00)
        ), TAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        "has the correct title" in {
          assertTitle(ExpectedValues.titleSingle)
        }

        "has the correct caption" in {
          assertCaption(ExpectedValues.caption)
        }

        "has the correct h1" in {
          assertH1(ExpectedValues.h1Singular)
        }

        "has a single account row" in {
          elementExist(Selectors.accountRow(1)) shouldBe true
          elementExist(Selectors.accountRow(2)) shouldBe false
        }

        "the row should have the correct account name" in {
          elementText(Selectors.accountRowName(1)) shouldBe "Bank of UK"
        }

        "the row should have a change link" which {

          "has the correct text" in {
            elementText(Selectors.accountRowChange(1)) shouldBe ExpectedValues.change
          }

          "has the correct link" in {
            element(Selectors.accountRowChange(1)).attr("href") shouldBe controllers.interest.routes.TaxedInterestAmountController
              .show(taxYear, Some("qwerty")).url
          }

        }

        "the row should have a remove link" which {

          "has the correct text" in {
            elementText(Selectors.accountRowRemove(1)) shouldBe ExpectedValues.remove
          }

          //TODO this will need doing during when the functionality exist
          "has the correct link" in pending
        }

      }

    }

    "render with 2 rows" when {

      "there are two accounts passed in" which {

        lazy val result = view(taxYear, Seq(
          InterestAccountModel(Some("qwerty"), "Bank of UK", 9000.01),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), TAXED)
        implicit val document: Document = Jsoup.parse(result.body)

        "has the correct title" in {
          assertTitle(ExpectedValues.titlePlural)
        }

        "has the correct caption" in {
          assertCaption(ExpectedValues.caption)
        }

        "has the correct h1" in {
          assertH1(ExpectedValues.h1Plural(2))
        }

        "has two account rows" in {
          elementExist(Selectors.accountRow(1)) shouldBe true
          elementExist(Selectors.accountRow(2)) shouldBe true
          elementExist(Selectors.accountRow(3)) shouldBe false
        }

        "the first row" should {
          "have the correct account name" in {
            elementText(Selectors.accountRowName(1)) shouldBe "Bank of UK"
          }

          "the row should have a change link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowChange(1)) shouldBe ExpectedValues.change
            }

            "has the correct link" in {
              element(Selectors.accountRowChange(1)).attr("href") shouldBe
                controllers.interest.routes.TaxedInterestAmountController.show(taxYear, Some("qwerty")).url
            }

          }

          "the row should have a remove link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowRemove(1)) shouldBe ExpectedValues.remove
            }

            //TODO this will need doing during when the functionality exist
            "has the correct link" in pending
          }
        }

        "the second row" should {
          "have the correct account name" in {
            elementText(Selectors.accountRowName(2)) shouldBe "Bank of EU"
          }

          "the row should have a change link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowChange(2)) shouldBe ExpectedValues.change
            }

            "has the correct link" in {
              element(Selectors.accountRowChange(2)).attr("href") shouldBe
                controllers.interest.routes.TaxedInterestAmountController.show(taxYear, Some("azerty")).url
            }

          }

          "the row should have a remove link" which {

            "has the correct text" in {
              elementText(Selectors.accountRowRemove(2)) shouldBe ExpectedValues.remove
            }

            //TODO this will need doing during when the functionality exist
            "has the correct link" in pending
          }
        }

      }

    }

  }

}
