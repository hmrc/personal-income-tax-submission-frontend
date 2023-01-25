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

package repositories

import models.User
import models.dividends.DividendsCheckYourAnswersModel
import models.mongo._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import utils.IntegrationTest

class UserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  val dividendsRepo: DividendsUserDataRepository = app.injector.instanceOf[DividendsUserDataRepository]

  val dividendsInvalidRepo: DividendsUserDataRepository = appWithInvalidEncryptionKey.injector.instanceOf[DividendsUserDataRepository]

  private def count: Long = await(dividendsRepo.collection.countDocuments().toFuture())

  class EmptyDatabase {
    await(dividendsRepo.collection.drop().toFuture())
    await(dividendsRepo.ensureIndexes)
  }

  val dividendsUserData: DividendsUserDataModel = DividendsUserDataModel(
    sessionId,
    mtditid,
    nino,
    taxYear,
    None
  )

  implicit val request: FakeRequest[AnyContent] = FakeRequest()

  "create" should {
    "add a document to the collection" in new EmptyDatabase {
      count mustBe 0
      val result: Either[DatabaseError, Boolean] = await(dividendsRepo.create(dividendsUserData)())
      result mustBe Right(true)
      count mustBe 1
    }
    "fail to add a document to the collection when it already exists" in new EmptyDatabase {
      count mustBe 0
      await(dividendsRepo.create(dividendsUserData)())
      val result: Either[DatabaseError, Boolean] = await(dividendsRepo.create(dividendsUserData)())
      result mustBe Left(DataNotUpdated)
      count mustBe 1
    }
  }

  "update" should {

    "update a document in the collection" in new EmptyDatabase {
      val testUser: User[AnyContent] = User(
        mtditid, None, nino, "individual", sessionId
      )

      val initialData: DividendsUserDataModel = DividendsUserDataModel(
        testUser.sessionId, testUser.mtditid, testUser.nino, taxYear,
        Some(DividendsCheckYourAnswersModel())
      )

      val newUserData: DividendsUserDataModel = initialData.copy(
        dividends = Some(DividendsCheckYourAnswersModel(
          None, Some(true), Some(100.00)
        ))
      )

      await(dividendsRepo.create(initialData)())
      count mustBe 1

      val res: Boolean = await(dividendsRepo.update(newUserData).map {
        case Right(value) => value
        case Left(value) => false
      })
      res mustBe true
      count mustBe 1

      val data: Option[DividendsUserDataModel] = await(dividendsRepo.find(taxYear)(testUser).map {
        case Right(value) => value
        case Left(value) => None
      })

      data.get.dividends.get.ukDividendsAmount.get shouldBe 100.00
      data.get.dividends.get.ukDividends.get shouldBe true
    }

    "return a leftDataNotUpdated if the document cannot be found" in {
      val newUserData = dividendsUserData.copy(sessionId = "sessionId-000001")
      count mustBe 1
      val res = await(dividendsRepo.update(newUserData))
      res mustBe Left(DataNotUpdated)
      count mustBe 1
    }
  }

  "find" should {
    def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = org.mongodb.scala.model.Filters.and(
      org.mongodb.scala.model.Filters.equal("sessionId", toBson(sessionId)),
      org.mongodb.scala.model.Filters.equal("mtdItId", toBson(mtdItId)),
      org.mongodb.scala.model.Filters.equal("nino", toBson(nino)),
      org.mongodb.scala.model.Filters.equal("taxYear", toBson(taxYear))
    )

    val testUser = User(
      mtditid, None, nino, "individual", sessionId
    )

    "get a document" in {
      count mustBe 1
      val dataAfter: Option[DividendsUserDataModel] = await(dividendsRepo.find(taxYear)(testUser).map {
        case Right(value) => value
        case Left(value) => None
      })

      dataAfter.get.dividends mustBe Some(DividendsCheckYourAnswersModel(None, Some(true), Some(100.0)))
    }

    "return an dataNotFoundError" in{
      await(dividendsInvalidRepo.find(taxYear)(testUser)) mustBe
        Left(EncryptionDecryptionError("Failed encrypting data"))
    }
  }

  "the set indexes" should {

    "enforce uniqueness" in {
      val result: Either[Exception, InsertOneResult] = try {
        Right(await(dividendsRepo.collection.insertOne(EncryptedDividendsUserDataModel(
          sessionId, mtditid, nino, taxYear
        )).toFuture()))
      } catch {
        case e: Exception => Left(e)
      }
      result.isLeft mustBe true
      result.left.e.swap.getOrElse(new Exception("wrong message")).getMessage must include(
        "E11000 duplicate key error collection: personal-income-tax-submission-frontend.dividendsUserData index: UserDataLookupIndex dup key:")
    }

  }

  "clear" should {

    "clear the document for the current user" in {
      count shouldBe 1
      await(dividendsRepo.create(DividendsUserDataModel(sessionId, "7788990066", nino, taxYear))())
      count shouldBe 2
      await(dividendsRepo.clear(taxYear))
      count shouldBe 1
    }
  }

}
