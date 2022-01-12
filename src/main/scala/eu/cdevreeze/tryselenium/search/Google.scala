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
import scala.util.Try
import scala.util.Using
import scala.util.Using.Releasable

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.ExpectedConditions.*
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait

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
    private val searchButtonLoc: By = By.name("btnK")

    def search(searchString: String): PageApi.SearchResultPage =
      val searchBox = driver.findElement(searchBoxLoc)
      val searchButton = driver.findElement(searchButtonLoc)

      searchBox.clear()
      searchBox.sendKeys(searchString)
      searchButton.click()

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
        val popup = newWait(driver).until(visibilityOfElementLocated(popupLoc))
        val okButton = newWait(driver).until(elementToBeClickable(okButtonLoc))
        okButton.click()
      }.getOrElse(println("No popup or unsuccesful handling of popup"))
      driver.switchTo().defaultContent()
      sleep()

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

  private def newWait(driver: WebDriver): Wait[WebDriver] = new WebDriverWait(driver, Duration.ofSeconds(5L))

  private given Releasable[WebDriver] with
    def release(r: WebDriver): Unit = r.quit()

  @main
  def doGoogleSearch(searchString: String): Unit =
    WebDriverManager.chromedriver().setup()

    val resultURIs: Seq[URI] = Using.resource(new ChromeDriver()) { driver =>
      setTimeouts(driver)
      val searchHomePage = SearchHomePage.loadPage(driver)
      val searchResultPage = searchHomePage.search(searchString)
      searchResultPage.getSearchResultUris
    }
    resultURIs.foreach(println)

  private def setTimeouts(driver: WebDriver): WebDriver =
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30)) // instead of default 0 sec
    driver

  private def sleep(): Unit = Thread.sleep(2000)

end Google
