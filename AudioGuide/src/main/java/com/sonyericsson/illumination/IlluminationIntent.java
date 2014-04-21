/*
 * Copyright 2013 Sony Corporation
 * Copyright (C) 2013 Sony Mobile Communications AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonyericsson.illumination;

/**
 * This class defines the Illumination intent constants.
 * <p>
 * The following functions can be achieved by android.intent.
 * <ul>
 * <li>Request to turn on a LED.</li>
 * <li>Request to turn off a LED.</li>
 * <li>Request to turn on a LED with a pulse.</li>
 * </ul>
 * </p>
 * <p>
 * The availability of the LEDs are dependent on the hardware, meaning that the requesting
 * application may not get the requested illumination effect. It is the responsibility of the
 * application to choose a suitable {@link #EXTRA_LED_ID} to be used.
 * </p>
 * <p>
 * It is always possible to start a new LED or illumination effect on a specific
 * {@link #EXTRA_LED_ID} but it is not possible to stop anything that is not started by the
 * requesting application.
 * </p>
 * <p>
 * Example to turn on the button key LED:
 * </p>
 * <pre>
 *     Intent intent = new Intent(IlluminationIntent.ACTION_START_LED);
 *     intent.putExtra(IlluminationIntent.EXTRA_LED_ID, IlluminationIntent.VALUE_BUTTON_2);
 *     intent.putExtra(IlluminationIntent.EXTRA_PACKAGE_NAME, "com.yourapplication.packagename");
 *     context.startService(intent);
 * </pre>
 * <p>
 * Characteristics of a LED
 * </p>
 * <p>
 * Priority: The different {@link #EXTRA_LED_ID} have a priority between them. E.g if there are
 * more than one {@link #EXTRA_LED_ID} connected to a physical LED and more than one is turned
 * on then that with the highest priority will be seen. When turning off the LED with highest
 * priority then the next priority will be seen. This is usually not a problem among our own
 * {@link #EXTRA_LED_ID} since the application should turn off a LED before turning it on again.
 * But it can have an impact on the standard Android IDs as they may not be seen until our own
 * {@link #EXTRA_LED_ID} are turned off.
 * </p>
 * <p>
 * Fade: A specific {@link #EXTRA_LED_ID} has a fade in (x ms) when turning the LED on and a fade
 * out (y ms) when turning the LED off. The fade is not possible to set and is different for all
 * {@link #EXTRA_LED_ID}. If setting the LED on and/or off time shorter than the time of
 * x and/or y then they will be decreased.
 * </p>
 * <p>
 * Color: A specific {@link #EXTRA_LED_ID} may support color, be white, follow the color of the
 * theme or is not implemented in a specific product which result in no light.
 * </p>
 * <p>
 * Brightness:
 * The max brightness of the LED can be set with use of the alpha value in {@link #EXTRA_LED_COLOR}
 * </p>
 * <p>
 * Description of LED curve:
 * </p>
 * <pre>
 * Brightness ^
 *  (alpha)   |
 *            |
 *       Max _|           ________________
 *            |          /                \
 *            |         /                  \
 *            |        /                    \
 *            |       /                      \
 *            |      /                        \
 *            |     /                          \
 *            |    /                            \
 *            |   /                              \
 *            |--|--------|--------------|--------|----------|--> time (ms)
 *               | Fade x |              | Fade y |          |
 *               | Led on incl fade      | LED off incl fade |
 *
 * </pre>
 *<p>
 * See this link http://developer.sonymobile.com/knowledge-base/experimental-apis/illumination-bar-api for more information regarding which ID, priority, fade and color
 * that is supported.</p>
 */

public class IlluminationIntent {
    /**
     * Turn on a LED.
     *<pre>
     * A trigger intent send by an application where IlluminationService
     * turns on a LED.
     * Application sending this intent require permission {@link #PERMISSION_ILLUMINATION}.
     *
     * Note: The application has to turn off the LED using {@link #ACTION_STOP_LED}
     * if the key {@link #EXTRA_LED_ON_TIME} is not set or set to 0.
     *
     * action  (mandatory): "com.sonyericsson.illumination.intent.action.START_LED"
     * extra   (mandatory): key: {@link #EXTRA_LED_ID}
     * extra   (mandatory): key: {@link #EXTRA_PACKAGE_NAME}
     * extra   (optional): key: {@link #EXTRA_LED_COLOR}
     * extra   (optional): key: {@link #EXTRA_LED_ON_TIME}
     * </pre>
     */
    public static final String ACTION_START_LED =
        "com.sonyericsson.illumination.intent.action.START_LED";

    /**
     * Turn off a LED.
     *<pre>
     * A trigger intent send by an application where IlluminationService
     * turns off a LED.
     * Application sending this intent require permission {@link #PERMISSION_ILLUMINATION}.
     *
     * Note: Only the application that has turned on the LED that is allowed to turn it off.
     *
     * action  (mandatory): "com.sonyericsson.illumination.intent.action.STOP_LED"
     * extra   (mandatory): key: {@link #EXTRA_LED_ID}
     * extra   (mandatory): key: {@link #EXTRA_PACKAGE_NAME}
     * </pre>
     */
    public static final String ACTION_STOP_LED =
        "com.sonyericsson.illumination.intent.action.STOP_LED";

    /**
     * Turn on a LED with a pulse effect.
     * <p>
     * A trigger intent send by an application where IlluminationService
     * turns on a LED with a pulse effect.
     * Application sending this intent require permission {@link #PERMISSION_ILLUMINATION}.
     * </p>
     * <p>
     * Note: The application has to turn off the LED using
     * {@link #ACTION_STOP_LED} if key {@link #EXTRA_LED_NO_OF_PULSES} is set to 0.
     * </p>
     * <pre>
     * action  (mandatory): "com.sonyericsson.illumination.intent.action.START_LED_PULSE"
     * extra   (mandatory): key: {@link #EXTRA_LED_ID}
     * extra   (mandatory): key: {@link #EXTRA_PACKAGE_NAME}
     * extra   (mandatory): key: {@link #EXTRA_LED_PULSE_ON_TIME}
     * extra   (mandatory): key: {@link #EXTRA_LED_PULSE_OFF_TIME}
     * extra   (mandatory): key: {@link #EXTRA_LED_NO_OF_PULSES}
     * extra   (optional): key: {@link #EXTRA_LED_COLOR}
     * </pre>
     */
    public static final String ACTION_START_LED_PULSE =
        "com.sonyericsson.illumination.intent.action.START_LED_PULSE";

    /**
     * Permission that is required to start Illumination.
     */
    public static final String PERMISSION_ILLUMINATION =
        "com.sonyericsson.illumination.permission.ILLUMINATION";

    /**
     * Used as a key for {@link #ACTION_START_LED}, {@link #ACTION_STOP_LED}
     * and {@link #ACTION_START_LED_PULSE} to set the LED id.<br>
     * Value (string): {@link #VALUE_BUTTON_RGB}, {@link #VALUE_BUTTON_2},
     * {@link #VALUE_PATTERN_1}, {@link #VALUE_PATTERN_2} and {@link #VALUE_PATTERN_3}
     */
    public static final String EXTRA_LED_ID =
        "com.sonyericsson.illumination.intent.extra.LED_ID";

   /**
     * Used as a key for {@link #ACTION_START_LED}, {@link #ACTION_STOP_LED}
     * and {@link #ACTION_START_LED_PULSE} to set the calling application.
     * This is used to keep track on which application that is using the LED.<br>
     * Value (string): Qualified application name with package, e.g.
     * "com.sonyericsson.illumination"
     */
    public static final String EXTRA_PACKAGE_NAME =
        "com.sonyericsson.illumination.intent.extra.PACKAGE_NAME";

   /**
     * Used as a key for {@link #ACTION_START_LED} to set the time LED should be on.<br>
     * Value (int): milliseconds.<br>
     * Note: 0 = always on, then the calling application must turn off the LED
     * using {@link #ACTION_STOP_LED}.
     */
    public static final String EXTRA_LED_ON_TIME =
        "com.sonyericsson.illumination.intent.extra.LED_ON_TIME";

    /**
     * Used as a key for {@link #ACTION_START_LED_PULSE} to set the time LED should be on.<br>
     * Value (int): milliseconds (greater than 0).<br>
     * Note: It is not recommended to set too low value since there is a fade in and out
     * effects on the LEDs which may bring an unwanted effect, e.g. no visible pulse.
     */
    public static final String EXTRA_LED_PULSE_ON_TIME =
        "com.sonyericsson.illumination.intent.extra.LED_PULSE_ON_TIME";

   /**
     * Used as a key for {@link #ACTION_START_LED_PULSE} to set the
     * time LED should be off.<br>
     * Value (int): milliseconds (greater than 0).<br>
     * Note: It is not recommended to set too low value since there is a fade in and out
     * effects on the LEDs which may bring an unwanted effect, e.g. no visible pulse.
     */
    public static final String EXTRA_LED_PULSE_OFF_TIME =
        "com.sonyericsson.illumination.intent.extra.LED_PULSE_OFF_TIME";

   /**
     * Used as a key for {@link #ACTION_START_LED_PULSE} to set the
     * the number of times the led shall pulse.<br>
     * Value (int).<br>
     * Note: 0 = never-ending, then the calling application must turn off the LED
     * using {@link #ACTION_STOP_LED}. It is not recommended to have a high number.
     */
    public static final String EXTRA_LED_NO_OF_PULSES =
        "com.sonyericsson.illumination.intent.extra.LED_NO_OF_PULSES";

   /**
     * Used as a key for {@link #ACTION_START_LED} and
     * {@link #ACTION_START_LED_PULSE} to set the LED color (if supported).
     * The max brightness can be set using the alpha value that is the first
     * two hex values in the value.<br>
     * Value (int): (e.g red 0xFFFF0000, green 0xFF00FF00, blue 0xFF0000FF,
     * half brightness with green 0x7F00FF00).<br>
     * Default: 0xFFFFFFFF (white) or the theme color if set.
     */
    public static final String EXTRA_LED_COLOR =
        "com.sonyericsson.illumination.intent.extra.LED_COLOR";

    /**
     * Used as a value for {@link #EXTRA_LED_ID}.<br>
     * There is a fade in and out effect on the LED to have a more pleasant
     * user experience.<br>
     * Note: Only the alpha value in {@link #EXTRA_LED_COLOR} is supported for this ID.
     * The color is either white or, if color is supported in the product, the color is
     * taken from the color of the theme.<br>
     */
    public static final String VALUE_BUTTON_2 =
        "com.sonyericsson.illumination.intent.extra.value.BUTTON_2";

    /**
     * Used as a value for {@link #EXTRA_LED_ID}.<br>
     * There is a fade in and out effect on the LED to have a more pleasant
     * user experience.<br>
     * This ID shall be used for colorized needs where white or theme color is not
     * wanted as default.<br>
     * If color is not supported in the device the LEDs will be off.<br>
     */
    public static final String VALUE_BUTTON_RGB =
        "com.sonyericsson.illumination.intent.extra.value.BUTTON_RGB";

    /**
     * Used as a value for {@link #EXTRA_LED_ID}.<br>
     * There is a fade in and out effect on the LED to have a more pleasant
     * user experience.<br>
     * If color is not supported in the device the LEDs will be white.<br>
     */
    public static final String VALUE_PATTERN_1 =
        "com.sonyericsson.illumination.intent.extra.value.PATTERN_1";

    /**
     * Used as a value for {@link #EXTRA_LED_ID}.<br>
     * There is a fade in and out effect on the LED to have a more pleasant
     * user experience.<br>
     * If color is not supported in the device the LEDs will be white.<br>
     */
    public static final String VALUE_PATTERN_2 =
        "com.sonyericsson.illumination.intent.extra.value.PATTERN_2";

    /**
     * Used as a value for {@link #EXTRA_LED_ID}.<br>
     * There is a fade in and out effect on the LED to have a more pleasant
     * user experience.<br>
     * If color is not supported in the device the LEDs will be white.<br>
     */
    public static final String VALUE_PATTERN_3 =
        "com.sonyericsson.illumination.intent.extra.value.PATTERN_3";

    /**
     * Used as a value for {@link #EXTRA_LED_ID}.<br>
     * Note: Just {@link #ACTION_START_LED} can be used to turn on this LED.<br>
     * This ID shall be used to make the LED pulse to the beat of music
     * with a color.<br>
     * If the music pulse is not supported in the device, there won't be
     * any LED effects.<br>
     * If color is not supported in the device the LEDs will be white.<br>
     */
    public static final String VALUE_AUDIO =
            "com.sonyericsson.illumination.intent.extra.value.AUDIO";
}
