package com.example.l4j.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 天气工具：调用 Open-Meteo 免费接口（无需 API Key），
 * 先通过地理编码接口把城市名转为经纬度，再查询实时天气。
 */
@Component
public class WeatherTools {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool("查询某个城市当前的实时天气，输入为城市名称，例如：北京、上海、Guangzhou")
    public String getCurrentWeather(@P("城市名称，例如：北京、上海、Guangzhou") String city) {
        if (city == null || city.isBlank()) {
            return "请提供要查询的城市名称";
        }
        try {
            // 1. 城市名 -> 经纬度
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=zh&name="
                    + URLEncoder.encode(city, StandardCharsets.UTF_8);
            JsonNode geo = objectMapper.readTree(httpGet(geoUrl));
            JsonNode results = geo.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return "未找到该城市的天气信息：" + city;
            }
            JsonNode location = results.get(0);
            double latitude = location.path("latitude").asDouble();
            double longitude = location.path("longitude").asDouble();
            String name = location.path("name").asText(city);

            // 2. 查询实时天气
            String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&timezone=auto";
            JsonNode current = objectMapper.readTree(httpGet(weatherUrl)).path("current");
            double temperature = current.path("temperature_2m").asDouble();
            int humidity = current.path("relative_humidity_2m").asInt();
            int weatherCode = current.path("weather_code").asInt();
            double windSpeed = current.path("wind_speed_10m").asDouble();

            return String.format("%s 当前天气：%s，气温 %.1f℃，相对湿度 %d%%，风速 %.1f km/h"
                            + "（数据来源 open-meteo.com）",
                    name, describeWeather(weatherCode), temperature, humidity, windSpeed);
        } catch (Exception e) {
            return "天气查询失败：" + e.getMessage();
        }
    }

    private String httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    /** WMO 天气代码（常见值）转中文描述。 */
    private String describeWeather(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1, 2 -> "多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "有雨";
            case 71, 73, 75, 77, 85, 86 -> "有雪";
            case 95, 96, 99 -> "雷暴";
            default -> "未知天气";
        };
    }
}
