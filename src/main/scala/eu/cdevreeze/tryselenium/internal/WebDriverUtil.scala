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

package eu.cdevreeze.tryselenium.internal

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration

import scala.util.Using.Releasable

import org.openqa.selenium.OutputType
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ThreadGuard
import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait

/**
 * WebDriver utility.
 *
 * @author
 *   Chris de Vreeze
 */
object WebDriverUtil:

  def getChromeDriver: WebDriver =
    val options = new ChromeOptions()
    // Should block popups, but does not stop the consent window from popping up
    options.setExperimentalOption("excludeSwitches", java.util.Arrays.asList("disable-popup-blocking"))
    options.setPageLoadStrategy(PageLoadStrategy.NORMAL) // Is the default, but let's be explicit about this
    // The driver can only be used from the same thread that created it
    ThreadGuard.protect(new ChromeDriver(options))

  def setTimeouts(driver: WebDriver): WebDriver =
    // Setting the implicitWait timeout to non-zero, combined with explicit waiting, is discouraged by Selenium.
    // After all, it can cause unpredictable wait times.
    // On the other hand, in practice we may need this, for example to get plenty of search results (without explicit wait).
    // Also see https://octopus.com/blog/selenium/8-mixing-waits/mixing-waits.
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30)) // instead of default 0 sec
    driver

  def takeDebuggingScreenshot(driver: WebDriver, e: Throwable): Path =
    val file: File = driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)
    val outputFile: File = File.createTempFile("selenium-screenshot-", ".png")
    Files.copy(file.toPath, outputFile.toPath, StandardCopyOption.REPLACE_EXISTING)
    outputFile.toPath

  def takeDebuggingScreenshotPrintingPath(driver: WebDriver, e: Throwable): Unit =
    val path = takeDebuggingScreenshot(driver, e)
    println(s"Screenshot (for debugging) stored at path $path")

  def new10SecWait(driver: WebDriver): Wait[WebDriver] = new WebDriverWait(driver, Duration.ofSeconds(10L))

  given Releasable[WebDriver] with
    def release(r: WebDriver): Unit = r.quit()

end WebDriverUtil
