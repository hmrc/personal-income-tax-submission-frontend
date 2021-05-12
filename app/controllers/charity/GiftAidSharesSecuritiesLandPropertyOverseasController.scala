
package controllers.charity

import config.{AppConfig, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.YesNoForm
import models.User
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper

import javax.inject.Inject

class GiftAidSharesSecuritiesLandPropertyOverseasController @Inject()(
                                                                       implicit val cc: MessagesControllerComponents,
                                                                       authAction: AuthorisedAction,
                                                                       implicit val appConfig: AppConfig
                                                                     ) extends FrontendController(cc) with I18nSupport with SessionHelper {


  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.shares-securities-land-property-overseas.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError)
  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>
    Ok
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>
    Ok
  }

}
