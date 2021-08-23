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

import config.{AppConfig, GIFT_AID, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.AmountForm
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel

import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.OverseasSharesSecuritiesLandPropertyAmountView

import scala.concurrent.{ExecutionContext, Future}

class OverseasSharesSecuritiesLandPropertyAmountController @Inject()(
                                                           implicit cc: MessagesControllerComponents,
                                                           appConfig: AppConfig,
                                                           view: OverseasSharesSecuritiesLandPropertyAmountView,
                                                           giftAidSharesSecuritiesLandPropertyOverseasController: GiftAidSharesSecuritiesLandPropertyOverseasController,
                                                           giftAidSessionService: GiftAidSessionService,
                                                           errorHandler: ErrorHandler,
                                                           authorisedAction: AuthorisedAction
                                                         ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  implicit val executionContext: ExecutionContext = cc.executionContext

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val priorAmount: Option[BigDecimal] = prior.flatMap(_.gifts.flatMap(_.investmentsNonUkCharities))
    val cyaAmount: Option[BigDecimal] = cya.overseasDonatedSharesSecuritiesLandOrPropertyAmount

    val amountForm = (priorAmount, cyaAmount) match {
      case (priorValueOpt, Some(cyaValue)) if !priorValueOpt.contains(cyaValue) => form(user.isAgent).fill(cyaValue)
      case _ => form(user.isAgent)
    }

    (prior, cya.overseasDonatedSharesSecuritiesLandOrProperty) match {
      case (_, Some(true)) => determineResult(Ok(view(taxYear, amountForm)),
        Redirect(controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(taxYear)),
        fromShow)
      case _ => giftAidSharesSecuritiesLandPropertyOverseasController.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.overseas-shares-securities-land-property-amount.error.empty-field." + agentOrIndividual,
    wrongFormatKey = "charity.overseas-shares-securities-land-property-amount.error.wrong-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.overseas-shares-securities-land-property-amount.error.max-amount." + agentOrIndividual
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getSessionData(taxYear).map {
      case Some(cyaData) =>
        form(user.isAgent).bindFromRequest().fold({
          formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors)))
        }, {
          amount =>
            val redirectLocation = controllers.charity.routes.GiftAidOverseasSharesNameController.show(taxYear, None)
            cyaData.giftAid.fold{
              Future.successful(redirectToOverview(taxYear))
            } {
              cyaModel => giftAidSessionService.updateSessionData(cyaModel.copy(overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(amount)), taxYear)(
                InternalServerError(errorHandler.internalServerErrorTemplate)
              )(
                Redirect(redirectLocation)
              )
            }


        }

        )
      case _ =>
        logger.info("[OverseasSharesSecuritiesLandPropertyAmountController][submit] No CYA data in session. Redirecting to overview page.")
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }.flatten
  }

}
