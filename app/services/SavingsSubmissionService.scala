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

    val nonOptBody: SavingsIncomeCYAModel = body.getOrElse(SavingsIncomeCYAModel(None, None, None, None))

    nonOptBody match {
      case SavingsIncomeCYAModel(Some(false), _, _, _) =>
        logger.info("[SavingsSubmissionService][submitSavings] User has entered No to gateway question. " +
          "Not submitting data to DES.")
        Future(Right(true))
      case _ =>
        nonOptBody.grossAmount match {
          case Some(grossAmount) =>
            val newSecurities =
            Some(SecuritiesModel(
              nonOptBody.taxTakenOffAmount,
              grossAmount,
              Some(grossAmount - nonOptBody.taxTakenOffAmount.getOrElse(BigDecimal(0)))
            ))
            val newBody = SavingsSubmissionModel(newSecurities, priorData.flatMap(_.foreignInterest).fold(Some(Seq[ForeignInterestModel]()))(data => Some(data)))
            savingsSubmissionConnector.submitSavings(newBody, nino, taxYear)(hc.withExtraHeaders("mtditid" -> mtditid), ec)
          case None =>
            logger.info("[SavingsSubmissionService][submitSavings] User is missing grossAmount" +
              "Not submitting data to DES.")
            Future(Right(true))
        }

    }
  }

}
