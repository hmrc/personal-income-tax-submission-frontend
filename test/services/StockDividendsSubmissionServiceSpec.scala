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

package services

import audit.{AuditModel, CreateOrAmendDividendsAuditDetail}
import config.MockAuditService
import connectors.{DividendsSubmissionConnector, StockDividendsSubmissionConnector, StockDividendsUserDataConnector}
import models.dividends._
import models.priorDataModels.IncomeSourcesModel
import models.{APIErrorBodyModel, APIErrorModel, User}
import org.scalatest.PrivateMethodTester
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class StockDividendsSubmissionServiceSpec extends UnitTest with MockAuditService with PrivateMethodTester {

  val stockDividendsSubmissionConnector: StockDividendsSubmissionConnector = mock[StockDividendsSubmissionConnector]
  val stockDividendsUserDataConnector: StockDividendsUserDataConnector = mock[StockDividendsUserDataConnector]
  val dividendsSessionService: DividendsSessionService = mock[DividendsSessionService]
  val dividendsSubmissionConnector: DividendsSubmissionConnector = mock[DividendsSubmissionConnector]
  val auth: AuthConnector = mock[AuthConnector]
  val service = new StockDividendsSubmissionService(
    stockDividendsSubmissionConnector, stockDividendsUserDataConnector, dividendsSessionService, dividendsSubmissionConnector, mockAuditService
  )

  ".submitDividends" should {

    "return the connector response" when {

      val dsmData = DividendsSubmissionModel(Some(5.00), Some(10.00))

      val cyaData: StockDividendsCheckYourAnswersModel =
        StockDividendsCheckYourAnswersModel(
          gateway = None,
          ukDividends = Some(true), Some(5.00),
          otherUkDividends = Some(true), Some(10.00),
          stockDividends = Some(true), Some(10.00),
          redeemableShares = Some(true), Some(10.00),
          closeCompanyLoansWrittenOff = Some(true), Some(10.00)
        )

      val stockSubmissionModel: StockDividendsSubmissionModel = StockDividendsSubmissionModel(
        foreignDividend = None,
        dividendIncomeReceivedWhilstAbroad = None,
        stockDividend = Some(StockDividendModel(None, 10.00)),
        redeemableShares = Some(StockDividendModel(None, 10.00)),
        bonusIssuesOfSecurities = None,
        closeCompanyLoansWrittenOff = Some(StockDividendModel(None, 10.00))
      )

      val nino = "AA123456A"
      val mtdItid = "1234567890"
      val taxYear = 2020

      "Given both Submission Connectors return Right(true) when both Dividends and Stock Dividends can be submitted" in {

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(Some(StockDividendsPriorSubmission()))))

        (dividendsSessionService.getPriorData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier).returning(Future.successful(Right(IncomeSourcesModel())))

        verifyAuditEvent[CreateOrAmendDividendsAuditDetail](AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update",
          CreateOrAmendDividendsAuditDetail.createFromStockCyaData(cyaData, IncomeSourcesModel().dividends, Some(StockDividendsPriorSubmission()),
            isUpdate = false, nino, mtdItid, Individual.toString, taxYear)))

        (dividendsSubmissionConnector.submitDividends(_: DividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(dsmData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(DividendsResponseModel(NO_CONTENT))))

        (stockDividendsSubmissionConnector.submitDividends(_: StockDividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(stockSubmissionModel, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(true)))

        val result = await(service.submitDividends(cyaData, nino, taxYear))
        result.isRight shouldBe true
      }

      "Given both Submission Connectors return Right(true) when neither Dividends and Stock Dividends can be submitted" in {

        val stockCya = StockDividendsCheckYourAnswersModel()

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(Some(StockDividendsPriorSubmission()))))

        (dividendsSessionService.getPriorData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier).returning(Future.successful(Right(IncomeSourcesModel())))

        verifyAuditEvent[CreateOrAmendDividendsAuditDetail](AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update",
          CreateOrAmendDividendsAuditDetail.createFromStockCyaData(stockCya, IncomeSourcesModel().dividends, Some(StockDividendsPriorSubmission()),
            isUpdate = false, nino, mtdItid, Individual.toString, taxYear)))

        val result = await(service.submitDividends(stockCya, nino, taxYear))
        result.isRight shouldBe true
      }

      "Given Dividends Submission Connector returns Right(true) when only Dividends can be submitted" in {

        val stockCya = StockDividendsCheckYourAnswersModel(
          gateway = Some(true),
          ukDividends = Some(true), ukDividendsAmount = Some(5.00),
          otherUkDividends = Some(true), otherUkDividendsAmount = Some(10.00)
        )

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(Some(StockDividendsPriorSubmission()))))

        (dividendsSessionService.getPriorData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier).returning(Future.successful(Right(IncomeSourcesModel())))

        verifyAuditEvent[CreateOrAmendDividendsAuditDetail](AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update",
          CreateOrAmendDividendsAuditDetail.createFromStockCyaData(stockCya, IncomeSourcesModel().dividends, Some(StockDividendsPriorSubmission()),
            isUpdate = false, nino, mtdItid, Individual.toString, taxYear)))

        (dividendsSubmissionConnector.submitDividends(_: DividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(dsmData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(DividendsResponseModel(NO_CONTENT))))

        val result = await(service.submitDividends(stockCya, nino, taxYear))
        result.isRight shouldBe true
      }

      "Given Stock Dividends Submission Connector returns Right(true) when only Stock Dividends can be submitted" in {

        val stockCya = StockDividendsCheckYourAnswersModel(
          gateway = Some(true),
          stockDividendsAmount = Some(10.00),
          redeemableSharesAmount = Some(10.00),
          closeCompanyLoansWrittenOffAmount = Some(10.00)
        )

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(Some(StockDividendsPriorSubmission()))))

        (dividendsSessionService.getPriorData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier).returning(Future.successful(Right(IncomeSourcesModel())))

        verifyAuditEvent[CreateOrAmendDividendsAuditDetail](AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update",
          CreateOrAmendDividendsAuditDetail.createFromStockCyaData(stockCya, IncomeSourcesModel().dividends, Some(StockDividendsPriorSubmission()),
            isUpdate = false, nino, mtdItid, Individual.toString, taxYear)))

        (stockDividendsSubmissionConnector.submitDividends(_: StockDividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(stockSubmissionModel, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(true)))

        val result = await(service.submitDividends(stockCya, nino, taxYear))
        result.isRight shouldBe true
      }

      "Given Stock Dividends User Data connector returns Left with an error" in {

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("test", "test")))))

        val result = await(service.submitDividends(cyaData, nino, taxYear))
        result.isLeft shouldBe true
      }

      "Given Dividends Session Service Connector returns Left with an error" in {

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(Some(StockDividendsPriorSubmission()))))

        (dividendsSessionService.getPriorData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier)
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("test", "test")))))

        val result = await(service.submitDividends(cyaData, nino, taxYear))
        result.isLeft shouldBe true
      }

      "Given Dividends Submission Connector returns a Left" in {

        val stockCya = StockDividendsCheckYourAnswersModel(
          gateway = Some(true),
          ukDividendsAmount = Some(5.00),
          otherUkDividendsAmount = Some(10.00)
        )

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(Some(StockDividendsPriorSubmission()))))

        (dividendsSessionService.getPriorData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier)
          .returning(Future.successful(Right(IncomeSourcesModel())))

        verifyAuditEvent[CreateOrAmendDividendsAuditDetail](AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update",
          CreateOrAmendDividendsAuditDetail.createFromStockCyaData(stockCya, IncomeSourcesModel().dividends, Some(StockDividendsPriorSubmission()),
            isUpdate = false, nino, mtdItid, Individual.toString, taxYear)))

        (dividendsSubmissionConnector.submitDividends(_: DividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(dsmData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("test", "test")))))

        val result = await(service.submitDividends(stockCya, nino, taxYear))
        result.isLeft shouldBe true
      }

      "Given Stock Dividends Submission Connector returns a Left" in {

        val stockCya = StockDividendsCheckYourAnswersModel(
          gateway = Some(true),
          stockDividendsAmount = Some(10.00),
          redeemableSharesAmount = Some(10.00),
          closeCompanyLoansWrittenOffAmount = Some(10.00),
        )

        (stockDividendsUserDataConnector.getUserData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Right(Some(StockDividendsPriorSubmission()))))

        (dividendsSessionService.getPriorData(_: Int)(_: User[_], _: HeaderCarrier))
          .expects(taxYear, user, emptyHeaderCarrier)
          .returning(Future.successful(Right(IncomeSourcesModel())))

        verifyAuditEvent[CreateOrAmendDividendsAuditDetail](AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update",
          CreateOrAmendDividendsAuditDetail.createFromStockCyaData(stockCya, IncomeSourcesModel().dividends, Some(StockDividendsPriorSubmission()),
            isUpdate = false, nino, mtdItid, Individual.toString, taxYear)))

        (stockDividendsSubmissionConnector.submitDividends(_: StockDividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(stockSubmissionModel, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid" -> mtdItid))
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("test", "test")))))

        val result = await(service.submitDividends(stockCya, nino, taxYear))
        result.isLeft shouldBe true
      }
    }
  }

  ".checkUpdatedValues" should {

    implicit lazy val app: Application = new GuiceApplicationBuilder()
      .in(Environment.simple(mode = Mode.Dev))
      .build()

    lazy val controller = app.injector.instanceOf[StockDividendsSubmissionService]

    val privateCheckUpdatedValues = PrivateMethod[Boolean](Symbol("checkUpdatedValues"))

    "return true" when {

      "both cyaData and priorData have different values" in {

        val updatedGrossAmount: Option[BigDecimal]  = Some(10.00)
        val priorGrossAmount: Option[BigDecimal] = Some(20.00)

        controller invokePrivate privateCheckUpdatedValues(updatedGrossAmount, priorGrossAmount) shouldBe true
      }

      "cyaData has a value and priorData is not present" in {
        val updatedGrossAmount: Option[BigDecimal]  = Some(10.00)
        val priorGrossAmount: Option[BigDecimal] = None

        controller invokePrivate privateCheckUpdatedValues(updatedGrossAmount, priorGrossAmount) shouldBe true
      }

      "priorData has a value and cyaAmount is not present" in {
        val updatedGrossAmount: Option[BigDecimal] = None
        val priorGrossAmount: Option[BigDecimal] = Some(10.00)

        controller invokePrivate privateCheckUpdatedValues(updatedGrossAmount, priorGrossAmount) shouldBe true
      }
    }

    "return false" when {

      "both cyaData and priorData have the same values" in {
        val updatedGrossAmount: Option[BigDecimal]  = Some(10.00)
        val priorGrossAmount: Option[BigDecimal] = Some(10.00)

        controller invokePrivate privateCheckUpdatedValues(updatedGrossAmount, priorGrossAmount) shouldBe false
      }

    }
  }
}
