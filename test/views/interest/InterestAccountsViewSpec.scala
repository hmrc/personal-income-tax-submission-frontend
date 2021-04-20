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

import forms.YesNoForm
import models.interest.InterestAccountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.interest.InterestAccountsView

class InterestAccountsViewSpec extends ViewTest {

  lazy val view: InterestAccountsView = app.injector.instanceOf[InterestAccountsView]

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  private val untaxedYesNoForm =
    YesNoForm.yesNoForm("Select yes if you received untaxed interest from the UK").bind(Map("value" -> "true"))
  private val taxedYesNoForm =
    YesNoForm.yesNoForm("Select yes if you received taxed interest from the UK").bind(Map("value" -> "true"))

  val accountRow: Int => String = rowNumber => s".govuk-form-group > ul > li:nth-child($rowNumber)"
  val accountRowName: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(1)"
  val accountRowChange: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(2) > a"
  val accountRowChangeHidden: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(2) > a > span.govuk-visually-hidden"
  val accountRowRemove: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a"
  val accountRowRemoveHidden: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a > span.govuk-visually-hidden"

  val accountRowChangePriorSubmission: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a"
  val accountRowChangePriorSubmissionHidden: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a > span.govuk-visually-hidden"

  val captionSelector = ".govuk-caption-l"
  val continueSelector = "#continue"
  val continueFormSelector = "#main-content > div > div > form"
  val doYouNeedSelector = "#main-content > div > div > form > div > fieldset > legend > div"
  val youMustTellSelector = "#interest-account-view-radio-hint"

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
  val errorTitleText: String => String = (titleText: String) => s"Error: $titleText"
  val doYouNeedText = "Do you need to add another account?"
  val youMustTellText = "You must tell us about all your accounts."
  val yesText = "Yes"
  val noText = "No"

  "InterestAccountsView when untaxed in English" should {
    "render with 1 row" when {

      "there is a single untaxed account passed in that is not a prior submission" which {
        lazy val result = view(untaxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), UNTAXED)(fakeRequest, messages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitleSingle)
        welshToggleCheck("English")
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChange(1)).child(0).text shouldBe changeText
          }

          "has the correct hidden text" in {
            element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }

          "has the correct link" in {
            element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
          }
        }

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            element(accountRowRemove(1)).child(0).text() shouldBe removeText
          }
          s"has the correct hidden text" in {
            element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
          }
          s"has the correct link" in {
            element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }

      "there is a single untaxed account passed in that is a prior submission" which {

        lazy val result = view(untaxedYesNoForm, taxYear, Seq(
          InterestAccountModel(Some("azerty"), "Bank of UK", 9001.00)
        ), UNTAXED)(fakeRequest, messages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitleSingle)
        welshToggleCheck("English")
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)

        textOnPageCheck( "Bank of UK", accountRowName(1))

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChangePriorSubmission(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangePriorSubmissionHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChangePriorSubmission(1)).attr("href") shouldBe changePriorUntaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }

      "the radio button form is not selected" which {
        val yesNoForm =
          YesNoForm.yesNoForm("Select yes if you received untaxed interest from the UK").bind(Map("value" -> ""))
        lazy val result = view(yesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), UNTAXED)(fakeRequest, messages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        welshToggleCheck("English")
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))

        val expectedErrorText = "Select yes if you received untaxed interest from the UK"

        titleCheck(errorTitleText(untaxedTitleSingle))
        errorSummaryCheck(expectedErrorText, "#value")
        errorAboveElementCheck(expectedErrorText)

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChange(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
          }
        }

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            element(accountRowRemove(1)).child(0).text() shouldBe removeText
          }
          s"has the correct hidden text" in {
            element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
          }
          s"has the correct" in {
            element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }
    }

    "render with 2 rows" when {

      "there are two accounts passed in, one new account and one prior" which {

        lazy val result = view(untaxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9000.01, Some("qwerty")),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), UNTAXED)(fakeRequest, messages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitlePlural)
        welshToggleCheck("English")
        h1Check(untaxedH1Plural)
        textOnPageCheck(captionText, captionSelector)

        "have an area for the first row" which {
          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
            }
            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            s"has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
            }
            s"has the correct link" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
            }
          }
        }

        "have an area for the second row" which {
          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChangePriorSubmission(2)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangePriorSubmissionHidden(2)).text shouldBe s"$changeText Bank of EU account details"
            }
            "has the correct link" in {
              element(accountRowChangePriorSubmission(2)).attr("href") shouldBe changePriorUntaxedHref
            }
          }
        }
        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }
    }
  }

  "InterestAccountsView when taxed in English" should {

    "render with 1 row" when {

      "there is a single taxed account passed in that is not a prior submission" which {

        lazy val result = view(taxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), TAXED)(fakeRequest, messages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitleSingle)
        welshToggleCheck("English")
        textOnPageCheck(captionText, captionSelector)
        h1Check(taxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChange(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChange(1)).attr("href") shouldBe changeTaxedHref
          }
        }

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            element(accountRowRemove(1)).child(0).text() shouldBe removeText
          }
          s"has the correct hidden text" in {
            element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
          }
          s"has the correct link" in {
            element(accountRowRemove(1)).attr("href") shouldBe removeTaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
      }

      "there is a single taxed account passed in that is a prior submission" which {

        lazy val result = view(taxedYesNoForm, taxYear, Seq(
          InterestAccountModel(Some("azerty"), "Bank of UK", 9001.00)
        ), TAXED)(fakeRequest, messages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitleSingle)
        welshToggleCheck("English")
        textOnPageCheck(captionText, captionSelector)
        h1Check(taxedH1Singular)

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChangePriorSubmission(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangePriorSubmissionHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChangePriorSubmission(1)).attr("href") shouldBe changePriorTaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
      }
    }

    "render with 2 rows" when {

      "there are two accounts passed in, one new account and one prior" which {
        lazy val result = view(taxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9000.01, Some("qwerty")),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), TAXED)(fakeRequest, messages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitlePlural)
        welshToggleCheck("English")
        h1Check(taxedH1Plural)
        textOnPageCheck(captionText, captionSelector)

        "have an area for the first row" which {

          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
            }
            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeTaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            "has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
            }
            s"has the correct link" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeTaxedHref
            }
          }
        }

        "have an area for the second row" which {
          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChangePriorSubmission(2)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangePriorSubmissionHidden(2)).text shouldBe s"$changeText Bank of EU account details"
            }
            "has the correct link" in {
              element(accountRowChangePriorSubmission(2)).attr("href") shouldBe changePriorTaxedHref
            }
          }
        }
        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
      }
    }
  }

  "InterestAccountsView when untaxed in Welsh" should {
    "render with 1 row" when {

      "there is a single untaxed account passed in that is not a prior submission" which {
        lazy val result = view(untaxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), UNTAXED)(fakeRequest, welshMessages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitleSingle)
        welshToggleCheck("Welsh")
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChange(1)).child(0).text shouldBe changeText
          }

          "has the correct hidden text" in {
            element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }

          "has the correct link" in {
            element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
          }
        }

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            element(accountRowRemove(1)).child(0).text() shouldBe removeText
          }
          s"has the correct hidden text" in {
            element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
          }
          s"has the correct link" in {
            element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }

      "there is a single untaxed account passed in that is a prior submission" which {

        lazy val result = view(untaxedYesNoForm, taxYear, Seq(
          InterestAccountModel(Some("azerty"), "Bank of UK", 9001.00)
        ), UNTAXED)(fakeRequest, welshMessages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitleSingle)
        welshToggleCheck("Welsh")
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)

        textOnPageCheck( "Bank of UK", accountRowName(1))

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChangePriorSubmission(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangePriorSubmissionHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChangePriorSubmission(1)).attr("href") shouldBe changePriorUntaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }

      "the radio button form is not selected" which {
        val yesNoForm =
          YesNoForm.yesNoForm("Select yes if you received untaxed interest from the UK").bind(Map("value" -> ""))
        lazy val result = view(yesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), UNTAXED)(fakeRequest, welshMessages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        textOnPageCheck(captionText, captionSelector)
        h1Check(untaxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))

        val expectedErrorText = "Select yes if you received untaxed interest from the UK"

        titleCheck(errorTitleText(untaxedTitleSingle))
        errorSummaryCheck(expectedErrorText, "#value")
        errorAboveElementCheck(expectedErrorText)

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChange(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
          }
        }

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            element(accountRowRemove(1)).child(0).text() shouldBe removeText
          }
          s"has the correct hidden text" in {
            element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
          }
          s"has the correct" in {
            element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }
    }

    "render with 2 rows" when {

      "there are two accounts passed in, one new account and one prior" which {

        lazy val result = view(untaxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9000.01, Some("qwerty")),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), UNTAXED)(fakeRequest, welshMessages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(untaxedTitlePlural)
        welshToggleCheck("Welsh")
        h1Check(untaxedH1Plural)
        textOnPageCheck(captionText, captionSelector)

        "have an area for the first row" which {
          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
            }
            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            s"has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
            }
            s"has the correct link" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
            }
          }
        }

        "have an area for the second row" which {
          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChangePriorSubmission(2)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangePriorSubmissionHidden(2)).text shouldBe s"$changeText Bank of EU account details"
            }
            "has the correct link" in {
              element(accountRowChangePriorSubmission(2)).attr("href") shouldBe changePriorUntaxedHref
            }
          }
        }
        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
      }
    }
  }

  "InterestAccountsView when taxed in Welsh" should {

    "render with 1 row" when {

      "there is a single taxed account passed in that is not a prior submission" which {

        lazy val result = view(taxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9001.00, Some("qwerty"))
        ), TAXED)(fakeRequest, welshMessages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitleSingle)
        welshToggleCheck("Welsh")
        textOnPageCheck(captionText, captionSelector)
        h1Check(taxedH1Singular)
        textOnPageCheck( "Bank of UK", accountRowName(1))

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChange(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChange(1)).attr("href") shouldBe changeTaxedHref
          }
        }

        "has a link for removing the account" which {
          s"has the text $removeText" in {
            element(accountRowRemove(1)).child(0).text() shouldBe removeText
          }
          s"has the correct hidden text" in {
            element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
          }
          s"has the correct link" in {
            element(accountRowRemove(1)).attr("href") shouldBe removeTaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
      }

      "there is a single taxed account passed in that is a prior submission" which {

        lazy val result = view(taxedYesNoForm, taxYear, Seq(
          InterestAccountModel(Some("azerty"), "Bank of UK", 9001.00)
        ), TAXED)(fakeRequest, welshMessages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitleSingle)
        welshToggleCheck("Welsh")
        textOnPageCheck(captionText, captionSelector)
        h1Check(taxedH1Singular)

        "has a link for changing the account" which {
          "has the correct text" in {
            element(accountRowChangePriorSubmission(1)).child(0).text shouldBe changeText
          }
          "has the correct hidden text" in {
            element(accountRowChangePriorSubmissionHidden(1)).text shouldBe s"$changeText Bank of UK account details"
          }
          "has the correct link" in {
            element(accountRowChangePriorSubmission(1)).attr("href") shouldBe changePriorTaxedHref
          }
        }

        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
      }
    }

    "render with 2 rows" when {

      "there are two accounts passed in, one new account and one prior" which {
        lazy val result = view(taxedYesNoForm, taxYear, Seq(
          InterestAccountModel(None, "Bank of UK", 9000.01, Some("qwerty")),
          InterestAccountModel(Some("azerty"), "Bank of EU", 1234.56)
        ), TAXED)(fakeRequest, welshMessages, mockAppConfig)
        implicit val document: Document = Jsoup.parse(result.body)

        titleCheck(taxedTitlePlural)
        welshToggleCheck("Welsh")
        h1Check(taxedH1Plural)
        textOnPageCheck(captionText, captionSelector)

        "have an area for the first row" which {

          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe s"$changeText Bank of UK account details"
            }
            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeTaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            "has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe s"$removeText Bank of UK account"
            }
            s"has the correct link" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeTaxedHref
            }
          }
        }

        "have an area for the second row" which {
          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChangePriorSubmission(2)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangePriorSubmissionHidden(2)).text shouldBe s"$changeText Bank of EU account details"
            }
            "has the correct link" in {
              element(accountRowChangePriorSubmission(2)).attr("href") shouldBe changePriorTaxedHref
            }
          }
        }
        textOnPageCheck(doYouNeedText, doYouNeedSelector)
        textOnPageCheck(youMustTellText, youMustTellSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
      }
    }
  }
}
