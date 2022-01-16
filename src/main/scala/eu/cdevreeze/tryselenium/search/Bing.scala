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

import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.Using
import scala.util.chaining.*
import scala.util.control.NonFatal

import eu.cdevreeze.tryselenium.internal.WebDriverUtil
import eu.cdevreeze.tryselenium.internal.WebDriverUtil.given
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions.*

/**
 * Bing searching.
 *
 * @author
 *   Chris de Vreeze
 */
object Bing:

  // To understand Selenium better, see the WebDriver spec: https://w3c.github.io/webdriver

  final class SearchHomePage(override val driver: WebDriver) extends PageApi.SearchHomePage, PageApi.Page(driver):
    require(driver.getCurrentUrl.contains("bing.com"), s"Not the bing search home page: ${driver.getCurrentUrl}")

    private val searchBoxLoc: By = By.cssSelector("input[name = q]")
    private val searchButtonLoc: By = By.cssSelector("label#search_icon")

    def search(searchString: String): PageApi.SearchResultPage =
      val searchBox = driver.findElement(searchBoxLoc)
      val searchButton = driver.findElement(searchButtonLoc)

      searchBox.clear()
      searchBox.sendKeys(searchString)
      searchButton.click()

      new SearchResultPage(driver)
    end search

  object SearchHomePage extends PageApi.PageLoader[SearchHomePage]:

    val pageUri: URI = URI.create("https://www.bing.com/")

    private val popupLoc: By = By.id("bnp_container")
    private val okButtonLoc: By = By.id("bnp_btn_accept")

    def loadPage(driver: WebDriver): SearchHomePage =
      driver.get(pageUri.toString)
      waitForAndAcceptPopup(driver)
      new SearchHomePage(driver)
    end loadPage

    private def waitForAndAcceptPopup(driver: WebDriver): Unit =
      Try {
        val popup = WebDriverUtil.new10SecWait(driver).until(visibilityOfElementLocated(popupLoc))
        val okButton = WebDriverUtil.new10SecWait(driver).until(elementToBeClickable(okButtonLoc))
        okButton.click()
      }.getOrElse(println("No popup or unsuccessful handling of popup"))

  end SearchHomePage

  final class SearchResultPage(override val driver: WebDriver) extends PageApi.SearchResultPage, PageApi.Page(driver):
    val currentUrl: URI = URI.create(driver.getCurrentUrl)
    require(
      currentUrl.getHost.contains("bing.com") && currentUrl.getPath.contains("search"),
      s"Not a bing search result page: $currentUrl")

    private val resultLiLoc: By = By.cssSelector("li.b_algo")

    def getSearchResultUris: Seq[URI] =
      val resultLis: Seq[WebElement] = driver.findElements(resultLiLoc).asScala.toList
      val resultLinks: Seq[WebElement] = resultLis.map(_.findElement(By.tagName("a")))
      resultLinks.map(_.getAttribute("href")).map(URI.create)

  end SearchResultPage

  @main
  def doBingSearch(searchString: String): Unit =
    WebDriverManager.chromedriver().setup()

    val resultURIs: Seq[URI] = Using.resource(WebDriverUtil.getChromeDriver) { driver =>
      try {
        WebDriverUtil.setTimeouts(driver)
        val searchHomePage = SearchHomePage.loadPage(driver)
        val searchResultPage = searchHomePage.search(searchString)
        searchResultPage.getSearchResultUris
      } catch {
        case NonFatal(e) =>
          WebDriverUtil.takeDebuggingScreenshotPrintingPath(driver, e)
          throw e
      }
    }
    resultURIs.foreach(println)
  end doBingSearch

end Bing
