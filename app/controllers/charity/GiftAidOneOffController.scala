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

import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.User
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.GiftAidOneOffView
import javax.inject.Inject
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import services.GiftAidSessionService

import scala.concurrent.{ExecutionContext, Future}

class GiftAidOneOffController @Inject()(
                                              implicit val cc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              giftAidDonatedAmountController: GiftAidDonatedAmountController,
                                              giftAidOneOffView: GiftAidOneOffView,
                                              giftAidSessionService: GiftAidSessionService,
                                              errorHandler: ErrorHandler,
                                              implicit val appConfig: AppConfig
                                            ) extends FrontendController(cc) with I18nSupport with SessionHelper with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    (prior, cya.donationsViaGiftAidAmount) match {
      case (Some(priorData), _) if priorData.giftAidPayments.map(_.oneOffCurrentYear).isDefined =>
        Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
      case (_, Some(amount)) => determineResult(
        Ok(giftAidOneOffView(yesNoForm(user), taxYear, giftAidDonations = amount)),
        Redirect(controllers.charity.routes.GiftAidOneOffController.show(taxYear)),
        fromShow)
      case _ => giftAidDonatedAmountController.handleRedirect(taxYear, cya, prior)
    }
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.one-off.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError)
  }

  implicit val executionContext: ExecutionContext = cc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

    giftAidSessionService.getSessionData(taxYear).map {
        case Some(cyaData) =>
          val previousAmount: Option[BigDecimal] = cyaData.giftAid.flatMap(_.donationsViaGiftAidAmount)

          yesNoForm(user).bindFromRequest().fold({
            formWithErrors =>
              previousAmount match {
                case Some(amount) => Future.successful(BadRequest(giftAidOneOffView(formWithErrors, taxYear, amount)))
                case _ => Future.successful(errorHandler.internalServerError())
              }
          }, {
            success =>
              val redirectLocation = if(success){
                controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear)
              } else {
                controllers.charity.routes.OverseasGiftAidDonationsController.show(taxYear)
              }
              cyaData.giftAid.fold{
                Future.successful(redirectToOverview(taxYear))
              } {
                cyaModel => giftAidSessionService.updateSessionData(cyaModel.copy(oneOffDonationsViaGiftAid = Some(success)), taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(
                  Redirect(redirectLocation)
                )
              }
          }

          )
        case _ =>
          logger.info("[GiftAidOneOffController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }.flatten
  }

}
