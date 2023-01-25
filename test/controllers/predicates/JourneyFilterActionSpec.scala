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

package controllers.predicates

import config.{AppConfig, INTEREST}
import models.User
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContent
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.UnitTest

class JourneyFilterActionSpec extends UnitTest {
  val taxYear: Int = 2022

  lazy val mockAppConf: AppConfig = mock[AppConfig]

  def taxYearAction(taxYear: Int, reset: Boolean = true): TaxYearAction = new TaxYearAction(taxYear, reset)(mockAppConf, mockMessagesControllerComponents)

  lazy val filter = new JourneyFilterAction(INTEREST, taxYear)(mockAppConf, mockExecutionContext)

  "JourneyFilterAction.refine" should {

    "return a Right" when {

      "the provided journey key is set to true" in {
        mockAppConf.isJourneyAvailable _ expects INTEREST returning true
        val result = await(filter.refine(new User[AnyContent]("asdfasfasdf", None, "AA123456A", AffinityGroup.Individual.toString, sessionId)(fakeRequest)))

        result.isRight shouldBe true
      }

    }

    "return a Left(result)" when {

      "the provided journey key is set to true" which {
        lazy val result = {
          mockAppConf.isJourneyAvailable _ expects INTEREST returning false
          mockAppConf.incomeTaxSubmissionOverviewUrl _ expects taxYear returning "/overview"
          await(filter.refine(new User[AnyContent]("asdfasfasdf", None, "AA123456A", AffinityGroup.Individual.toString, sessionId)(fakeRequest)))
        }

        "has a status of 303" in {
          result.left.get.header.status shouldBe SEE_OTHER
        }

        "has a redirect URL to the overview page" in {
          result.left.get.header.headers("Location") shouldBe "/overview"
        }
      }

    }

  }

}
