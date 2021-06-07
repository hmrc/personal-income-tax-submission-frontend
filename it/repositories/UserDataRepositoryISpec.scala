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

package repositories

import models.User
import models.dividends.DividendsCheckYourAnswersModel
import models.mongo.DividendsUserDataModel
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.AnyContent
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import utils.IntegrationTest

class UserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  val dividendsRepo: DividendsUserDataRepository = app.injector.instanceOf[DividendsUserDataRepository]

  val taxYear = 2022

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
      val result: Boolean = await(dividendsRepo.create(dividendsUserData))
      result mustBe true
      count mustBe 1
    }
  }

  "update" should {

    "update a document in the collection" in new EmptyDatabase {
      val testUser: User[AnyContent] = User(
        mtditid, None, nino, "individual", sessionId
      )

      val ludicrousAmount: BigDecimal = 999999999

      val initialData: DividendsUserDataModel = DividendsUserDataModel(
        testUser.sessionId, testUser.mtditid, testUser.nino, taxYear,
        Some(DividendsCheckYourAnswersModel(
          Some(true),
          Some(100.00)
        ))
      )

      val newUserData: DividendsUserDataModel = initialData.copy(
        dividends = Some(DividendsCheckYourAnswersModel(
          Some(true), Some(ludicrousAmount)
        ))
      )

      await(dividendsRepo.create(initialData))
      count mustBe 1

      val res: Boolean = await(dividendsRepo.update(newUserData))
      res mustBe true
      count mustBe 1

      val data: DividendsUserDataModel = await(dividendsRepo.find(taxYear)(testUser)).get
      data.dividends.get.ukDividendsAmount.get shouldBe ludicrousAmount
    }

    "return a false if the document cannot be found" in {
      val newUserData = dividendsUserData.copy(sessionId = "sessionId-000001")
      count mustBe 1
      val res: Boolean = await(dividendsRepo.update(newUserData))
      res mustBe false
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

    "get a document and update the TTL" in {
      count mustBe 1
      val dataBefore = await(dividendsRepo.collection.find(
        filter(sessionId, mtditid, nino, dividendsUserData.taxYear)
      ).toFuture()).head
      val dataAfter: Option[DividendsUserDataModel] = await(dividendsRepo.find(taxYear)(testUser))

      dataAfter.map(_.copy(lastUpdated = dataBefore.lastUpdated)) mustBe Some(dataBefore)
      dataAfter.map(_.lastUpdated.isAfter(dataBefore.lastUpdated)) mustBe Some(true)
    }
  }

  "the set indexes" should {

    "enforce uniqueness" in {
      val result: Either[Exception, InsertOneResult] = try {
        Right(await(dividendsRepo.collection.insertOne(DividendsUserDataModel(
          sessionId, mtditid, nino, taxYear
        )).toFuture()))
      } catch {
        case e: Exception => Left(e)
      }
      result.isLeft mustBe true
      result.left.get.getMessage must include(
        "E11000 duplicate key error collection: personal-income-tax-submission-frontend.dividendsUserData index: UserDataLookupIndex dup key:")
    }

  }

  "clear" should {

    "clear the document for the current user" in {
      count shouldBe 1
      dividendsRepo.create(DividendsUserDataModel(sessionId, "7788990066", nino, taxYear))
      count shouldBe 2
      dividendsRepo.clear(taxYear)
      count shouldBe 1
    }
  }

}
