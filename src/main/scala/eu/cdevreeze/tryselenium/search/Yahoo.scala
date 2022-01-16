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

import java.io.File
import java.net.URI
import java.nio.file.StandardCopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.Using
import scala.util.Using.Releasable
import scala.util.chaining.*
import scala.util.control.NonFatal

import io.github.bonigarcia.wdm.WebDriverManager
import javax.activation.FileDataSource
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions.*
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait

/**
 * Yahoo searching.
 *
 * @author
 *   Chris de Vreeze
 */
object Yahoo:

  // To understand Selenium better, see the WebDriver spec: https://w3c.github.io/webdriver

  final class SearchHomePage(override val driver: WebDriver) extends PageApi.SearchHomePage, PageApi.Page(driver):
    require(driver.getCurrentUrl.contains("search.yahoo.com"), s"Not the yahoo search home page: ${driver.getCurrentUrl}")

    private val searchBoxLoc: By = By.cssSelector("input[name = p]")
    private val searchButtonLoc: By = By.cssSelector("span.mag-glass")

    def search(searchString: String): PageApi.SearchResultPage =
      val searchBox = driver.findElement(searchBoxLoc)
      val searchButton = driver.findElement(searchButtonLoc)

      searchBox.clear()
      searchBox.sendKeys(searchString)
      searchButton.click()

      new SearchResultPage(driver)
    end search

  object SearchHomePage extends PageApi.PageLoader[SearchHomePage]:

    val pageUri: URI = URI.create("https://search.yahoo.com/")

    def loadPage(driver: WebDriver): SearchHomePage =
      driver.get(pageUri.toString)

      // I obtained the consent page DOM from method driver.getPageSource
      // More on popups etc.: https://www.browserstack.com/docs/automate/selenium/handle-permission-pop-ups#introduction
      if driver.getCurrentUrl.pipe(URI.create).getHost == "consent.yahoo.com" then
        driver.findElement(By.xpath("//button[@type='submit' and @name='agree' and @value='agree']")).click()

      new SearchHomePage(driver)
    end loadPage

  end SearchHomePage

  final class SearchResultPage(override val driver: WebDriver) extends PageApi.SearchResultPage, PageApi.Page(driver):
    val currentUrl: URI = URI.create(driver.getCurrentUrl)
    require(
      currentUrl.getHost.contains("search.yahoo.com") && currentUrl.getPath.contains("search"),
      s"Not a yahoo search result page: $currentUrl")

    private val resultLinkLoc: By = By.cssSelector("a.d-ib.ls-05.fz-20.lh-26.td-hu.tc.va-bot.mxw-100p")

    def getSearchResultUris: Seq[URI] =
      val resultLinks: Seq[WebElement] = driver.findElements(resultLinkLoc).asScala.toList
      resultLinks.map(_.getAttribute("href")).map(URI.create)

  end SearchResultPage

  private given Releasable[WebDriver] with
    def release(r: WebDriver): Unit = r.quit()

  @main
  def doYahooSearch(searchString: String): Unit =
    WebDriverManager.chromedriver().setup()

    val resultURIs: Seq[URI] = Using.resource(getChromeDriver) { driver =>
      try {
        setTimeouts(driver)
        val searchHomePage = SearchHomePage.loadPage(driver)
        val searchResultPage = searchHomePage.search(searchString)
        searchResultPage.getSearchResultUris
      } catch {
        case NonFatal(e) =>
          takeDebuggingScreenshot(driver, e)
          throw e
      }
    }
    resultURIs.foreach(println)
  end doYahooSearch

  private def getChromeDriver: WebDriver =
    val options = new ChromeOptions()
    // Should block popups, but does not stop the consent window from popping up
    options.setExperimentalOption("excludeSwitches", java.util.Arrays.asList("disable-popup-blocking"))
    options.setPageLoadStrategy(PageLoadStrategy.NORMAL) // Is the default, but let's be explicit about this
    new ChromeDriver(options)

  private def setTimeouts(driver: WebDriver): WebDriver =
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30)) // instead of default 0 sec
    driver

  private def takeDebuggingScreenshot(driver: WebDriver, e: Throwable): Unit =
    val file: File = driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)
    val outputFile: File = File.createTempFile("selenium-screenshot-", ".png")
    Files.copy(file.toPath, outputFile.toPath, StandardCopyOption.REPLACE_EXISTING)
    println(s"Screenshot (for debugging) stored at path $outputFile")

end Yahoo