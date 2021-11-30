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
import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.charity.{CharityNameModel, DecodedGiftAidSubmissionPayload, GiftAidCYAModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{GiftAidSessionService, GiftAidSubmissionService, NrsService}
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
                                      giftAidSessionService: GiftAidSessionService,
                                      nrsService: NrsService
                                    ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  lazy val logger: Logger = Logger(this.getClass.getName)

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      (cya, prior) match {
        case (Some(cyaData), potentialPriorData) =>
          if (cyaData.isFinished) {
            Future.successful(Ok(view(taxYear, cyaData, potentialPriorData)))
          } else {
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))) //TODO This should redirect to the last logical position. Sort out during navigation.
          }
        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          giftAidSessionService.createSessionData(cyaModel, taxYear)(errorHandler.internalServerError())(Ok(view(taxYear, cyaModel, Some(priorData))))
        case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }.flatten
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>
        val submissionModel = GiftAidSubmissionModel(
          Some(GiftAidPaymentsModel(
            model.overseasDonationsViaGiftAidAmount,
            if (model.overseasCharityNames.isEmpty) None else Some(model.overseasCharityNames.map(_.name).toList),
            model.donationsViaGiftAidAmount,
            model.addDonationToLastYearAmount,
            model.addDonationToThisYearAmount,
            model.oneOffDonationsViaGiftAidAmount
          )),
          Some(GiftsModel(
            model.overseasDonatedSharesSecuritiesLandOrPropertyAmount,
            if (model.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.isEmpty) {
              None
            } else {
              Some(model.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.toList.map(_.name))
            },
            model.donatedSharesOrSecuritiesAmount, model.donatedLandOrPropertyAmount
          ))
        )

        if (comparePriorData(model, prior)) {
          logger.info("[GiftAidCYAController][submit] User has made Gift Aid Data Changes, " +
            "Submitting data to DES.")
          giftAidSubmissionService.submitGiftAid(Some(submissionModel), user.nino, user.mtditid, taxYear).flatMap {
            case Right(_) =>
              auditSubmission(CreateOrAmendGiftAidAuditDetail(
                prior, Some(submissionModel), prior.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase(), taxYear)
              )

              if (appConfig.nrsEnabled) {
                nrsService.submit(user.nino, new DecodedGiftAidSubmissionPayload(prior, Some(submissionModel)), user.mtditid)
              }

              giftAidSessionService.clear(taxYear)(errorHandler.internalServerError())(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
            case Left(error) => Future.successful(errorHandler.handleError(error.status))
          }
        } else {
          logger.info("[GiftAidCYAController][submit] User has not made Gift Aid Data Changes, " +
            "Not submitting data to DES.")
          auditSubmission(CreateOrAmendGiftAidAuditDetail(
            prior, Some(submissionModel), prior.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase(), taxYear)
          )
          giftAidSessionService.clear(taxYear)(errorHandler.internalServerError())(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
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

  private def generateCyaFromPrior(prior: GiftAidSubmissionModel): GiftAidCYAModel = {
    GiftAidCYAModel(
      Some(prior.giftAidPayments.exists(_.currentYear.nonEmpty)),
      prior.giftAidPayments.flatMap(_.currentYear),
      Some(prior.giftAidPayments.exists(_.oneOffCurrentYear.nonEmpty)),
      prior.giftAidPayments.flatMap(_.oneOffCurrentYear),
      Some(prior.giftAidPayments.exists(_.nonUkCharities.nonEmpty)),
      prior.giftAidPayments.flatMap(_.nonUkCharities),
      prior.giftAidPayments.flatMap(_.nonUkCharitiesCharityNames.map(_.map(CharityNameModel(_)))).getOrElse(Seq.empty),
      Some(prior.giftAidPayments.exists(_.currentYearTreatedAsPreviousYear.nonEmpty)),
      prior.giftAidPayments.flatMap(_.currentYearTreatedAsPreviousYear),
      Some(prior.giftAidPayments.exists(_.nextYearTreatedAsCurrentYear.nonEmpty)),
      prior.giftAidPayments.flatMap(_.nextYearTreatedAsCurrentYear),
      Some(prior.gifts.exists(_.sharesOrSecurities.nonEmpty)),
      prior.gifts.flatMap(_.sharesOrSecurities),
      Some(prior.gifts.exists(_.landAndBuildings.nonEmpty)),
      prior.gifts.flatMap(_.landAndBuildings),
      Some(prior.gifts.exists(_.investmentsNonUkCharities.nonEmpty)),
      prior.gifts.flatMap(_.investmentsNonUkCharities),
      prior.gifts.flatMap(_.investmentsNonUkCharitiesCharityNames.map(_.map(CharityNameModel(_)))).getOrElse(Seq.empty)
    )
  }

  private def comparePriorData(cyaData: GiftAidCYAModel, priorData: Option[GiftAidSubmissionModel]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

}
