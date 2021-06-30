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
import forms.AmountForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidDonatedAmountView
import javax.inject.Inject
import models.User
import models.charity.GiftAidCYAModel
import services.GiftAidSessionService

class GiftAidDonatedAmountController @Inject()(
                                                implicit cc: MessagesControllerComponents,
                                                giftAidOneOffController: GiftAidOneOffController,
                                                authAction: AuthorisedAction,
                                                appConfig: AppConfig,
                                                view: GiftAidDonatedAmountView,
                                                giftAidSessionService: GiftAidSessionService,
                                                errorHandler: ErrorHandler
                                              ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  def handleRedirect(taxYear: Int, cya: GiftAidCYAModel)(implicit user: User[AnyContent]): Result = {
    cya.donationsViaGiftAid match {
      case Some(true) => Ok(view(taxYear, form(user.isAgent,taxYear), None))
      case Some(_) => giftAidOneOffController.handleRedirect(taxYear, cya)
      case _ => redirectToOverview(taxYear)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.amount-via-gift-aid.error.no-input." + agentOrIndividual,
    wrongFormatKey = "charity.amount-via-gift-aid.error.incorrect-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.amount-via-gift-aid.error.too-high." + agentOrIndividual,
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      prior match {
        case Some(priorData) if priorData.
      }

    }

    Ok(view(taxYear, form(user.isAgent,taxYear), None))
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)) { implicit user =>


    form(user.isAgent,taxYear).bindFromRequest().fold(
      { formWithErrors =>
        BadRequest(view(taxYear, formWithErrors, None))
      },
      { submittedAmount =>
        //TODO Add to data model during wireup
        Ok("YAY NEXT PAGE") //TODO direct to next page during wireup
      }
    )

  }

}
