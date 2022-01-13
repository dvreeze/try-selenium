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
      searchBox.sendKeys("\n")

      val searchLink = newWait(driver).until(elementToBeClickable(searchLinkLoc))
      searchLink.click()

      new SearchResultPage(driver)
    end search

  object SearchHomePage extends PageApi.PageLoader[SearchHomePage]:

    val pageUri: URI = URI.create("https://www.wanttoknow.info/")

    def loadPage(driver: WebDriver): SearchHomePage =
      driver.get(pageUri.toString)
      Thread.sleep(1000)
      new SearchHomePage(driver)

  end SearchHomePage

  final class SearchResultPage(override val driver: WebDriver) extends PageApi.SearchResultPage, PageApi.Page(driver):
    val currentUrl: URI = URI.create(driver.getCurrentUrl)
    require(
      currentUrl.getHost.contains("wanttoknow.info") && currentUrl.getQuery.contains("aq="),
      s"Not a wanttoknow article search result page: $currentUrl")

    private val contentDivLoc: By = By.cssSelector("div.content")

    def getSearchResultUris: Seq[URI] =
      val contentDiv: WebElement = driver.findElement(contentDivLoc)
      val resultLinks: Seq[WebElement] = contentDiv.findElements(By.tagName("a")).asScala.toSeq
      resultLinks.flatMap(link => Option(link.getAttribute("href"))).map(URI.create)

  end SearchResultPage

  private def newWait(driver: WebDriver): Wait[WebDriver] = new WebDriverWait(driver, Duration.ofSeconds(5L))

  private given Releasable[WebDriver] with
    def release(r: WebDriver): Unit = r.quit()

  @main
  def doWantToKnowArticleSearch(searchString: String): Unit =
    WebDriverManager.chromedriver().setup()

    val resultURIs: Seq[URI] = Using.resource(new ChromeDriver()) { driver =>
      setTimeouts(driver)
      val searchHomePage = SearchHomePage.loadPage(driver)
      val searchResultPage = searchHomePage.search(searchString)
      searchResultPage.getSearchResultUris
    }
    resultURIs.foreach(println)
  end doWantToKnowArticleSearch

  private def setTimeouts(driver: WebDriver): WebDriver =
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30)) // instead of default 0 sec
    driver

end WantToKnowArticles
