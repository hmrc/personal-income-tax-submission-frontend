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

package services

import connectors.InterestSubmissionConnector
import connectors.httpParsers.InterestSubmissionHttpParser.InterestSubmissionsResponse
import models.interest.{InterestCYAModel, InterestSubmissionModel}
import play.api.Logger
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestSubmissionService @Inject()(interestSubmissionConnector: InterestSubmissionConnector) {

  lazy val logger: Logger = Logger(this.getClass.getName)

  def submit(cyaData: InterestCYAModel, nino: String, taxYear: Int, mtditid: String)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[InterestSubmissionsResponse] = {

    val untaxedAccounts: Seq[InterestSubmissionModel] = cyaData.untaxedUkAccounts.map(_.map { account =>
      InterestSubmissionModel(account.id, account.accountName, Some(account.amount), None)
    }).getOrElse(Seq.empty[InterestSubmissionModel])

    val taxedAccounts: Seq[InterestSubmissionModel] = cyaData.taxedUkAccounts.map(_.map { account =>
      InterestSubmissionModel(account.id, account.accountName, None, Some(account.amount))
    }).getOrElse(Seq.empty[InterestSubmissionModel])

    val body: Seq[InterestSubmissionModel] = untaxedAccounts ++ taxedAccounts

    if(body.isEmpty){
      logger.info("[InterestSubmissionService][submit] User has entered No & No to both interest questions. Not submitting data to DES.")
      Future(Right(NO_CONTENT))
    } else {
      interestSubmissionConnector.submit(body, nino, taxYear, mtditid)
    }
  }
}
