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

package utils

import models.charity.GiftAidCYAModel
import models.priorDataModels.IncomeSourcesModel
import play.api.libs.ws.WSResponse

trait CharityITHelper extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  def getResult(pageUrl: String,
                cyaData: Option[GiftAidCYAModel],
                priorData: Option[IncomeSourcesModel],
                isAgent: Boolean = false,
                welsh: Boolean = false): WSResponse = {

    wireMockServer.resetAll()

    if(priorData.isDefined) userDataStub(priorData.get, nino, taxYear) else emptyUserDataStub()

    dropGiftAidDB()
    if (cyaData.isDefined) insertGiftAidCyaData(cyaData)

    authoriseAgentOrIndividual(isAgent)
    urlGet(pageUrl, welsh, follow = false, playSessionCookie(isAgent))

  }

  def postResult(pageUrl: String,
                 cyaData: Option[GiftAidCYAModel],
                 priorData: Option[IncomeSourcesModel],
                 input: Map[String, String],
                 isAgent: Boolean = false,
                 welsh: Boolean = false): WSResponse = {

    wireMockServer.resetAll()

    if(priorData.isDefined) userDataStub(priorData.get, nino, taxYear) else emptyUserDataStub()

    dropGiftAidDB()
    if (cyaData.isDefined) insertGiftAidCyaData(cyaData)

    authoriseAgentOrIndividual(isAgent)
    urlPost(pageUrl, input, welsh, follow = false, playSessionCookie(isAgent))
  }

}
