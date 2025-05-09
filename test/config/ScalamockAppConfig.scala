/*
 * Copyright 2025 HM Revenue & Customs
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

package config

import org.scalamock.handlers.CallHandler0
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite

//TODO: This should be renamed to `MockAppConfig` but it is dependent on the existing `MockAppConfig` in the project being
//      refactored and removed as per the deprecation notice in the `MockAppConfig` class.
trait ScalamockAppConfig extends MockFactory { _: TestSuite =>

  val scalamockAppConfig: AppConfig = mock[AppConfig]

  def mockSessionServiceEnabled(response: Boolean): CallHandler0[Boolean] =
    (() => scalamockAppConfig.sessionCookieServiceEnabled)
      .expects()
      .returning(response)
}
