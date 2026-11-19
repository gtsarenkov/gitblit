/*
 * Copyright 2013 akquinet tech@spree GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.akquinet.devops.test.ui.generic;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import com.gitblit.GitBlitServer;

import de.akquinet.devops.GitblitRunnable;

/**
 * This abstract class implements the setUpClass and tearDownClass for
 * selenium-based UITests. They require a running gitblit server instance and a
 * webdriver instance, which are managed by the setUpClass and tearDownClass
 * method. Write a separate test class derived from this abstract class for each
 * scenario consisting of one or more test cases, which can share the same
 * server instance.
 * 
 * @author saheba
 * 
 */
public abstract class AbstractUITest {

	private static Thread serverThread;
	private static WebDriver driver;

	private static final int HTTP_PORT = 8080, HTTPS_PORT = 8443,
			SHUTDOWN_PORT = 8081;
	private static final String GITBLIT_PROPERTIES_PATH = "test-ui-gitblit.properties",
			USERS_PROPERTIES_PATH = "test-ui-users.conf";

	/**
	 * starts a gitblit server instance in a separate thread before test cases
	 * of concrete, non-abstract child-classes are executed
	 */
	@BeforeClass
	public static void setUpClass() {
		FirefoxOptions options = new FirefoxOptions()
				.addArguments("-headless")
				.setAcceptInsecureCerts(true)
				.addPreference("browser.download.folderList", 2)
				.addPreference("browser.download.dir", System.getProperty("java.io.tmpdir"))
				.addPreference("browser.helperApps.neverAsk.saveToDisk",
						"text/csv,text/plain,application/zip,application/pdf");
		driver = new FirefoxDriver(options);

		Runnable gitblitRunnable = new GitblitRunnable(HTTP_PORT, HTTPS_PORT,
				SHUTDOWN_PORT, GITBLIT_PROPERTIES_PATH, USERS_PROPERTIES_PATH);
		serverThread = new Thread(gitblitRunnable);
		serverThread.start();
	}

	/**
	 * stops the gitblit server instance running in a separate thread after test
	 * cases of concrete, non-abstract child-classes have been executed
	 */
	@AfterClass
	public static void tearDownClass() throws InterruptedException {
		try {
			if (driver != null) {
				driver.quit();
			}
		} finally {
			driver = null;
			if (serverThread != null) {
				GitBlitServer.main("--stop", "--shutdownPort", Integer.toString(SHUTDOWN_PORT));
				serverThread = null;
				Thread.sleep(1000);
			}
		}
	}

	public static WebDriver getDriver() {
		return AbstractUITest.driver;
	}
}
