/*
 * Copyright 2022 HM Revenue & Customs
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

    val taxedAccounts = cyaData.taxedAccounts
    val untaxedAccounts = cyaData.untaxedAccounts

    val m1: Map[String, Option[BigDecimal]] = untaxedAccounts.foldLeft(Map.empty[String, Option[BigDecimal]]) {
      case (key, value) =>
        key + (value.accountName -> value.amount)
    }
    val m2: Map[String, Option[BigDecimal]] = taxedAccounts.foldLeft(Map.empty[String, Option[BigDecimal]]) {
      case (key, value) =>
        key + (value.accountName -> value.amount)
    }

    val accounts: Seq[InterestSubmissionModel] = (m1.keySet ++ m2.keySet).foldLeft(Map.empty[String, InterestSubmissionModel]) {
      case (m: Map[String, InterestSubmissionModel], key: String) =>
        val untaxedAccountId = untaxedAccounts.find(_.accountName == key).flatMap(_.id)
        val taxedAccountId = taxedAccounts.find(_.accountName == key).flatMap(_.id)

        m + (key -> InterestSubmissionModel(
          if (taxedAccountId.isDefined) taxedAccountId else untaxedAccountId,
          key,
          m1.get(key).flatten,
          m2.get(key).flatten))
    }.values.to[Seq]

    if(accounts.isEmpty){
      logger.info("[InterestSubmissionService][submit] User has entered No & No to both interest questions. Not submitting data to DES.")
      Future(Right(NO_CONTENT))
    } else {
      interestSubmissionConnector.submit(accounts, nino, taxYear)(hc.withExtraHeaders("mtditid" -> mtditid), ec)
    }
  }
}
