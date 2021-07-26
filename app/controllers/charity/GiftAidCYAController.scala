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

package controllers.charity

import audit.{AuditModel, AuditService, CreateOrAmendGiftAidAuditDetail}
import common.SessionValues
import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSubmissionService
import services.GiftAidSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.GiftAidCYAView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidCYAController @Inject()(
                                      implicit mcc: MessagesControllerComponents,
                                      authorisedAction: AuthorisedAction,
                                      auditService: AuditService,
                                      appConfig: AppConfig,
                                      view: GiftAidCYAView,
                                      giftAidSubmissionService: GiftAidSubmissionService,
                                      errorHandler: ErrorHandler,
                                      giftAidSessionService: GiftAidSessionService
                                    ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  val fakeCyaDataMax: Option[GiftAidCYAModel] = Some(GiftAidCYAModel(
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgium Trust", "American Trust")),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(true), Some(100.00), Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgium Trust", "American Trust"))
  ))

  val fakeCyaDataMin: Option[GiftAidCYAModel] = Some(GiftAidCYAModel(
    donationsViaGiftAid = Some(false),
    oneOffDonationsViaGiftAid = Some(false),
    overseasDonationsViaGiftAid = Some(false),
    addDonationToLastYear = Some(false),
    addDonationToThisYear = Some(false),
    donatedSharesSecuritiesLandOrProperty = Some(false),
    overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
  ))

  val fakeCyaDataUnfinished: Option[GiftAidCYAModel] = Some(GiftAidCYAModel(
    donationsViaGiftAid = Some(false),
    oneOffDonationsViaGiftAid = Some(false),
    overseasDonationsViaGiftAid = Some(false),
    addDonationToLastYear = None,
    addDonationToThisYear = Some(false),
    donatedSharesSecuritiesLandOrProperty = Some(false),
    overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
  ))

  val fakePriorData: Option[GiftAidSubmissionModel] = Some(GiftAidSubmissionModel(
    Some(GiftAidPaymentsModel(
      Some(123.55), Some(List("Trust 1", "Trust 2")), Some(103.4), Some(154.78), Some(983.92), Some(200.00)
    )),
    Some(GiftsModel(
      Some(98765.32), Some(List("Trust 3", "Trust 4")), Some(142.9), Some(123.44)
    ))
  ))

  val fakePriorDataMin: Option[GiftAidSubmissionModel] = Some(GiftAidSubmissionModel())

  val fakePriorDataAlmostMin: Option[GiftAidSubmissionModel] = Some(GiftAidSubmissionModel(
    gifts = Some(GiftsModel(
      landAndBuildings = Some(100.34)
    ))
  ))

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle[Result](taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      (cya, prior) match {
        case (Some(cyaData), potentialPriorData) =>
          if (cyaData.isFinished) {
            Ok(view(taxYear, cyaData, potentialPriorData))
          } else {
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)) //TODO This should redirect to the last logical position. Sort out during navigation.
          }
        case (None, Some(priorData)) =>
          val cyaModel = GiftAidCYAModel(
            Some(priorData.giftAidPayments.exists(_.currentYear.nonEmpty)),
            priorData.giftAidPayments.flatMap(_.currentYear),
            Some(priorData.giftAidPayments.exists(_.oneOffCurrentYear.nonEmpty)),
            priorData.giftAidPayments.flatMap(_.oneOffCurrentYear),
            Some(priorData.giftAidPayments.exists(_.nonUkCharities.nonEmpty)),
            priorData.giftAidPayments.flatMap(_.nonUkCharities),
            priorData.giftAidPayments.flatMap(_.nonUkCharitiesCharityNames.map(_.toSeq)),
            Some(priorData.giftAidPayments.exists(_.currentYearTreatedAsPreviousYear.nonEmpty)),
            priorData.giftAidPayments.flatMap(_.currentYearTreatedAsPreviousYear),
            Some(priorData.giftAidPayments.exists(_.nextYearTreatedAsCurrentYear.nonEmpty)),
            priorData.giftAidPayments.flatMap(_.nextYearTreatedAsCurrentYear),
            Some(priorData.gifts.exists(_.sharesOrSecurities.nonEmpty) || priorData.gifts.exists(_.landAndBuildings.nonEmpty)),
            Some(priorData.gifts.exists(_.sharesOrSecurities.nonEmpty)),
            priorData.gifts.flatMap(_.sharesOrSecurities),
            Some(priorData.gifts.exists(_.landAndBuildings.nonEmpty)),
            priorData.gifts.flatMap(_.landAndBuildings),
            Some(priorData.gifts.exists(_.investmentsNonUkCharities.nonEmpty)),
            priorData.gifts.flatMap(_.investmentsNonUkCharities),
            priorData.gifts.flatMap(_.investmentsNonUkCharitiesCharityNames.map(_.toSeq))
          )

          Ok(view(taxYear, cyaModel, Some(priorData)))
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>
        val submissionModel = GiftAidSubmissionModel(
          Some(GiftAidPaymentsModel(
            model.overseasDonationsViaGiftAidAmount, model.overseasCharityNames.map(_.toList),
            model.donationsViaGiftAidAmount,
            model.addDonationToLastYearAmount,
            model.addDonationToThisYearAmount,
            model.oneOffDonationsViaGiftAidAmount
          )),
          Some(GiftsModel(
            model.overseasDonatedSharesSecuritiesLandOrPropertyAmount, model.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.map(_.toList),
            model.donatedSharesOrSecuritiesAmount, model.donatedLandOrPropertyAmount
          ))
        )

        giftAidSubmissionService.submitGiftAid(Some(submissionModel), user.nino, user.mtditid, taxYear).map {
          case Right(_) =>
            auditSubmission(CreateOrAmendGiftAidAuditDetail(
              prior, Some(submissionModel), prior.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase(), taxYear)
            )
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          case Left(error) => errorHandler.handleError(error.status)
        }
      }
    }.flatten
  }

  private def auditSubmission(details: CreateOrAmendGiftAidAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendGiftAidUpdate", "CreateOrAmendGiftAidUpdate", details)
    auditService.auditModel(event)
  }

}
