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
import java.time.Duration

import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.Using.Releasable

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions.*
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait

/**
 * DuckDuckGo searching.
 *
 * @author
 *   Chris de Vreeze
 */
object DuckDuckGo:

  // To understand Selenium better, see the WebDriver spec: https://w3c.github.io/webdriver

  final class SearchHomePage(override val driver: WebDriver) extends PageApi.SearchHomePage, PageApi.Page(driver):
    require(driver.getCurrentUrl.contains("duckduckgo.com"), s"Not the duckduckgo home page: ${driver.getCurrentUrl}")

    private val searchBoxLoc: By = By.name("q")
    private val searchButtonLoc: By = By.id("search_button_homepage")

    def search(searchString: String): PageApi.SearchResultPage =
      val searchBox = driver.findElement(searchBoxLoc)
      val searchButton = driver.findElement(searchButtonLoc)

      searchBox.clear()
      searchBox.sendKeys(searchString)
      searchButton.click()

      new SearchResultPage(driver)
    end search

  object SearchHomePage extends PageApi.PageLoader[SearchHomePage]:

    val pageUri: URI = URI.create("https://duckduckgo.com/")

    def loadPage(driver: WebDriver): SearchHomePage =
      driver.get(pageUri.toString)
      new SearchHomePage(driver)

  end SearchHomePage

  final class SearchResultPage(override val driver: WebDriver) extends PageApi.SearchResultPage, PageApi.Page(driver):
    val currentUrl: URI = URI.create(driver.getCurrentUrl)
    require(
      currentUrl.getHost.contains("duckduckgo.com") && currentUrl.getQuery.contains("q="),
      s"Not a duckduckgo search result page: $currentUrl")

    private val resultLinkLoc: By = By.cssSelector("a.js-result-title-link")

    def getSearchResultUris: Seq[URI] =
      val resultLinks: Seq[WebElement] = driver.findElements(resultLinkLoc).asScala.toList
      resultLinks.map(_.getAttribute("href")).map(URI.create)

  end SearchResultPage

  private given Releasable[WebDriver] with
    def release(r: WebDriver): Unit = r.quit()

  @main
  def doSearch(searchString: String): Unit =
    WebDriverManager.chromedriver().setup()

    val resultURIs: Seq[URI] = Using.resource(new ChromeDriver()) { driver =>
      setTimeouts(driver)
      val searchHomePage = SearchHomePage.loadPage(driver)
      val searchResultPage = searchHomePage.search(searchString)
      searchResultPage.getSearchResultUris
    }
    resultURIs.foreach(println)
  end doSearch

  private def setTimeouts(driver: WebDriver): WebDriver =
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30)) // instead of default 0 sec
    driver

end DuckDuckGo
