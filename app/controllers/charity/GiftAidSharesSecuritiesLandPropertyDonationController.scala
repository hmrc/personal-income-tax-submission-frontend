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
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.GiftAidSharesSecuritiesLandPropertyDonationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GiftAidSharesSecuritiesLandPropertyDonationController @Inject()(
                                                                       implicit val cc: MessagesControllerComponents,
                                                                       authAction: AuthorisedAction,
                                                                       giftAidSharesSecuritiesLandPropertyDonationView: GiftAidSharesSecuritiesLandPropertyDonationView,
                                                                       giftAidSessionService: GiftAidSessionService,
                                                                       errorHandler: ErrorHandler,
                                                                       yesNoRedirect: DonationsToPreviousTaxYearController,
                                                                       amountRedirect: GiftAidAppendNextYearTaxAmountController,
                                                                       implicit val appConfig: AppConfig
                                                                     ) extends FrontendController(cc) with I18nSupport with SessionHelper with CharityJourney {

  override def handleRedirect(
                               taxYear: Int,
                               cya: GiftAidCYAModel,
                               prior: Option[GiftAidSubmissionModel],
                               fromShow: Boolean = false
                             )(implicit user: User[AnyContent]): Result = {

    val appendToThisTaxYearYesNo = cya.addDonationToThisYear
    val appendToThisTaxYearAmount = cya.addDonationToThisYearAmount

    val prefilledForm = cya.donatedSharesSecuritiesLandOrProperty.fold(yesNoForm(user))(yesNoForm(user).fill)

    lazy val page = determineResult(
      Ok(giftAidSharesSecuritiesLandPropertyDonationView(prefilledForm, taxYear)),
      Redirect(controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(taxYear)),
      fromShow
    )

    (prior, appendToThisTaxYearYesNo, appendToThisTaxYearAmount) match {
      case (Some(priorData), _, _) if priorCondition(priorData) =>
        redirectToCya(taxYear)
      case (_, Some(yesNo), amount) if !yesNo || (yesNo && amount.nonEmpty) => if (fromShow) page else {
        Redirect(controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(taxYear))
      }
      case (_, Some(true), None) => amountRedirect.handleRedirect(taxYear, cya, prior)
      case _ => yesNoRedirect.handleRedirect(taxYear, cya, prior)
    }

  }

  private def priorCondition(prior: GiftAidSubmissionModel) = {
    val landsBuildings = prior.gifts.flatMap(_.landAndBuildings).isDefined
    val sharesSecurities = prior.gifts.flatMap(_.sharesOrSecurities).isDefined

    if (landsBuildings || sharesSecurities) true else false
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.shares-securities-land-property.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError)
  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { case (cya, prior) =>
      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>
    yesNoForm(user).bindFromRequest().fold(
      {
        formWithErrors =>
          Future.successful(BadRequest(
            giftAidSharesSecuritiesLandPropertyDonationView(formWithErrors, taxYear)
          ))
      },
      {
        yesNoForm =>
          giftAidSessionService.getSessionData(taxYear).map(_.flatMap(_.giftAid)).map {
            case Some(cyaData) =>
              val updatedCya = if (yesNoForm) {
                cyaData.copy(donatedSharesSecuritiesLandOrProperty = Some(true))
              } else {
                cyaData.copy(
                  donatedSharesSecuritiesLandOrProperty = Some(false),
                  donatedSharesOrSecurities = None,
                  donatedSharesOrSecuritiesAmount = None,
                  donatedLandOrProperty = None,
                  donatedLandOrPropertyAmount = None,
                  overseasDonatedSharesSecuritiesLandOrProperty = None,
                  overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
                  overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Seq.empty
                )
              }

              val redirectLocation = if (yesNoForm) {
                Redirect(controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear))
              } else {
                Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
              }

              giftAidSessionService.updateSessionData(updatedCya, taxYear)(errorHandler.internalServerError())(redirectLocation)
            case None => Future.successful(redirectToOverview(taxYear))
          }.flatten
      }
    )
  }
}
