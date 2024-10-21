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

package controllers

import config.{AppConfig, DIVIDENDS, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.mongo.{JourneyAnswers, JourneyStatus}
import models.mongo.JourneyStatus.{Completed, InProgress}
import models.{Journey, SubJourney}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.SectionCompletedService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.SectionCompletedStateView

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SectionCompletedStateController @Inject()(implicit val cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                view: SectionCompletedStateView,
                                                errorHandler: ErrorHandler,
                                                implicit val appConfig: AppConfig,
                                                sectionCompeltedService: SectionCompletedService,
                                                ec: ExecutionContext
                                               ) extends FrontendController(cc) with I18nSupport {

  def form(): Form[Boolean] = YesNoForm.yesNoForm("sectionCompletedState.error.required")

  def show(taxYear: Int,journeyKey: String,subJourneyKey: String): Action[AnyContent] =
    (authAction andThen taxYearAction(taxYear)).async{ implicit user =>

      val maybeJourney: Either[String, Journey] = Journey.pathBindable.bind("journey", journeyKey)
      val maybeSubJourney: Either[String, SubJourney] = SubJourney.pathBindable.bind("subJourney", subJourneyKey)

      (maybeJourney, maybeSubJourney) match {
        case (Right(journey), Right(subJourney)) =>
          if (journey.subJourneys.contains(subJourney)){
            Future.successful(Ok(view(form(), taxYear,journeyKey,subJourneyKey)))
          } else {
            Future.successful(errorHandler.handleError(BAD_REQUEST))
           }
        case _ =>
          Future.successful(errorHandler.handleError(BAD_REQUEST))
      }
  }

  def submit(taxYear: Int,journeyKey: String,subJourneyKey: String): Action[AnyContent] = commonPredicates(taxYear,DIVIDENDS).async { implicit user =>


    form()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear,journeyKey,subJourneyKey))),
        answer => {
          val maybeJourney: Either[String, Journey] = Journey.pathBindable.bind("journey", journeyKey)
          val maybeSubJourney: Either[String, SubJourney] = SubJourney.pathBindable.bind("subJourney", subJourneyKey)

          (maybeJourney, maybeSubJourney) match {
            case (Right(journey), Right(subJourney)) =>
              if (journey.subJourneys.contains(subJourney)) {
                saveAndRedirect(answer, taxYear,subJourneyKey,user.mtditid)
              } else {
                Future.successful(errorHandler.handleError(BAD_REQUEST))
              }
            case _ =>
              Future.successful(errorHandler.handleError(BAD_REQUEST))
          }

        }
      )
  }

  // TODO: Add implementation to change status of this to in progress or completed
  private def saveAndRedirect(answer: Boolean, taxYear: Int,subJourneyKey: String,mtditid:String)(implicit hc:HeaderCarrier): Future[Result] = {
    val status: JourneyStatus = if (answer) Completed else InProgress

    sectionCompeltedService.set(JourneyAnswers(mtditid,taxYear,subJourneyKey,Json.obj({"status"-> status.toString}), Instant.now))
    Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
  }

}
