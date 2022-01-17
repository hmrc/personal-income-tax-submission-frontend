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

package repositories

import config.AppConfig
import models.mongo.{EncryptedInterestUserDataModel, InterestUserDataModel}
import services.EncryptionService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class InterestUserDataRepository @Inject()(
                                             implicit mongo: MongoComponent,
                                             val ec: ExecutionContext,
                                             appConfig: AppConfig,
                                             encryptionService: EncryptionService
                                           ) extends PlayMongoRepository[EncryptedInterestUserDataModel](
  mongoComponent = mongo,
  collectionName = "interestUserData",
  domainFormat = EncryptedInterestUserDataModel.formats,
  indexes = RepositoryIndexes.indexes
) with UserDataRepository[EncryptedInterestUserDataModel]{
  override val repoName = "interestUserData"
  override type UserData = InterestUserDataModel
  override def encryptionMethod: InterestUserDataModel => EncryptedInterestUserDataModel = encryptionService.encryptInterestUserData
  override def decryptionMethod: EncryptedInterestUserDataModel => InterestUserDataModel = encryptionService.decryptInterestUserData
}
