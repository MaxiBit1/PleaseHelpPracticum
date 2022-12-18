import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Converter {
    double rateUSD;
    double rateEUR;
    double rateJPY;
    private HttpClient client;

    public Converter() {
        client = HttpClient.newHttpClient();
    }

    private double getRate(String symbolCurrency) {
        double result = 0;
        URI url = URI.create("https://api.exchangerate.host/latest?base=RUB&symbol="+symbolCurrency);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(url)
                .build();

        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 200) {
                JsonElement jsonElement = JsonParser.parseString(response.body());
                if(!jsonElement.isJsonObject()) {
                    System.out.println("Ответ от сервера не соответствует ожидаемому.");
                    return 0;
                }

                JsonObject jsonObject = jsonElement.getAsJsonObject();
                result = jsonObject.get("rates").getAsJsonObject().get(symbolCurrency).getAsDouble();
            } else {
                return 0;
            }
        }catch (IOException | InterruptedException e) {
            return 0;
        }
        return result;
    }

    public Converter(double usd, double eur, double jpy) {
        rateUSD = usd;
        rateEUR = eur;
        rateJPY = jpy;
    }

    public void convert(double rubles, String currency) {
        double rateCurrence = getRate(currency);
        if(rateCurrence == 0.0) {
            return;
        }
        System.out.println("Ваши сбережения: "+ rubles*rateCurrence + " " + currency);
    }
}