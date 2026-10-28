package pe.test.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "exchange-rate.api.key=clave-de-prueba")
class ExchangeServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
