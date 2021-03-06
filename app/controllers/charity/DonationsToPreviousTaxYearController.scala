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

import config.{AppConfig, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.User
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.DonationsToPreviousTaxYearView
import javax.inject.Inject

class DonationsToPreviousTaxYearController @Inject() (
                                                      implicit val cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      donationsToPreviousTaxYearView: DonationsToPreviousTaxYearView,
                                                      implicit val appConfig: AppConfig
                                                     ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  val yesNoForm: (User[AnyContent], Int) => Form[Boolean] = (user, taxYear) => {
    val missingInputError = s"charity.donations-to-previous-tax-year.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError, Seq(taxYear.toString))
  }

  def show(taxYear: Int, otherTaxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>
    if(taxYear != otherTaxYear) {
      Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
    } else {
      Ok(donationsToPreviousTaxYearView(yesNoForm(user, taxYear), taxYear))
    }
  }

  def submit(taxYear: Int, otherTaxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)) { implicit user =>
    if(taxYear != otherTaxYear) {
      Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
    } else {
      yesNoForm(user, taxYear).bindFromRequest().fold(
        {
          formWithErrors =>
            BadRequest(donationsToPreviousTaxYearView(formWithErrors, taxYear))
        },
        {
          yesNoForm => Ok("next page")
        }
      )
    }
  }

}
