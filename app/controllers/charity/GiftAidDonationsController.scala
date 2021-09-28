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
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.GiftAidDonationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidDonationsController @Inject()(
                                            implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            giftAidDonationView: GiftAidDonationView,
                                            giftAidSessionService: GiftAidSessionService,
                                            errorHandler: ErrorHandler,
                                            implicit val appConfig: AppConfig
                                          ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.uk-charity.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError)
  }

  implicit val executionContext: ExecutionContext = cc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      prior match {
        case Some(data) if data.giftAidPayments.map(_.currentYear).isDefined =>
          Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
        case _ =>
          Ok(giftAidDonationView(cya.flatMap(_.donationsViaGiftAid).fold(yesNoForm(user))(yesNoForm(user).fill), taxYear))
      }
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

    yesNoForm(user).bindFromRequest().fold(
      {
        formWithErrors =>
          Future.successful(BadRequest(
            giftAidDonationView(formWithErrors, taxYear)
          ))
      },
      {
        yesNoForm =>

          giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
            val priorData: Option[BigDecimal] = prior.flatMap(_.giftAidPayments.flatMap(_.currentYear))
            val updatedModel: GiftAidCYAModel = cya.getOrElse(GiftAidCYAModel()).copy(donationsViaGiftAid = Some(yesNoForm))

            if (priorData.isDefined) {
              Future.successful(Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear)))
            } else {
              val updatedCya = {
                if (yesNoForm) {
                  updatedModel
                } else {
                  updatedModel.copy(
                    donationsViaGiftAidAmount = None,
                    oneOffDonationsViaGiftAid = None,
                    oneOffDonationsViaGiftAidAmount = None,
                    overseasDonationsViaGiftAid = None,
                    overseasDonationsViaGiftAidAmount = None,
                    overseasCharityNames = Seq.empty,
                    addDonationToLastYear = None,
                    addDonationToLastYearAmount = None)
                }
              }

              val redirectLocation = (updatedCya.isFinished, yesNoForm) match {
                case (true, _) => Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                case (_, true) => Redirect(controllers.charity.routes.GiftAidDonatedAmountController.show(taxYear))
                case _ => Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
              }

              createOrUpdateSessionData(updatedCya, taxYear, cya.isEmpty)(redirectLocation)
            }
          }.flatten
      }
    )
  }

  private[charity] def createOrUpdateSessionData(cyaModel: GiftAidCYAModel, taxYear: Int, newData: Boolean)
                                                (block: Result)
                                                (implicit user: User[_]): Future[Result] = {
    if (newData) {
      giftAidSessionService.createSessionData(cyaModel, taxYear)(
        errorHandler.internalServerError()
      )(
        block
      )
    } else {
      giftAidSessionService.updateSessionData(cyaModel, taxYear)(
        errorHandler.internalServerError()
      )(
        block
      )
    }
  }
}
