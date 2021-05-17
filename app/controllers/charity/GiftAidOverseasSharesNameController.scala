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

import common.SessionValues
import config.{AppConfig, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.charity.GiftAidOverseasSharesNameForm
import javax.inject.Inject
import models.User
import models.charity.GiftAidSubmissionModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidOverseasSharesNameView

class GiftAidOverseasSharesNameController @Inject()(
                                                implicit cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                appConfig: AppConfig,
                                                view: GiftAidOverseasSharesNameView
                                              ) extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>
    //TODO retrieve Overseas Names from session
    val previousNames = getSessionData[GiftAidSubmissionModel](SessionValues.GIFT_AID_PRIOR_SUB).
      flatMap(_.gifts).flatMap(_.investmentsNonUkCharitiesCharityNames).getOrElse(List())

    lazy val form: Form[String] = GiftAidOverseasSharesNameForm.giftAidOverseasSharesNameForm(previousNames, user.isAgent)

    Ok(view(taxYear, form))
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)) { implicit user =>

    val previousNames = getSessionData[GiftAidSubmissionModel](SessionValues.GIFT_AID_PRIOR_SUB).
      flatMap(_.gifts).flatMap(_.investmentsNonUkCharitiesCharityNames).getOrElse(List())

    lazy val form: Form[String] = GiftAidOverseasSharesNameForm.giftAidOverseasSharesNameForm(previousNames, user.isAgent)

    form.bindFromRequest().fold(
      { formWithErrors =>
        BadRequest(view(taxYear, formWithErrors))
      },
      { submittedAmount =>
        //TODO Add to data model during wireup
        Ok("Redirect to next page") //TODO direct to next page during wireup
      }
    )

  }

  private[charity] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

}
