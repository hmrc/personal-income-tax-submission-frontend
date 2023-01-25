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

import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.OverseasGiftAidDonationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OverseasGiftAidDonationsController @Inject()(
                                                    implicit val cc: MessagesControllerComponents,
                                                    giftAidOneOffController: GiftAidOneOffController,
                                                    giftAidOneOffAmountController: GiftAidOneOffAmountController,
                                                    authAction: AuthorisedAction,
                                                    overseasGiftAidDonationView: OverseasGiftAidDonationView,
                                                    giftAidSessionService: GiftAidSessionService,
                                                    errorHandler: ErrorHandler,
                                                    ec: ExecutionContext,
                                                    implicit val appConfig: AppConfig
                                                  ) extends FrontendController(cc) with I18nSupport with SessionHelper with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    (prior, cya.oneOffDonationsViaGiftAid) match {
      case (Some(priorData), _) if priorData.giftAidPayments.map(_.nonUkCharities).isDefined =>
        Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
      case (_, Some(true)) if cya.oneOffDonationsViaGiftAidAmount.isEmpty => giftAidOneOffAmountController.handleRedirect(taxYear, cya, prior)
      case (_, Some(_)) =>

        val prefillForm = cya.overseasDonationsViaGiftAid.fold(yesNoForm(user))(yesNoForm(user).fill)
        determineResult(
          Ok(overseasGiftAidDonationView(prefillForm, taxYear)),
          Redirect(controllers.charity.routes.OverseasGiftAidDonationsController.show(taxYear)),
          fromShow)
      case _ => giftAidOneOffController.handleRedirect(taxYear, cya, prior)
    }
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.overseas-gift-aid.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError)
  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

    yesNoForm(user).bindFromRequest().fold({
      formWithErrors =>
        Future.successful(BadRequest(overseasGiftAidDonationView(formWithErrors, taxYear)))
    }, {
      formAnswer =>
        giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { case (cya, prior) =>
          if (prior.flatMap(_.giftAidPayments.flatMap(_.nonUkCharities)).isDefined) {
            Future.successful(redirectToCya(taxYear))
          } else {
            cya.fold(Future.successful(redirectToOverview(taxYear))) { cyaData =>

              val updatedCya = {
                val updatedModel = cyaData.copy(overseasDonationsViaGiftAid = Some(formAnswer))
                if (formAnswer) {
                  updatedModel
                } else {
                  updatedModel.copy(overseasDonationsViaGiftAidAmount = None, overseasCharityNames = Seq.empty)
                }
              }

              val redirectLocation = (formAnswer, updatedCya.isFinished) match {
                case (true, _) => Redirect(controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear))
                case (_, true) => redirectToCya(taxYear)
                case _ => Redirect(controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear))
              }

              giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                InternalServerError(errorHandler.internalServerErrorTemplate)
              )(redirectLocation)
            }
          }
        }.flatten
    })
  }
}
