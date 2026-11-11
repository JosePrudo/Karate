import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;
import reporte.ReporteGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiTest {

    @Test
    void testParallel() {
        Results results = Runner.path("classpath:api")
                .outputCucumberJson(true)
                .outputJunitXml(true)
                .parallel(5);
        // Se genera antes de la aserción para que el reporte exista también cuando hay fallos
        System.out.println("Reporte de pruebas: " + ReporteGenerator.generar(results));
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
