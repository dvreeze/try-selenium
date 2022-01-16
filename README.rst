============
Try-Selenium
============

This project is about getting to know Selenium better. One goal is to learn how to use Selenium effectively,
without dumping Thread.sleep calls all over the place in the code. In other words: is it possible
to write clear and at the same time robust Selenium client code that does not suffer from race
conditions between Selenium driver and browser? That's mainly what is explored here, using
automation of search engines as example.

The code uses the Selenium Java API, but for fun it is written in (quite straightforward) Scala 3.

Selenium 4 seems to be quite usable. The `Selenium documentation`_ can be used as a reference but
also as a tutorial. It also pays much attention to best practices, and to explicit and implicit
waits, which are essential topics in order to build robust Selenium client code.

Selenium 4 is based on the (recent) `W3C WebDriver standard`_, which formally describes the HTTP
protocol underlying the Selenium API offered for different programming languages, such as Java.
Reading this specification also solidifies the understanding of how Selenium works.

Using `Selenium Grid 4`_, Selenium tests can run in parallel on multiple machines. Note that Selenium
Grid 4 has a new code base, so its implementation is new compared to older Selenium Grid versions.

Of course frontend developers would rather use the JavaScript API instead of the Java API, but the
underlying WebDriver protocol is obviously the same. Or they would prefer to use `Cypress`_, although
it must be said that they satisfy different needs, so there is room for both of them.

.. _`Selenium documentation`: https://www.selenium.dev/documentation/
.. _`W3C WebDriver standard`: https://www.w3.org/TR/webdriver/
.. _`Selenium Grid 4`: https://www.selenium.dev/documentation/grid/
.. _`Cypress`: https://www.cypress.io/
