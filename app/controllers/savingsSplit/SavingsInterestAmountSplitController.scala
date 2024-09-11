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

package controllers.savingsSplit

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import forms.AmountForm
import models.savings.SavingsIncomeCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SavingsSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.savings.SavingsInterestAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavingsInterestAmountSplitController @Inject()(
                                                      view: SavingsInterestAmountView,
                                                      savingsSessionService: SavingsSessionService,
                                                      errorHandler: ErrorHandler
                                                    )
                                                    (
                                                      implicit appConfig: AppConfig,
                                                      mcc: MessagesControllerComponents,
                                                      ec: ExecutionContext,
                                                      authorisedAction: AuthorisedAction
                                                    ) extends FrontendController(mcc) with I18nSupport {

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"savings.interest-amount.errors.no-entry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"savings.interest-amount.error.wrong-format.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"savings.interest-amount.error.maximum.${if (isAgent) "agent" else "individual"}",
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    savingsSessionService.getSessionData(taxYear).flatMap {
      case Left(_) =>
        Future.successful(errorHandler.internalServerError())
      case Right(cya) =>
        if (cya.isDefined) {
          val valueCheck = cya.flatMap(_.savingsIncome.flatMap(_.grossAmount))

          valueCheck match {
            case None => Future.successful(Ok(view(form(user.isAgent, taxYear), taxYear)))
            case Some(value) => Future.successful(Ok(view(form(user.isAgent, taxYear).fill(value), taxYear)))
          }
        } else {
          savingsSessionService.createSessionData(SavingsIncomeCYAModel(Some(true)), taxYear)(errorHandler.internalServerError())(
            Ok(view(form(user.isAgent, taxYear), taxYear))
          )
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    form(user.isAgent, taxYear).bindFromRequest().fold(formWithErrors => {
      Future.successful(BadRequest(view(formWithErrors, taxYear)))
    }, {
      amount =>
        savingsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (optionalCya, prior) =>
          (optionalCya, prior) match {
            case (Some(cya), _) =>
              val newData = cya.copy(grossAmount = Some(amount))
              savingsSessionService.updateSessionData(newData, taxYear)(errorHandler.internalServerError()) {
                if (newData.isFinished) {
                  Redirect(controllers.savingsBase.routes.InterestSecuritiesCyaBaseController.show(taxYear))
                } else {
                  Redirect(controllers.savings.routes.TaxTakenFromInterestController.show(taxYear))
                }
              }
            case _ => Future.successful(Redirect(controllers.savingsBase.routes.InterestSecuritiesCyaBaseController.show(taxYear)))
          }
        }
    })
  }
}
