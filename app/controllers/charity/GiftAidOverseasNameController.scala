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
import forms.charity.GiftAidOverseasNameForm
import models.User
import models.charity.prior.GiftAidSubmissionModel
import models.charity.{CharityNameModel, GiftAidCYAModel}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidOverseasNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidOverseasNameController @Inject()(
                                               implicit cc: MessagesControllerComponents,
                                               giftAidOverseasAmountController: GiftAidOverseasAmountController,
                                               authAction: AuthorisedAction,
                                               appConfig: AppConfig,
                                               view: GiftAidOverseasNameView,
                                               giftAidSessionService: GiftAidSessionService,
                                               errorHandler: ErrorHandler,
                                               ec: ExecutionContext
                                             ) extends FrontendController(cc) with I18nSupport with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    cya.overseasDonationsViaGiftAidAmount match {
      case Some(_) => determineResult(
        Ok(view(taxYear, form(user.isAgent, cya))),
        Redirect(controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None)),
        fromShow)
      case _ => giftAidOverseasAmountController.handleRedirect(taxYear, cya, prior)
    }
  }

  def show(taxYear: Int, changeCharityId: Option[String]): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      (cya, changeCharityId) match {
        case (Some(cyaData), Some(id)) if cyaData.overseasDonationsViaGiftAidAmount.isDefined =>
          cyaData.overseasCharityNames.find(_.id == id).map(_.name).fold(
            Redirect(controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear))
          )(charityName => Ok(view(taxYear, form(user.isAgent, cyaData, Some(id)).fill(charityName), Some(id))))
        case (Some(cyaData), _) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int, changeCharityId: Option[String]): Action[AnyContent] =
    (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async {
      implicit user =>

        giftAidSessionService.getSessionData(taxYear).map {
          case Left(_) => Future.successful(errorHandler.internalServerError())
          case Right(data) =>
            data.flatMap(_.giftAid) match {
          case Some(cyaModel) =>
            form(user.isAgent, cyaModel, changeCharityId).bindFromRequest().fold({
              formWithErrors =>
                Future.successful(BadRequest(view(taxYear, formWithErrors, changeCharityId)))
            }, {
              formName =>
                val updatedCya = {
                  (changeCharityId, cyaModel.overseasCharityNames) match {
                    case (Some(id), namesList) =>
                      val indexToBeUpdated = namesList.indexOf(CharityNameModel(id, namesList.find(_.id == id).map(_.name).get))
                      cyaModel.copy(overseasCharityNames = namesList.updated(indexToBeUpdated, CharityNameModel(id, formName)))
                    case (_, namesList) => cyaModel.copy(overseasCharityNames = namesList :+ CharityNameModel(formName))
                    case _ => cyaModel.copy(overseasCharityNames = Seq(CharityNameModel(formName)))
                  }
                }

                giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(
                  Redirect(controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear))
                )
            })
          case _ =>
            logger.info("[GiftAidOverseasNameController][submit] No CYA data in session. Redirecting to overview page.")
            Future.successful(redirectToOverview(taxYear))
        }
        }.flatten
    }

  private def form(isAgent: Boolean, cya: GiftAidCYAModel, changedCharityId: Option[String] = None): Form[String] = {
    val previousNames = cya.overseasCharityNames.filterNot(_.id == changedCharityId.getOrElse("")).map(_.name).toList
    GiftAidOverseasNameForm.giftAidOverseasNameForm(previousNames, isAgent)
  }
}

