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
import models.dividends.StockDividendsCheckYourAnswersModel
import models.mongo._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import utils.IntegrationTest

class StockDividendsUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  val stockDividendsRepo: StockDividendsUserDataRepository = app.injector.instanceOf[StockDividendsUserDataRepository]

  val stockDividendsInvalidRepo: StockDividendsUserDataRepository = appWithInvalidEncryptionKey.injector.instanceOf[StockDividendsUserDataRepository]

  private def count: Long = await(stockDividendsRepo.collection.countDocuments().toFuture())

  class EmptyDatabase {
    await(stockDividendsRepo.collection.drop().toFuture())
    await(stockDividendsRepo.ensureIndexes())
  }

  val stockDividendsUserData: StockDividendsUserDataModel = StockDividendsUserDataModel(
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
      val result: Either[DatabaseError, Boolean] = await(stockDividendsRepo.create(stockDividendsUserData)())
      result mustBe Right(true)
      count mustBe 1
    }
    "fail to add a document to the collection when it already exists" in new EmptyDatabase {
      count mustBe 0
      await(stockDividendsRepo.create(stockDividendsUserData)())
      val result: Either[DatabaseError, Boolean] = await(stockDividendsRepo.create(stockDividendsUserData)())
      result mustBe Left(DataNotUpdated)
      count mustBe 1
    }
  }

  "update" should {

    "update a document in the collection" in new EmptyDatabase {
      val testUser: User[AnyContent] = User(
        mtditid, None, nino, "individual", sessionId
      )

      val initialData: StockDividendsUserDataModel = StockDividendsUserDataModel(
        testUser.sessionId, testUser.mtditid, testUser.nino, taxYear,
        Some(StockDividendsCheckYourAnswersModel())
      )

      val newUserData: StockDividendsUserDataModel = initialData.copy(
        stockDividends = Some(StockDividendsCheckYourAnswersModel(
          None, Some(true), Some(100.00), Some(true), Some(100.00), Some(true), Some(100.00), Some(true), Some(100.00), Some(true), Some(100.00)
        ))
      )

      await(stockDividendsRepo.create(initialData)())
      count mustBe 1

      val res: Boolean = await(stockDividendsRepo.update(newUserData).map {
        case Right(value) => value
        case Left(value) => false
      })
      res mustBe true
      count mustBe 1

      val data: Option[StockDividendsUserDataModel] = await(stockDividendsRepo.find(taxYear)(testUser).map {
        case Right(value) => value
        case Left(value) => None
      })

      data.get.stockDividends.get.ukDividendsAmount.get shouldBe 100.00
      data.get.stockDividends.get.ukDividends.get shouldBe true
      data.get.stockDividends.get.stockDividendsAmount.get shouldBe 100.00
      data.get.stockDividends.get.stockDividends.get shouldBe true
    }

    "return a leftDataNotUpdated if the document cannot be found" in {
      val newUserData = stockDividendsUserData.copy(sessionId = "sessionId-000001")
      count mustBe 1
      val res = await(stockDividendsRepo.update(newUserData))
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
      val dataAfter: Option[StockDividendsUserDataModel] = await(stockDividendsRepo.find(taxYear)(testUser).map {
        case Right(value) => value
        case Left(value) => None
      })

      dataAfter.get.stockDividends mustBe
        Some(StockDividendsCheckYourAnswersModel(
          None,
          Some(true), Some(100.0),
          Some(true),Some(100.0),
          Some(true),Some(100.0),
          Some(true),Some(100.0),
          Some(true),Some(100.0)))
    }

    "return an dataNotFoundError" in {
      await(stockDividendsInvalidRepo.find(taxYear)(testUser)) mustBe
        Left(EncryptionDecryptionError("Failed encrypting data"))
    }
  }

  "the set indexes" should {

    "enforce uniqueness" in {
      val result: Either[Exception, InsertOneResult] = try {
        Right(await(stockDividendsRepo.collection.insertOne(EncryptedStockDividendsUserDataModel(
          sessionId, mtditid, nino, taxYear
        )).toFuture()))
      } catch {
        case e: Exception => Left(e)
      }
      result.isLeft mustBe true
      result.left.e.swap.getOrElse(new Exception("wrong message")).getMessage must include(
        "E11000 duplicate key error collection: personal-income-tax-submission-frontend.stockDividendsUserData index: UserDataLookupIndex dup key:")
    }

  }

  "clear" should {

    "clear the document for the current user" in {
      count shouldBe 1
      await(stockDividendsRepo.create(StockDividendsUserDataModel(sessionId, "7788990066", nino, taxYear))())
      count shouldBe 2
      await(stockDividendsRepo.clear(taxYear))
      count shouldBe 1
    }
  }

}
