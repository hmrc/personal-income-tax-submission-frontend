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

import config._
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import models.interest.InterestCYAModel
import models.priorDataModels.StockDividendsPriorDataModel
import models.savings.SavingsIncomeCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.ZeroingWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ZeroingWarningController @Inject()(
                                          view: ZeroingWarningView,
                                          dividendsSession: DividendsSessionService,
                                          stockDividendsSession: StockDividendsSessionServiceProvider,
                                          interestSession: InterestSessionService,
                                          savingsSession: SavingsSessionService,
                                          giftAidSession: GiftAidSessionService,
                                          errorHandler: ErrorHandler
                                        )(
                                          implicit appConfig: AppConfig,
                                          auth: AuthorisedAction,
                                          mcc: MessagesControllerComponents,
                                          ec: ExecutionContext
                                        ) extends FrontendController(mcc) with I18nSupport {

  private def page(taxYear: Int, journeyKey: String)(implicit user: User[_]): Result = {
    val (continueCall, cancelHref): (Call, String) = {
      journeyKey match {
        case "dividends" => (
          controllers.routes.ZeroingWarningController.submit(taxYear, DIVIDENDS.stringify),
          controllers.dividends.routes.DividendsGatewayController.show(taxYear).url
        )
        case "stock-dividends" => (
          controllers.routes.ZeroingWarningController.submit(taxYear, STOCK_DIVIDENDS.stringify),
          controllers.dividends.routes.DividendsGatewayController.show(taxYear).url
        )
        case "interest" =>
          (
            controllers.routes.ZeroingWarningController.submit(taxYear, INTEREST.stringify),
            controllers.interest.routes.InterestGatewayController.show(taxYear).url
          )
        case "savings" =>
          (
            controllers.routes.ZeroingWarningController.submit(taxYear, SAVINGS.stringify),
            controllers.savings.routes.SavingsGatewayController.show(taxYear).url
          )
        case "gift-aid" => (
          controllers.routes.ZeroingWarningController.submit(taxYear, GIFT_AID.stringify),
          controllers.charity.routes.GiftAidGatewayController.show(taxYear).url)
      }
    }

    Ok(view(taxYear, journeyKey, continueCall, cancelHref))
  }

  private def zeroingPredicates(taxYear: Int, journeyKey: String): ActionBuilder[User, AnyContent] = {
    commonPredicates(taxYear, {
      journeyKey match {
        case "dividends" => DIVIDENDS
        case "stock-dividends" => STOCK_DIVIDENDS
        case "interest" => INTEREST
        case "savings" => SAVINGS
        case "gift-aid" => GIFT_AID
      }
    })
  }

  def show(taxYear: Int, journeyKey: String): Action[AnyContent] = zeroingPredicates(taxYear, journeyKey).apply { implicit user =>
    if (appConfig.interestTailoringEnabled || appConfig.dividendsTailoringEnabled || appConfig.charityTailoringEnabled) {
      page(taxYear, journeyKey)
    } else {
      Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

  def submit(taxYear: Int, journeyKey: String): Action[AnyContent] = zeroingPredicates(taxYear, journeyKey).async { implicit user =>
    if (appConfig.interestTailoringEnabled || appConfig.dividendsTailoringEnabled) {
      journeyKey match {
        case key@"dividends" => handleDividends(taxYear)
        case key@"stock-dividends" => handleStockDividends(taxYear)
        case "interest" => handleInterest(taxYear)
        case "savings" => handleSavings(taxYear)
        case key@"gift-aid" => handleCharity(taxYear)//TODO why gift aid dependent on interest and dividends?
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  private[controllers] def handleDividends(taxYear:Int)(implicit user: User[_]): Future[Result] = {
    dividendsSession.getAndHandle(taxYear)(errorHandler.internalServerError()) { case (cya, prior) =>
      cya match {
        case Some(cyaData) =>
          val newSessionData = zeroDividendsData(cyaData)
          dividendsSession.updateSessionData(newSessionData, taxYear)(errorHandler.internalServerError()) {
            Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
          }
        case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  private[controllers] def zeroDividendsData(cyaData: DividendsCheckYourAnswersModel): DividendsCheckYourAnswersModel = {
    cyaData.copy(
      ukDividendsAmount = if (cyaData.ukDividends.contains(true)) Some(0) else None,
      otherUkDividendsAmount = if (cyaData.otherUkDividends.contains(true)) Some(0) else None
    )
  }

  private[controllers] def handleStockDividends(taxYear: Int)(implicit user: User[_]): Future[Result] = {
    stockDividendsSession.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { case (cya, prior) =>
      cya match {
        case Some(cyaData) =>
          val newSessionData = zeroStockDividendsData(prior, cyaData)
          stockDividendsSession.updateSessionData(newSessionData, taxYear)(errorHandler.internalServerError()) {
            Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
          }
        case _ =>
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  private[controllers] def zeroStockDividendsData(prior: Option[StockDividendsPriorDataModel],
                                                  cyaData: StockDividendsCheckYourAnswersModel): StockDividendsCheckYourAnswersModel = {
    val updatedCya = zeroDividendsAnswers(prior, cyaData)
    zeroStockDividendsAnswers(prior, updatedCya)
  }

  private[controllers] def zeroDividendsAnswers(prior: Option[StockDividendsPriorDataModel],
                                                cyaData: StockDividendsCheckYourAnswersModel): StockDividendsCheckYourAnswersModel = {
    cyaData.copy(
      ukDividends = updateQuestion(prior.flatMap(_.ukDividendsAmount)),
      ukDividendsAmount = updateAmount(prior.flatMap(_.ukDividendsAmount), cyaData.ukDividendsAmount),
      otherUkDividends = updateQuestion(prior.flatMap(_.otherUkDividendsAmount)),
      otherUkDividendsAmount = updateAmount(prior.flatMap(_.otherUkDividendsAmount), cyaData.otherUkDividendsAmount)
    )
  }

  private[controllers] def zeroStockDividendsAnswers(prior: Option[StockDividendsPriorDataModel],
                                                     cyaData: StockDividendsCheckYourAnswersModel): StockDividendsCheckYourAnswersModel = {
    cyaData.copy(
      stockDividends = updateQuestion(prior.flatMap(_.stockDividendsAmount)),
      stockDividendsAmount = updateAmount(prior.flatMap(_.stockDividendsAmount), cyaData.stockDividendsAmount),
      redeemableShares = updateQuestion(prior.flatMap(_.redeemableSharesAmount)),
      redeemableSharesAmount = updateAmount(prior.flatMap(_.redeemableSharesAmount), cyaData.redeemableSharesAmount),
      closeCompanyLoansWrittenOff = updateQuestion(prior.flatMap(_.closeCompanyLoansWrittenOffAmount)),
      closeCompanyLoansWrittenOffAmount = updateAmount(prior.flatMap(_.closeCompanyLoansWrittenOffAmount), cyaData.closeCompanyLoansWrittenOffAmount)
    )
  }

  private[controllers] def updateAmount(priorAmount: Option[BigDecimal], cyaAmount:  Option[BigDecimal]): Option[BigDecimal] = {
    if(cyaAmount == priorAmount) {
      priorAmount
    } else if(priorAmount.isDefined && cyaAmount.isEmpty) {
      Some(0)
    } else {
      cyaAmount
    }
  }

  private[controllers] def updateQuestion(currentAnswer: Option[BigDecimal]): Option[Boolean] = {
    if(currentAnswer.isDefined) Some(true) else Some(false)
  }

  private[controllers] def handleInterest(taxYear: Int)(implicit user: User[_]): Future[Result] = {
    interestSession.getAndHandle(taxYear)(errorHandler.internalServerError()) { case (cya, prior) =>
      cya match {
        case Some(cyaData) =>
          val newSessionData = zeroInterestData(cyaData, prior.map(_.submissions.flatMap(_.id)).getOrElse(Seq.empty[String]))
          interestSession.updateSessionData(newSessionData, taxYear)(errorHandler.internalServerError()) {
            Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
          }
        case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  private[controllers] def zeroInterestData(data: InterestCYAModel, priorIds: Seq[String]): InterestCYAModel = {
    val zeroedData = data.copy(accounts = data.accounts.filter(account => account.id.fold(false)(priorIds.contains)).map { account =>
      account.copy(
        untaxedAmount = account.untaxedAmount.map(_ => 0),
        taxedAmount = account.taxedAmount.map(_ => 0)
      )
    })

    zeroedData.copy(
      untaxedUkInterest = Some(zeroedData.accounts.flatMap(_.untaxedAmount).nonEmpty),
      taxedUkInterest = Some(zeroedData.accounts.flatMap(_.taxedAmount).nonEmpty)
    )
  }

  private[controllers] def handleSavings(taxYear: Int)(implicit user: User[_]): Future[Result] = {
    savingsSession.getAndHandle(taxYear)(errorHandler.internalServerError()) { case (cya, prior) =>
      cya match {
        case Some(cyaData: SavingsIncomeCYAModel) =>
          val newSessionData = zeroSavingsData(cyaData)
          savingsSession.updateSessionData(newSessionData, taxYear)(errorHandler.internalServerError()) {
            Redirect(controllers.savingsBase.routes.InterestSecuritiesCyaBaseController.show(taxYear))
          }
        case _ =>
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  private[controllers] def zeroSavingsData(cyaData: SavingsIncomeCYAModel): SavingsIncomeCYAModel = {
    cyaData.copy(
      grossAmount = Some(0),
      taxTakenOffAmount = if (cyaData.taxTakenOff.contains(true)) Some(0) else None
    )
  }

  private[controllers] def handleCharity(taxYear: Int)(implicit user: User[_]): Future[Result] = {
    giftAidSession.getSessionData(taxYear).flatMap {
        case Right(userDataModel) =>
          userDataModel.map(_.giftAid) match {
            case Some(cyaData) =>
              cyaData match {
                case Some(data) => giftAidSession.updateSessionData(data.zeroData, taxYear)(errorHandler.internalServerError()) {
                  Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                }
                case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
              }
            case None => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
  }
}
