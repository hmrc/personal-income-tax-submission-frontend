package controllers.interest

import common.SessionValues
import config.AppConfig
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, UNAUTHORIZED}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import utils.IntegrationTest
import views.html.interest.ChangeAccountAmountView

import scala.concurrent.Future

class ChangeAccountAmountControllerTest extends IntegrationTest{

  lazy val frontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]
  def controller(stubbedRetrieval: Future[_], acceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq()): ChangeAccountAmountController = {
    new ChangeAccountAmountController(
      mcc,
      authAction(stubbedRetrieval, acceptedConfidenceLevels),
      app.injector.instanceOf[ChangeAccountAmountView],
      frontendAppConfig
    )
  }

  "Hitting the show endpoint" should {

    s"return an OK ($OK)" when {

      "all auth requirements are met" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val priorSub = Json.arr(
          Json.obj(
            "accountName" -> "Taxed Account",
            "incomeSourceId" -> "TaxedId",
            "taxedUkInterest" -> 25
          )
        )
        val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
          Enrolments(Set(
            Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
            Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
          )),
          Some(AffinityGroup.Individual)
        ))

        val result = await(controller(retrieval).show(2021, "taxed", "TaxedId")
        (FakeRequest().withSession(
          (SessionValues.INTEREST_CYA, Json.prettyPrint(Json.toJson(interestCYA))),
          (SessionValues.INTEREST_PRIOR_SUB, priorSub.toString()))
        ))

        result.header.status shouldBe OK
      }
    }

    s"return an UNAUTHORISED ($UNAUTHORIZED)" when {

      "the confidence level is too low" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val priorSub = Json.arr(
          Json.obj(
            "accountName" -> "Taxed Account",
            "incomeSourceId" -> "TaxedId",
            "taxedUkInterest" -> 25
          )
        )
        val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
          Enrolments(Set(
            Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
            Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
          )),
          Some(AffinityGroup.Individual)
        ))

        val result = await(controller(retrieval, Seq(ConfidenceLevel.L500)).show(2021, "taxed", "TaxedId")
        (FakeRequest().withSession(
          (SessionValues.INTEREST_CYA, Json.prettyPrint(Json.toJson(interestCYA))),
          (SessionValues.INTEREST_PRIOR_SUB, priorSub.toString()))
        ))

        result.header.status shouldBe UNAUTHORIZED
      }

      "it contains the wrong credentials" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val priorSub = Json.arr(
          Json.obj(
            "accountName" -> "Taxed Account",
            "incomeSourceId" -> "TaxedId",
            "taxedUkInterest" -> 25
          )
        )
        val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
          Enrolments(Set(
            Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated", None),
            Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
          )),
          Some(AffinityGroup.Individual)
        ))

        val result = await(controller(retrieval).show(2021, "taxed", "TaxedId")
        (FakeRequest().withSession(
          (SessionValues.INTEREST_CYA, Json.prettyPrint(Json.toJson(interestCYA))),
          (SessionValues.INTEREST_PRIOR_SUB, priorSub.toString()))
        ))

        result.header.status shouldBe UNAUTHORIZED
      }

    }

  }

}
