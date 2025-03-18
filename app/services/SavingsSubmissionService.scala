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

package services

import connectors.SavingsSubmissionConnector
import connectors.httpParsers.SavingsSubmissionHttpParser.SavingsSubmissionResponse
import models.savings.{ForeignInterestModel, SavingsIncomeCYAModel, SavingsIncomeDataModel, SavingsSubmissionModel, SecuritiesModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavingsSubmissionService @Inject()(savingsSubmissionConnector: SavingsSubmissionConnector){

  def submitSavings(body: Option[SavingsIncomeCYAModel], priorData: Option[SavingsIncomeDataModel], nino: String, mtditid: String, taxYear: Int)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SavingsSubmissionResponse] = {
    lazy val logger: Logger = Logger(this.getClass.getName)

    body match {
      case Some(SavingsIncomeCYAModel(_, None, _, _)) =>
        logger.info("[SavingsSubmissionService][submitSavings] User is missing grossAmount" +
          "Not submitting data to DES.")
        Future(Right(true))
      case _ =>
        val grossAmount = body.flatMap(_.grossAmount).getOrElse(BigDecimal(0))
        val taxTakenOffAmount = body.flatMap(_.taxTakenOffAmount)
        val newSecurities = Some(SecuritiesModel(taxTakenOffAmount, grossAmount, Some(grossAmount - taxTakenOffAmount.getOrElse(BigDecimal(0)))))
        val foreignInterestModel: Option[Seq[ForeignInterestModel]] = priorData.flatMap(_.foreignInterest)
        val newBody = SavingsSubmissionModel(newSecurities, foreignInterestModel)

        savingsSubmissionConnector.submitSavings(newBody, nino, taxYear)(hc.withExtraHeaders("mtditid" -> mtditid), ec)
    }
  }

}
