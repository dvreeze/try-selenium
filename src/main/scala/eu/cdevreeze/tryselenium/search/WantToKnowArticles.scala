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
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions.*

/**
 * Want-to-know article searching.
 *
 * @author
 *   Chris de Vreeze
 */
object WantToKnowArticles:

  // To understand Selenium better, see the WebDriver spec: https://w3c.github.io/webdriver

  // See http://makeseleniumeasy.com/2020/05/26/elementnotinteractableexception-element-not-interactable/ for some common problems

  final class SearchHomePage(override val driver: WebDriver) extends PageApi.SearchHomePage, PageApi.Page(driver):
    require(driver.getCurrentUrl.contains("wanttoknow.info"), s"Not the wanttoknow home page: ${driver.getCurrentUrl}")

    private val searchBoxLoc: By = By.cssSelector("input#home-q")
    private val searchLinkLoc: By = By.id("home-search-articles")

    def search(searchString: String): PageApi.SearchResultPage =
      val searchBox = driver.findElement(searchBoxLoc)
      // Element with CSS selector "input.search-submit" is not visible and can therefore not be interacted with
      // We hit newline in the search box instead

      searchBox.clear()
      searchBox.sendKeys(searchString)
      searchBox.sendKeys(Keys.ENTER)

      val searchLink = WebDriverUtil.new10SecWait(driver).until(elementToBeClickable(searchLinkLoc))
      searchLink.click()

      new SearchResultPage(driver)
    end search

  object SearchHomePage extends PageApi.PageLoader[SearchHomePage]:

    val pageUri: URI = URI.create("https://www.wanttoknow.info/")

    def loadPage(driver: WebDriver): SearchHomePage =
      driver.get(pageUri.toString)
      new SearchHomePage(driver)

  end SearchHomePage

  final class SearchResultPage(override val driver: WebDriver) extends PageApi.SearchResultPage, PageApi.Page(driver):
    val currentUrl: URI = URI.create(driver.getCurrentUrl)
    require(
      currentUrl.getHost.contains("wanttoknow.info") && currentUrl.getQuery.contains("aq="),
      s"Not a wanttoknow article search result page: $currentUrl")

    private val contentDivLoc: By = By.cssSelector("div.content")

    private val minExpectedResults = 7

    def getSearchResultUris: Seq[URI] =
      Try {
        WebDriverUtil.new10SecWait(driver)
          .until(numberOfElementsToBeMoreThan(By.cssSelector("div.content a"), minExpectedResults))
      }.getOrElse(())
      val contentDiv: WebElement = driver.findElement(contentDivLoc)
      val resultLinks: Seq[WebElement] = contentDiv.findElements(By.tagName("a")).asScala.toSeq
      resultLinks.flatMap(link => Option(link.getAttribute("href"))).map(URI.create)

  end SearchResultPage

  @main
  def doWantToKnowArticleSearch(searchString: String): Unit =
    WebDriverManager.chromedriver().setup()

    val resultURIs: Seq[URI] = Using.resource(WebDriverUtil.getChromeDriver) { driver =>
      try {
        // Default timeouts, including implicit wait time of zero, to prevent interference with explicit waits
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
  end doWantToKnowArticleSearch

end WantToKnowArticles
