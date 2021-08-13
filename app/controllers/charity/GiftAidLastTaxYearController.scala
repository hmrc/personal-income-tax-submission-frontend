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
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import org.slf4j
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.GiftAidLastTaxYearView

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GiftAidLastTaxYearController @Inject()(
                                              implicit val cc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              giftAidOverseasNameController: GiftAidOverseasNameController,
                                              giftAidLastTaxYearView: GiftAidLastTaxYearView,
                                              val appConfig: AppConfig,
                                              giftAidSessionService: GiftAidSessionService,
                                              errorHandler: ErrorHandler
                                            ) extends FrontendController(cc) with I18nSupport with SessionHelper with CharityJourney {

  lazy val logger: slf4j.Logger = Logger(this.getClass).logger
  
  override def handleRedirect(
                               taxYear: Int,
                               cya: GiftAidCYAModel,
                               prior: Option[GiftAidSubmissionModel],
                               fromShow: Boolean = false
                             )(implicit user: User[AnyContent]): Result = {
    lazy val page: BigDecimal => Result = priorInput => Ok(giftAidLastTaxYearView(yesNoForm(user), taxYear, priorInput))
    
    (prior, cya.overseasDonationsViaGiftAid, cya.overseasCharityNames, cya.donationsViaGiftAidAmount) match {
      case (Some(priorData), _, _, _) if priorData.giftAidPayments.flatMap(_.currentYearTreatedAsPreviousYear).isDefined =>
        redirectToCya(taxYear)
      case (_, Some(false), _, Some(totalDonations)) =>
        determineResult(page(totalDonations), Redirect(controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)), fromShow)
      case (_, Some(true), Some(names), Some(totalDonations)) if names.nonEmpty =>
        determineResult(page(totalDonations), Redirect(controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)), fromShow)
      case (_, Some(true), _, _) => giftAidOverseasNameController.handleRedirect(taxYear, cya, prior)
      case _ => redirectToOverview(taxYear)
    }
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.last-tax-year.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError)
  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ =>
          logger.warn("[GiftAidLastTaxYearController][show] Empty CYA data returned from database. Redirecting to the overview page.")
          redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { case (cya, prior) =>
      yesNoForm(user).bindFromRequest().fold(
        {
          formWithErrors =>
            cya.flatMap(_.donationsViaGiftAidAmount) match {
              case Some(donation) => Future.successful(BadRequest(giftAidLastTaxYearView(formWithErrors, taxYear, donation)))
              case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
            }
        },
        {
          yesNoForm => cya.fold(Future.successful(redirectToOverview(taxYear))) { cyaData =>
            if(prior.flatMap(_.giftAidPayments.flatMap(_.currentYearTreatedAsPreviousYear)).isDefined){
              Future.successful(redirectToCya(taxYear))
            } else {
              val updatedCya = cyaData.copy(
                addDonationToLastYear = Some(yesNoForm),
                addDonationToLastYearAmount = if(yesNoForm) cyaData.addDonationToLastYearAmount else None
              )

              val redirectLocation = (yesNoForm, updatedCya.isFinished) match {
                case (true, _) => Redirect(controllers.charity.routes.LastTaxYearAmountController.show(taxYear))
                case (_, true) => redirectToCya(taxYear)
                case _ => Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
              }

              giftAidSessionService.updateSessionData(updatedCya, taxYear)(errorHandler.internalServerError())(redirectLocation)
            }
          }
        }
      )
    }.flatten
  }

}
