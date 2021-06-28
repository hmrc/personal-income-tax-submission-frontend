

package controllers.interest

import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class InterestAccountAmountViewISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper {

  val taxYear: Int = 2022
  val UNTAXED: String = "untaxed"
  val TAXED: String = "taxed"

  val interestAccountAmountUrl: String = s"$appUrl/$taxYear/interest/how-much-untaxed-uk-interest"

  object Selectors {

  }

  trait SpecificExpectedResults {
    def expectedTitle(taxType: String): String
    def expectedHeading(taxType: String): String
    def expectedErrorTitle(taxType: String): String
    def expectedErrorNoEntry(taxType: String): String
    def expectedErrorInvalid(taxType: String): String
    def expectedErrorOverMax(taxType: String): String

  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedHintText: String
    val poundPrefixText: String
    val continueText: String

  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    def expectedTitle(taxType: String): String = s"How much $taxType UK interest did you get"
    def expectedHeading(taxType: String): String = s"How much $taxType UK interest did you get from Halifax"
    def expectedErrorTitle(taxType: String): String = s"Error: ${expectedTitle(taxType)}"
    def expectedErrorNoEntry(taxType: String): String = s"Enter the amount of $taxType UK interest you got"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = "Interest for 6 April 2021 to 5 April 2022"
    val expectedHintText = "For example, £600 or £193.54"
    val poundPrefixText = "£"
    val continueText = "Continue"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

}
