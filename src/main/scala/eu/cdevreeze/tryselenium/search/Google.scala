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
import scala.util.control.NonFatal

import eu.cdevreeze.tryselenium.internal.WebDriverUtil
import eu.cdevreeze.tryselenium.internal.WebDriverUtil.given
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.ExpectedConditions.*

/**
 * Google searching.
 *
 * @author
 *   Chris de Vreeze
 */
object Google:

  // To understand Selenium better, see the WebDriver spec: https://w3c.github.io/webdriver

  final class SearchHomePage(override val driver: WebDriver) extends PageApi.SearchHomePage, PageApi.Page(driver):
    require(driver.getCurrentUrl.contains("google.com"), s"Not the google home page: ${driver.getCurrentUrl}")

    private val searchBoxLoc: By = By.name("q")

    def search(searchString: String): PageApi.SearchResultPage =
      // Element with name "btnK" is not always visible and can therefore not always be interacted with
      // We hit newline in the search box instead

      val searchBox = driver.findElement(searchBoxLoc)

      searchBox.clear()
      searchBox.sendKeys(searchString)
      searchBox.sendKeys("\n")

      new SearchResultPage(driver)
    end search

  object SearchHomePage extends PageApi.PageLoader[SearchHomePage]:

    val pageUri: URI = URI.create("https://www.google.com/")

    private val popupLoc: By = By.cssSelector("div.dbsFrd")
    private val okButtonLoc: By = By.cssSelector("button#L2AGLb")

    def loadPage(driver: WebDriver): SearchHomePage =
      driver.get(pageUri.toString)
      waitForAndAcceptPopup(driver)
      new SearchHomePage(driver)

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
      currentUrl.getHost.contains("google.com") && currentUrl.getQuery.contains("q="),
      s"Not a google search result page: $currentUrl")

    private val resultLinkParentDivLoc: By = By.cssSelector("div.yuRUbf")

    def getSearchResultUris: Seq[URI] =
      val resultLinkParentDivs: Seq[WebElement] = driver.findElements(resultLinkParentDivLoc).asScala.toList
      val resultLinks: Seq[WebElement] = resultLinkParentDivs.map(_.findElement(By.tagName("a")))
      resultLinks.map(_.getAttribute("href")).map(URI.create)

  end SearchResultPage

  @main
  def doGoogleSearch(searchString: String): Unit =
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
  end doGoogleSearch

end Google
