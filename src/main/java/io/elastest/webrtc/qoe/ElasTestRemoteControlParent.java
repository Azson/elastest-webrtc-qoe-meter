/*
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.elastest.webrtc.qoe;

import static java.io.File.createTempFile;
import static java.lang.String.valueOf;
import static java.lang.System.nanoTime;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllBytes;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.IOUtils.copy;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.NoSuchFileException;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

public class ElasTestRemoteControlParent {

    final Logger log = getLogger(lookup().lookupClass());

    static final String REMOTE_CONTROL_JS_OBJECT = "elasTestRemoteControl";
    static final int POLL_TIME_MS = 500;

    public WebDriver driver;
    public String sut;

    public ElasTestRemoteControlParent(WebDriver driver, String sut) {
        this.driver = driver;
        this.sut = sut;

        initDriver();
    }

    private void initDriver() {
        try {
            log.debug("Testing {} with {}", sut, driver);
            driver.get(sut);

            injectRemoteControlJs();
        } catch (IOException e) {
            log.warn("Exception injecting remote-control JavaScript", e);
        }

        injectRecordRtc();
    }

    private void injectRemoteControlJs() throws IOException {
        String jsPath = "js/script.min.js";
        log.debug("Injecting {} in {}", jsPath, driver);

        String jsContent = "";
        try {
            File pageFile = new File(this.getClass().getClassLoader()
                    .getResource(jsPath).getFile());
            jsContent = new String(readAllBytes(pageFile.toPath()));
        } catch (NoSuchFileException nsfe) {
            InputStream inputStream = this.getClass().getClassLoader()
                    .getResourceAsStream(jsPath);
            StringWriter writer = new StringWriter();
            copy(inputStream, writer, defaultCharset());
            jsContent = writer.toString();
        }
        jsContent = jsContent.replaceAll("\r", "").replaceAll("\n", "");
        log.trace("Content of injected file: {}", jsContent);

        String remoteControlJs = "var remoteControlScript=window.document.createElement('script');";
        remoteControlJs += "remoteControlScript.type='text/javascript';";
        remoteControlJs += "remoteControlScript.text='" + jsContent + "';";
        remoteControlJs += "window.document.head.appendChild(remoteControlScript);";
        remoteControlJs += "return true;";
        this.executeScript(remoteControlJs);
    }

    private void injectRecordRtc() {
        String recordingJs = "var recScript=window.document.createElement('script');";
        recordingJs += "recScript.type='text/javascript';";
        recordingJs += "recScript.src='https://cdn.webrtc-experiment.com/RecordRTC.js';";
        recordingJs += "window.document.head.appendChild(recScript);";
        recordingJs += "return true;";
        this.executeScript(recordingJs);
    }

    private Object executeScript(String command) {
        return ((JavascriptExecutor) driver).executeScript(command);
    }

    private Object getProperty(String property) {
        Object value = null;
        for (int i = 0; i < 60; i++) {
            value = executeScript("return " + REMOTE_CONTROL_JS_OBJECT + "."
                    + property + ";");
            if (value != null) {
                break;
            } else {
                try {
                    log.debug("{} not present still... waiting {} ms", property,
                            POLL_TIME_MS);
                    Thread.sleep(POLL_TIME_MS);
                } catch (InterruptedException e) {
                    log.warn("Exception wait polling whil getting {}", property,
                            e);
                }
            }
        }
        String clazz = value != null ? value.getClass().getName() : "";
        log.trace(">>> getProperty {} {} {}", property, value, clazz);
        return value;
    }

    // Public API

    public String sayHello() {
        return executeScript(
                "return " + REMOTE_CONTROL_JS_OBJECT + ".sayHello();")
                        .toString();
    }

    public void startRecording(String stream) {
        executeScript(
                REMOTE_CONTROL_JS_OBJECT + ".startRecording(" + stream + ");");
    }

    public void stopRecording() {
        executeScript(REMOTE_CONTROL_JS_OBJECT + ".stopRecording();");
        getProperty("recordRTC");
    }

    public File saveRecordingToDisk(String fileName, String downloadsFolder) {
        executeScript(REMOTE_CONTROL_JS_OBJECT + ".saveRecordingToDisk('"
                + fileName + "');");
        File output = new File(downloadsFolder, fileName);
        do {
            if (!output.exists()) {
                try {
                    Thread.sleep(POLL_TIME_MS); // polling
                } catch (InterruptedException e) {
                    log.warn("Exception waiting for file {}", output, e);
                }
            } else {
                break;
            }
        } while (true);
        return output;
    }

    public void openRecordingInNewTab() {
        executeScript(REMOTE_CONTROL_JS_OBJECT + ".openRecordingInNewTab();");
    }

    public File getRecording() throws IOException {
        File tmpFile = createTempFile(valueOf(nanoTime()), ".webm");
        return getRecording(tmpFile.getAbsolutePath());
    }

    public File getRecording(String fileName) throws IOException {
        executeScript(REMOTE_CONTROL_JS_OBJECT + ".recordingToData();");
        String recording = getProperty("recordingData").toString();

        // Base64 to File
        File outputFile = new File(fileName);
        byte[] bytes = decodeBase64(
                recording.substring(recording.lastIndexOf(",") + 1));
        writeByteArrayToFile(outputFile, bytes);

        return outputFile;
    }

}
