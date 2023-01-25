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
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.DonationsToPreviousTaxYearView

import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class DonationsToPreviousTaxYearController @Inject()(
                                                      implicit cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      donationsToPreviousTaxYearView: DonationsToPreviousTaxYearView,
                                                      giftAidSessionService: GiftAidSessionService,
                                                      giftAidLastTaxYearController: GiftAidLastTaxYearController,
                                                      giftAidLastTaxYearAmountController: GiftAidLastTaxYearAmountController,
                                                      errorHandler: ErrorHandler,
                                                      appConfig: AppConfig,
                                                      ec: ExecutionContext
                                                    ) extends FrontendController(cc) with I18nSupport with SessionHelper with CharityJourney {

  override def handleRedirect(
                               taxYear: Int,
                               cya: GiftAidCYAModel,
                               prior: Option[GiftAidSubmissionModel],
                               fromShow: Boolean = false
                             )(implicit user: User[AnyContent]): Result = {

    val prefillForm = cya.addDonationToThisYear.fold(yesNoForm(user, taxYear))(yesNoForm(user, taxYear).fill)

    (prior, cya.donationsViaGiftAid, cya.addDonationToLastYear, cya.addDonationToLastYearAmount) match {
      case (Some(priorData), _, _, _) if priorData.giftAidPayments.flatMap(_.nextYearTreatedAsCurrentYear).isDefined =>
        redirectToCya(taxYear)
      case (_, Some(true), Some(true), None) => giftAidLastTaxYearAmountController.handleRedirect(taxYear, cya, prior)
      case (_, Some(true), None, _) => giftAidLastTaxYearController.handleRedirect(taxYear, cya, prior)
      case (_, None, _, _) => Redirect(controllers.charity.routes.GiftAidDonationsController.show(taxYear))
      case _ =>
        determineResult(
          Ok(donationsToPreviousTaxYearView(prefillForm, taxYear)),
          Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear)),
          fromShow
        )
    }
  }

  val yesNoForm: (User[AnyContent], Int) => Form[Boolean] = (user, taxYear) => {
    val missingInputError = s"charity.add-to-current-tax-year.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError, Seq(taxYear.toString))
  }

  def show(taxYear: Int, otherTaxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    if (taxYear != otherTaxYear) {
      Future.successful(Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear)))
    } else {
      giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { case (cya, prior) =>
        cya.fold(
          redirectToOverview(taxYear)
        ) {
          cyaData => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        }
      }
    }
  }

  def submit(taxYear: Int, otherTaxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>
    if (taxYear != otherTaxYear) {
      Future.successful(Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear)))
    } else {
      yesNoForm(user, taxYear).bindFromRequest().fold(
        {
          formWithErrors =>
            Future.successful(BadRequest(donationsToPreviousTaxYearView(formWithErrors, taxYear)))
        },
        {
          yesNoForm =>
            giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { case (cya, prior) =>
              if (prior.flatMap(_.giftAidPayments.flatMap(_.nextYearTreatedAsCurrentYear)).isDefined) {
                Future.successful(redirectToCya(taxYear))
              } else {
                cya.fold(Future.successful(redirectToOverview(taxYear))) { cyaData =>
                  val updatedCya = cyaData.copy(
                    addDonationToThisYear = Some(yesNoForm),
                    addDonationToThisYearAmount = if (yesNoForm) cyaData.addDonationToThisYearAmount else None
                  )

                  val redirectLocation = (yesNoForm, updatedCya.isFinished) match {
                    case (true, _) => Redirect(controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear))
                    case (_, true) => redirectToCya(taxYear)
                    case _ => Redirect(controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear))
                  }

                  giftAidSessionService.updateSessionData(updatedCya, taxYear)(errorHandler.internalServerError())(redirectLocation)
                }
              }
            }.flatten
        }
      )
    }
  }
}
