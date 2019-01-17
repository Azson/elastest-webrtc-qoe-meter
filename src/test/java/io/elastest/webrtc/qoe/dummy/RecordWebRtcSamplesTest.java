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
package io.elastest.webrtc.qoe.dummy;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;

import io.elastest.webrtc.qoe.ElasTestRemoteControlParent;
import io.github.bonigarcia.seljup.Arguments;
import io.github.bonigarcia.seljup.SeleniumExtension;

@ExtendWith(SeleniumExtension.class)
public class RecordWebRtcSamplesTest extends ElasTestRemoteControlParent {

    final Logger log = getLogger(lookup().lookupClass());

    public RecordWebRtcSamplesTest(
            @Arguments({ "--use-fake-device-for-media-stream",
                    "--use-fake-ui-for-media-stream" }) ChromeDriver driver) {
        super(driver,
                "https://webrtc.github.io/samples/src/content/devices/input-output/");
    }

    @Test
    void webrtcTest() throws InterruptedException {
        Thread.sleep(3000);

        startRecording("window.stream");

        Thread.sleep(3000);

        stopRecording();

        String downloads = System.getProperty("user.home") + File.separator
                + "Downloads";

        saveRecordingToDisk("simple.webm", downloads);
    }

}
