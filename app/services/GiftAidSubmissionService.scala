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

import connectors.GiftAidSubmissionConnector
import connectors.httpParsers.GiftAidSubmissionHttpParser._
import models.charity.prior.GiftAidSubmissionModel
import play.api.Logger
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GiftAidSubmissionService @Inject()(giftAidSubmissionConnector: GiftAidSubmissionConnector) {

  def submitGiftAid(body: Option[GiftAidSubmissionModel], nino: String, mtditid: String, taxYear: Int)
                   (implicit hc: HeaderCarrier): Future[GiftAidSubmissionsResponse] = {

    lazy val logger: Logger = Logger(this.getClass.getName)

    val nonOptBody: GiftAidSubmissionModel = body.getOrElse(GiftAidSubmissionModel(None, None))

    nonOptBody match {
      case GiftAidSubmissionModel(None, None) =>
        logger.info("[GiftAidSubmissionService][submitGiftAid] User has no data inSession to submit" +
          "Not submitting data to DES.")
        Future(Right(NO_CONTENT))
      case _ =>
        giftAidSubmissionConnector.submitGiftAid(nonOptBody, nino, taxYear)(hc.withExtraHeaders("mtditid" -> mtditid))
    }
  }

}
