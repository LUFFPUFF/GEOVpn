package  org.geovpn.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients // Включает поиск Feign-клиентов
public class GeoVpnBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeoVpnBotApplication.class, args);
        System.out.println("✅ Telegram-Bot запущен");
    }
}