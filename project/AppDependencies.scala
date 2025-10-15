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

import play.core.PlayVersion.current
import sbt.*

object AppDependencies {

  private val bootstrapPlay30Version = "10.2.0"
  private val hmrcMongoPlay30Version = "2.9.0"
  private val hmrcPlayFrontend = "12.17.0"

  val jacksonAndPlayExclusions: Seq[InclusionRule] = Seq(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
    ExclusionRule(organization = "com.fasterxml.jackson.module"),
    ExclusionRule(organization = "com.fasterxml.jackson.core:jackson-annotations"),
    ExclusionRule(organization = "com.typesafe.play")
  )

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30" % bootstrapPlay30Version,
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30" % hmrcPlayFrontend,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % hmrcMongoPlay30Version,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.20.0",
    "uk.gov.hmrc"                   %% "crypto-json-play-30"        % "8.4.0",
    "com.beachape"                  %% "enumeratum"                 % "1.9.0",
    "com.beachape"                  %% "enumeratum-play-json"       % "1.9.0" excludeAll (jacksonAndPlayExclusions *),
    "org.typelevel"                 %% "cats-core"                  % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % bootstrapPlay30Version,
    "org.jsoup"               %  "jsoup"                    % "1.21.2",
    "org.playframework"       %% "play-test"                % current,
    "org.mockito"             %% "mockito-scala"            % "2.0.0",
    "org.scalamock"           %% "scalamock"                % "7.5.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"  % hmrcMongoPlay30Version
  ).map(_ % "test")

}
