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

package connectors.stockdividends

import config.AppConfig
import connectors.RawResponseReads
import connectors.httpParsers.stockdividends.UpdateStockDividendsSessionHttpParser._
import models.dividends.StockDividendsCheckYourAnswersModel
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateStockDividendsSessionConnector @Inject()(val http: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) extends RawResponseReads {

  def updateSessionData(body: StockDividendsCheckYourAnswersModel, taxYear: Int)(implicit hc: HeaderCarrier): Future[UpdateStockDividendsSessionResponse] = {

    val stockDividendsUserDataUrl: String = appConfig.dividendsBaseUrl + s"/income-tax/income/dividends/$taxYear/stock-dividends/session"
    http.put(url"$stockDividendsUserDataUrl")
      .withBody(Json.toJson(body))
      .execute[UpdateStockDividendsSessionResponse]
  }
}
