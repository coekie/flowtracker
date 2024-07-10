package com.coekie.flowtracker.util;

/*-
 * Copyright 2024 Wouter Coekaerts
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

import java.util.HashMap;
import java.util.Map;

/**
 * Contains configuration settings for flowtracker, specified as arguments to the agent or using the
 * flowtracker.agentArgs system property.
 */
public class Config {
  private final Map<String, String> map;

  private Config(Map<String, String> map) {
    this.map = map;
  }

  public String get(String key) {
    return map.get(key);
  }

  public String get(String key, String defaultValue) {
    return map.getOrDefault(key, defaultValue);
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    String value = map.get(key);
    if (value == null) {
      return defaultValue;
    }
    return value.isEmpty() || value.equals("true");
  }

  public boolean containsKey(String key) {
    return map.containsKey(key);
  }

  public boolean hideInternals() {
    return getBoolean("hideInternals", true);
  }

  public static Config initialize(String agentArgs) {
    HashMap<String, String> map = new HashMap<>();
    initMap(map, agentArgs);
    // system property to override agent args. Useful in IntelliJ which picks up the agent from
    // the maven surefire settings but makes it impossible to change the arguments passed to it.
    initMap(map, System.getProperty("flowtracker.agentArgs"));
    return new Config(map);
  }

  public static Config empty() {
    return new Config(Map.of());
  }

  public static Config forTesting(Map<String, String> map) {
    return new Config(map);
  }

  private static void initMap(Map<String, String> map, String properties) {
    if (properties != null) {
      for (String arg : properties.split(";")) {
        String[] keyAndValue = arg.split("=", 2);
        if (keyAndValue.length == 1) {
          map.put(keyAndValue[0], "");
        } else {
          map.put(keyAndValue[0], keyAndValue[1]);
        }
      }
    }
  }
}
