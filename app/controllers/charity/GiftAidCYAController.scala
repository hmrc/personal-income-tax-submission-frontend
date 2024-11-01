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

package controllers.charity

import audit._
import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.charity.{CharityNameModel, GiftAidCYAModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ExcludeJourneyService, GiftAidSessionService, GiftAidSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.GiftAidCYAView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import controllers.routes
import models.User

class GiftAidCYAController @Inject()(implicit mcc: MessagesControllerComponents,
                                      authorisedAction: AuthorisedAction,
                                      auditService: AuditService,
                                      appConfig: AppConfig,
                                      view: GiftAidCYAView,
                                      giftAidSubmissionService: GiftAidSubmissionService,
                                      errorHandler: ErrorHandler,
                                      giftAidSessionService: GiftAidSessionService,
                                      excludeJourneyService: ExcludeJourneyService
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
            Future.successful(Redirect(controllers.charity.routes.GiftAidGatewayController.show(taxYear)))
          }
        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          giftAidSessionService.createSessionData(cyaModel, taxYear)(errorHandler.internalServerError())(Ok(view(taxYear, cyaModel, Some(priorData))))
        case _ =>
          logger.info("[GiftAidCYAController][show] No CYA and prior data. Redirecting to the overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }.flatten
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user: User[AnyContent] =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      cya.fold {
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } { model =>
        handleCharityTailoring(model, taxYear)

        val submissionModel = createNewSubmissionModel(model)
        val hasDataChanges = comparePriorData(model, prior)

        if (hasDataChanges) {
          submitWithAudit(submissionModel, prior, taxYear)
        } else {
          logger.info("[GiftAidCYAController][submit] User has not made Gift Aid Data Changes, Not submitting data to DES.")
          auditSubmission(CreateOrAmendGiftAidAuditDetail(prior,
            Some(submissionModel),
            prior.isDefined,
            user.nino,
            user.mtditid, user.affinityGroup.toLowerCase, taxYear))
          clearSessionWithRedirect(taxYear)
        }
      }
    }.flatten
  }

  private def handleCharityTailoring(model: GiftAidCYAModel, taxYear: Int)(implicit user: User[AnyContent]): Unit = {
    if (appConfig.charityTailoringEnabled && model.gateway == Some(false)) {
      auditTailorRemoveIncomeSources(TailorRemoveIncomeSourcesAuditDetail(
        nino = user.nino,
        mtditid = user.mtditid,
        userType = user.affinityGroup.toLowerCase,
        taxYear = taxYear,
        body = TailorRemoveIncomeSourcesBody(Seq(GIFT_AID.stringify))
      ))

      excludeJourneyService.excludeJourney(GIFT_AID.stringify, taxYear, user.nino).recover {
        case ex: Exception =>
          //TODO revisit me in future, as in current code this scenario was ignored, at the moment added logging
          logger.error(s"Failed to exclude journey for user ${user.nino} for tax year $taxYear", ex)
      }
    }
  }

  private def submitWithAudit(submissionModel: GiftAidSubmissionModel, prior: Option[GiftAidSubmissionModel], taxYear: Int)
                             (implicit user: User[AnyContent]): Future[Result] = {
    logger.info("[GiftAidCYAController][submit] User has made Gift Aid Data Changes, Submitting data to DES.")
    giftAidSubmissionService.submitGiftAid(Some(submissionModel), user.nino, user.mtditid, taxYear).flatMap {
      case Right(_) =>
        auditSubmission(CreateOrAmendGiftAidAuditDetail(prior,
          Some(submissionModel),
          prior.isDefined,
          user.nino,
          user.mtditid,
          user.affinityGroup.toLowerCase,
          taxYear))

        clearSessionWithRedirect(taxYear)
      case Left(error) =>
        Future.successful(errorHandler.handleError(error.status))
    }
  }

  private def clearSessionWithRedirect(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    giftAidSessionService.clear(taxYear)(errorHandler.internalServerError()) {
      redirect(appConfig.sectionCompletedQuestionEnabled,taxYear)
    }
  }

  private def createNewSubmissionModel(model: GiftAidCYAModel): GiftAidSubmissionModel ={
    GiftAidSubmissionModel(
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
  }

  private def auditSubmission(details: CreateOrAmendGiftAidAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendGiftAidUpdate", "CreateOrAmendGiftAidUpdate", details)
    auditService.auditModel(event)
  }

  private def auditTailorRemoveIncomeSources(details: TailorRemoveIncomeSourcesAuditDetail)
                                            (implicit hc: HeaderCarrier,
                                             executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("TailorRemoveIncomeSources", "tailorRemoveIncomeSources", details)
    auditService.auditModel(event)
  }

  private def generateCyaFromPrior(prior: GiftAidSubmissionModel): GiftAidCYAModel = {
    GiftAidCYAModel(
      Some(true),
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
      case Some(prior) =>
        val cyaFromPrior = generateCyaFromPrior(prior)

        val comparisonCyaData = cyaData.copy(
          overseasCharityNames = cyaData.overseasCharityNames.map(names => names.copy(id = "")),
          overseasDonatedSharesSecuritiesLandOrPropertyCharityNames =
            cyaData.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.map(names => names.copy(id = ""))
        )

        val comparisonCyaFromPrior = cyaFromPrior.copy(
          overseasCharityNames = cyaFromPrior.overseasCharityNames.map(names => names.copy(id = "")),
          overseasDonatedSharesSecuritiesLandOrPropertyCharityNames =
            cyaFromPrior.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.map(names => names.copy(id = ""))
        )
        !comparisonCyaData.equals(comparisonCyaFromPrior)
    }
  }

  private def redirect(completedSectionQuestionEnabled: Boolean, taxYear: Int): Result =
    if (completedSectionQuestionEnabled) {
      Redirect(routes.SectionCompletedStateController.show(taxYear,"gift-aid"))
    } else {
      Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

}
