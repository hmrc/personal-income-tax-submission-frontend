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

package models.question

import models.question.Question.{WithDependency, WithoutDependency}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Call

class QuestionTest extends AnyWordSpec with Matchers {

  "WithoutDependency" when {
    ".isValid is called" should {

      "return always TRUE" in {
        val resultOne = WithoutDependency(Some(false), Call("GET", "/some/url"))
        val resultTwo = WithoutDependency(Some(true), Call("GET", "/some/url"))
        val resultThree = WithoutDependency(None, Call("GET", "/some/url"))

        resultOne.isValid shouldBe true
        resultTwo.isValid shouldBe true
        resultThree.isValid shouldBe true
      }
    }
  }

  "WithDependency" when {

    def createAnswer(dependency: Option[Boolean], page: Option[Any]): WithDependency = {
      WithDependency(
        page,
        dependency,
        Call("GET", "/some/url"),
        Call("GET", "/some/redirect/url")
      )
    }

    ".isValid is called" should {

      "return FALSE when both dependency AND page are None" in {
        val result = createAnswer(None, None)
        result.isValid shouldEqual false
      }

      "return FALSE when dependency is None and page is defined" in {
        val result = createAnswer(None, Some(1))
        result.isValid shouldEqual false
      }

      "return FALSE when dependency is Some(false) and page is defined" in {
        val result = createAnswer(Some(false), Some(1))
        result.isValid shouldEqual false
      }

      "return FALSE when dependency is Some(false) and page is undefined" in {
        val result = createAnswer(Some(false), None)
        result.isValid shouldEqual false
      }

      "return TRUE when dependency is Some(true) and page is defined" in {
        val result = createAnswer(Some(true), Some(1))
        result.isValid shouldEqual true
      }

      "return TRUE when dependency is Some(true) and page is undefined" in {
        val result = createAnswer(Some(true), None)
        result.isValid shouldEqual true
      }
    }
  }

}
