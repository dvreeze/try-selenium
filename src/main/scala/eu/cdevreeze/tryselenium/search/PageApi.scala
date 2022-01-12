/*
 * Copyright 2022-2022 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.tryselenium.search

import java.net.URI

import org.openqa.selenium.WebDriver

/**
 * Purely abstract Page API.
 *
 * @author
 *   Chris de Vreeze
 */
object PageApi:

  trait Page(val driver: WebDriver)

  trait PageLoader[A <: Page]:
    def loadPage(driver: WebDriver): A

  trait SearchHomePage extends Page:
    def search(searchString: String): SearchResultPage

  trait SearchResultPage extends Page:
    def getSearchResultUris: Seq[URI]

end PageApi