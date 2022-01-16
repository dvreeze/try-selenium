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
 * Yandex searching.
 *
 * @author
 * Chris de Vreeze
 */
object Yandex:

  // To understand Selenium better, see the WebDriver spec: https://w3c.github.io/webdriver

  final class SearchHomePage(override val driver: WebDriver) extends PageApi.SearchHomePage, PageApi.Page(driver) :
    require(driver.getCurrentUrl.contains("yandex.com"), s"Not the yandex search home page: ${driver.getCurrentUrl}")

    private val searchBoxLoc: By = By.cssSelector("input#text")

    def search(searchString: String): PageApi.SearchResultPage =
      val searchBox = driver.findElement(searchBoxLoc)

      searchBox.clear()
      searchBox.sendKeys(searchString)
      searchBox.sendKeys("\n")

      new SearchResultPage(driver)
    end search

  object SearchHomePage extends PageApi.PageLoader[SearchHomePage] :

    val pageUri: URI = URI.create("https://yandex.com/")

    private val popupLoc: By = By.cssSelector("div.sc-iqAbSa.sc-crzoUp.bphZWB.iatwsz")
    private val okButtonLoc: By = By.cssSelector("button.sc-pNWxx.sc-jrsJCI.dryRrI.bCOFvp")

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

  final class SearchResultPage(override val driver: WebDriver) extends PageApi.SearchResultPage, PageApi.Page(driver) :
    val currentUrl: URI = URI.create(driver.getCurrentUrl)
    require(
      currentUrl.getHost.contains("yandex.com") && currentUrl.getPath.contains("search"),
      s"Not a yandex search result page: $currentUrl")

    private val resultDivLoc: By =
      By.cssSelector("div.Organic.organic.Typo.Typo_text_m.Typo_line_s.i-bem")

    def getSearchResultUris: Seq[URI] =
      val resultDivs: Seq[WebElement] = driver.findElements(resultDivLoc).asScala.toList
      val resultLinks: Seq[WebElement] = resultDivs.flatMap(_.findElements(By.tagName("a")).asScala.headOption)
      resultLinks.map(_.getAttribute("href")).map(URI.create)

  end SearchResultPage

  @main
  def doYandexSearch(searchString: String): Unit =
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
  end doYandexSearch

end Yandex
