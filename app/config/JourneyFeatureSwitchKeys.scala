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

package config

sealed trait JourneyFeatureSwitchKeys {
  val stringify: String
}

case object DIVIDENDS extends JourneyFeatureSwitchKeys {
  override val stringify: String = "dividends"
}

case object STOCK_DIVIDENDS extends JourneyFeatureSwitchKeys {
  override val stringify: String = "stock-dividends"
}

case object INTEREST extends JourneyFeatureSwitchKeys {
  override val stringify: String = "interest"
}

case object GIFT_AID extends JourneyFeatureSwitchKeys {
  override val stringify: String = "gift-aid"
}

case object SAVINGS extends JourneyFeatureSwitchKeys {
  override val stringify: String = "savings"
}
