package reporte;

import com.intuit.karate.Results;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Genera un reporte HTML con gráficas, análisis y conclusiones a partir de los resultados de Karate.
 * Siempre escribe en la misma ruta y sobrescribe el reporte de la ejecución anterior.
 */
public final class ReporteGenerator {

    public static final Path RUTA_REPORTE = Path.of("reportes", "Reporte_Pruebas.html");
    private static final double UMBRAL_LENTO_S = 5.0;
    private static final Set<String> TAGS_NO_MODULO = Set.of("smoke", "negativo", "borde", "defecto", "ignore");

    private static final String GREEN = "#10b981", RED = "#f43f5e", AMBER = "#f59e0b", INK3 = "#7a82a3";

    private ReporteGenerator() {
    }

    /** Un caso de prueba agrupa las iteraciones de un Scenario Outline (misma línea del mismo feature). */
    private static final class Caso {
        String id, titulo, modulo, tipo, feature;
        int linea, iteraciones, fallidas;
        double segundos;
        boolean defectoConocido;
        final List<String> nombres = new ArrayList<>();
        final List<String> errores = new ArrayList<>();

        boolean fallo() {
            return fallidas > 0;
        }
    }

    public static Path generar(Results results) {
        List<Caso> casos = agrupar(results);
        String html = cargarPlantilla();
        long total = casos.size();
        long fallidos = casos.stream().filter(Caso::fallo).count();
        long aprobados = total - fallidos;
        int tasa = total == 0 ? 0 : (int) Math.round(aprobados * 100.0 / total);
        Set<String> features = casos.stream().map(c -> c.feature).collect(Collectors.toCollection(java.util.TreeSet::new));
        LocalDateTime ahora = LocalDateTime.now();

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("{{TITULO}}", "Última ejecución");
        tokens.put("{{ALCANCE}}", esc(total + " casos en " + features.size() + " features: " + String.join(", ", features)));
        tokens.put("{{EJECUCION}}", "Última ejecución · " + results.getScenariosTotal() + " iteraciones · "
                + fmt1(results.getElapsedTime() / 1000) + " s");
        tokens.put("{{PROYECTO}}", "Karate · Pruebas de API");
        tokens.put("{{AMBIENTE}}", "karate.env = " + esc(String.valueOf(System.getProperty("karate.env", "dev"))));
        tokens.put("{{FECHA}}", ahora.format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm:ss", Locale.forLanguageTag("es"))));
        tokens.put("{{ANALISTA}}", esc(System.getProperty("user.name", "QA")) + " · ejecución automatizada");
        tokens.put("{{TASA}}", String.valueOf(tasa));
        tokens.put("{{APROBADOS}}", String.valueOf(aprobados));
        tokens.put("{{FALLIDOS}}", String.valueOf(fallidos));
        tokens.put("{{TOTAL}}", String.valueOf(total));
        tokens.put("{{ANIO}}", String.valueOf(ahora.getYear()));
        tokens.put("{{MAIN}}", main(casos, results, aprobados, fallidos, tasa));
        for (Map.Entry<String, String> t : tokens.entrySet()) {
            html = html.replace(t.getKey(), t.getValue());
        }

        try {
            Files.createDirectories(RUTA_REPORTE.toAbsolutePath().getParent());
            Files.writeString(RUTA_REPORTE, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el reporte " + RUTA_REPORTE.toAbsolutePath(), e);
        }
        return RUTA_REPORTE.toAbsolutePath();
    }

    private static List<Caso> agrupar(Results results) {
        Map<String, Caso> mapa = new LinkedHashMap<>();
        List<ScenarioResult> ordenados = results.getScenarioResults()
                .sorted(Comparator.comparing((ScenarioResult r) -> r.getScenario().getFeature().getResource().getRelativePath())
                        .thenComparingInt(r -> r.getScenario().getSection().getLine())
                        .thenComparingInt(r -> r.getScenario().getExampleIndex()))
                .collect(Collectors.toList());
        for (ScenarioResult r : ordenados) {
            Scenario s = r.getScenario();
            String feature = s.getFeature().getResource().getRelativePath()
                    .replaceFirst("^classpath:", "").replaceFirst("\\.feature$", "");
            // La sección es la misma para todas las iteraciones de un Scenario Outline
            Caso c = mapa.computeIfAbsent(feature + "#" + s.getSection().getLine(), k -> nuevoCaso(s, feature));
            c.iteraciones++;
            c.nombres.add(s.getName());
            c.segundos += r.getDurationMillis() / 1000;
            if (r.isFailed()) {
                c.fallidas++;
                String nombre = s.getExampleIndex() >= 0 ? "[" + s.getName() + "] " : "";
                c.errores.add(nombre + r.getErrorMessage());
            }
        }
        for (Caso c : mapa.values()) {
            c.titulo = titulo(c);
        }
        return new ArrayList<>(mapa.values());
    }

    private static Caso nuevoCaso(Scenario s, String feature) {
        List<String> tags = s.getTagsEffective().getTags().stream().map(t -> t.replaceFirst("^@", "")).collect(Collectors.toList());
        List<String> tagsFeature = s.getFeature().getTags() == null ? List.of()
                : s.getFeature().getTags().stream().map(t -> t.getName().replaceFirst("^@", "")).collect(Collectors.toList());
        Caso c = new Caso();
        c.feature = feature;
        c.linea = s.getSection().getLine();
        c.id = tags.stream().filter(t -> t.matches("(?i)CP-\\w+")).findFirst()
                .orElse(feature.substring(feature.lastIndexOf('/') + 1) + ":" + c.linea);
        c.modulo = tags.stream()
                .filter(t -> !t.matches("(?i)CP-\\w+") && !TAGS_NO_MODULO.contains(t.toLowerCase()) && !tagsFeature.contains(t))
                .findFirst().map(ReporteGenerator::capitalizar)
                .orElse(s.getFeature().getName() == null || s.getFeature().getName().isBlank() ? feature : s.getFeature().getName());
        c.tipo = tags.contains("negativo") ? "Negativo" : tags.contains("borde") ? "Borde" : "Positivo";
        c.defectoConocido = tags.contains("defecto");
        return c;
    }

    private static String titulo(Caso c) {
        String t = c.nombres.get(0);
        if (c.nombres.size() > 1) {
            for (String n : c.nombres) {
                int i = 0;
                while (i < t.length() && i < n.length() && t.charAt(i) == n.charAt(i)) {
                    i++;
                }
                t = t.substring(0, i);
            }
            t = t.replaceAll("[\\s\\-:]+$", "");
        }
        return t.replaceFirst("(?i)^" + java.util.regex.Pattern.quote(c.id) + "\\s*", "");
    }

    // ------------------------------------------------------------------ HTML

    private static String main(List<Caso> casos, Results results, long aprobados, long fallidos, int tasa) {
        List<Caso> fallidosList = casos.stream().filter(Caso::fallo).collect(Collectors.toList());
        boolean todosConocidos = fallidosList.stream().allMatch(c -> c.defectoConocido);
        String veredicto = fallidos == 0 ? "aprobado" : todosConocidos ? "observado" : "no-aprobado";
        String etiqueta = fallidos == 0 ? "Aprobado" : todosConocidos ? "Observado" : "No aprobado";
        List<Caso> lentos = casos.stream().filter(c -> c.segundos > UMBRAL_LENTO_S)
                .sorted(Comparator.comparingDouble((Caso c) -> c.segundos).reversed()).collect(Collectors.toList());

        Map<String, long[]> modulos = new LinkedHashMap<>();
        Map<String, Integer> tipos = new LinkedHashMap<>();
        for (Caso c : casos) {
            long[] m = modulos.computeIfAbsent(c.modulo, k -> new long[2]);
            m[c.fallo() ? 1 : 0]++;
            tipos.merge(c.tipo, 1, Integer::sum);
        }

        StringBuilder sb = new StringBuilder("<main>\n");

        // 01 Veredicto
        String motivo = fallidos == 0
                ? "Los " + casos.size() + " casos de prueba fueron aprobados (100 %). No se encontraron defectos en esta ejecución."
                : aprobados + " de " + casos.size() + " casos aprobados (" + tasa + " %). Fallaron: "
                + fallidosList.stream().map(c -> "<b>" + esc(c.id) + "</b>").collect(Collectors.joining(", ")) + "."
                + (todosConocidos ? " Todos los fallos corresponden a defectos conocidos (etiqueta @defecto)."
                : " Hay fallos no clasificados como defecto conocido que deben analizarse antes de liberar.");
        sb.append(seccion("veredicto", "01", "Veredicto"))
                .append("<div class=\"card verdict ").append(veredicto).append("\"><span class=\"verdict-stamp\">").append(etiqueta)
                .append("</span><h4 class=\"subttl\">Motivo</h4><p>").append(motivo).append("</p></div></section>\n");

        // 02 Gráficas
        sb.append(seccion("graficas", "02", "Análisis gráfico de resultados"))
                .append("<div class=\"kpis\">")
                .append(kpi("", String.valueOf(casos.size()), "Casos ejecutados"))
                .append(kpi("", String.valueOf(results.getScenariosTotal()), "Iteraciones"))
                .append(kpi("ok", String.valueOf(aprobados), "Casos aprobados"))
                .append(kpi("fail", String.valueOf(fallidos), "Casos fallidos"))
                .append(kpi("", fmt1(results.getElapsedTime() / 1000) + " s", "Duración de la ejecución"))
                .append("</div><div class=\"charts\">")
                .append("<figure class=\"chart-card\"><figcaption>Resultado global por caso</figcaption><div class=\"donut-wrap\">")
                .append(donut(aprobados, fallidos))
                .append("<ul class=\"legend\"><li><i style=\"background:").append(GREEN).append("\"></i>Aprobados: ").append(aprobados)
                .append("</li><li><i style=\"background:").append(RED).append("\"></i>Fallidos: ").append(fallidos).append("</li></ul></div></figure>")
                .append("<figure class=\"chart-card\"><figcaption>Resultados por módulo funcional</figcaption>")
                .append(barrasApiladas(modulos)).append("</figure></div>")
                .append("<figure class=\"chart-card wide\"><figcaption>Duración por caso de prueba (segundos) · verde &lt; ")
                .append(fmt1(UMBRAL_LENTO_S)).append(" s · ámbar &gt; ").append(fmt1(UMBRAL_LENTO_S)).append(" s · rojo = fallido</figcaption>")
                .append(barrasVerticales(casos)).append("</figure>")
                .append("<div class=\"charts\"><figure class=\"chart-card\"><figcaption>Distribución por tipo de prueba</figcaption><div class=\"kpis small\">");
        tipos.forEach((t, n) -> sb.append(kpi("", String.valueOf(n), t)));
        long noPositivos = casos.size() - tipos.getOrDefault("Positivo", 0);
        sb.append("</div><p class=\"muted\">").append(pct(noPositivos, casos.size()))
                .append(" % de los casos son negativos o de borde.</p></figure>")
                .append("<figure class=\"chart-card\"><figcaption>Casos más lentos</figcaption><ol class=\"muted\" style=\"margin:0\">");
        casos.stream().sorted(Comparator.comparingDouble((Caso c) -> c.segundos).reversed()).limit(5)
                .forEach(c -> sb.append("<li><code>").append(esc(c.id)).append("</code> ").append(esc(c.titulo))
                        .append(" · <b>").append(fmt1(c.segundos)).append(" s</b></li>"));
        sb.append("</ol></figure></div></section>\n");

        // 03 Tabla de casos
        sb.append(seccion("casos", "03", "Resumen de Casos de prueba"))
                .append("<div class=\"table-wrap\"><table class=\"cases\"><colgroup><col style=\"width: 110px\" /><col />")
                .append("<col style=\"width: 130px\" /><col style=\"width: 32%\" /></colgroup><thead><tr><th scope=\"col\">ID</th>")
                .append("<th scope=\"col\">Caso de prueba</th><th scope=\"col\">Resultado</th><th scope=\"col\">Observaciones</th></tr></thead><tbody>\n");
        for (Caso c : casos) {
            List<String> obs = new ArrayList<>();
            if (c.iteraciones > 1) {
                obs.add((c.iteraciones - c.fallidas) + "/" + c.iteraciones + " iteraciones PASS");
            }
            if (c.fallo()) {
                obs.add(c.defectoConocido ? "Defecto conocido (@defecto)" : "Fallo nuevo: revisar");
            }
            if (c.segundos > UMBRAL_LENTO_S) {
                obs.add("Duración " + fmt1(c.segundos) + " s");
            }
            sb.append("<tr><td><code>").append(esc(c.id)).append("</code></td><td>").append(esc(c.titulo))
                    .append("<br/><small class=\"muted\">").append(esc(c.modulo)).append(" · ").append(c.tipo).append(" · ")
                    .append(esc(c.feature)).append(".feature:").append(c.linea).append("</small></td><td><span class=\"pill ")
                    .append(c.fallo() ? "fail\">FAIL" : "ok\">PASS").append("</span></td><td>")
                    .append(obs.isEmpty() ? "—" : esc(String.join("; ", obs))).append("</td></tr>\n");
        }
        sb.append("</tbody></table></div></section>\n");

        // 04 Fallos (se omite si no hay)
        if (!fallidosList.isEmpty()) {
            sb.append(seccion("defectos", "04", "Casos fallidos y defectos"));
            for (Caso c : fallidosList) {
                String sev = c.defectoConocido ? "med" : "high";
                sb.append("<article class=\"card defect sev-").append(sev).append("\"><header class=\"defect-head\"><span class=\"id\">")
                        .append(esc(c.id)).append("</span><h3>").append(esc(c.titulo)).append("</h3><span class=\"sev sev-").append(sev)
                        .append("\">").append(c.defectoConocido ? "Medio" : "Alto").append("</span></header><div class=\"defect-body\">")
                        .append("<h4 class=\"subttl\">Ubicación</h4><p><code>").append(esc(c.feature)).append(".feature:").append(c.linea)
                        .append("</code> · módulo ").append(esc(c.modulo)).append("</p><h4 class=\"subttl\">Clasificación</h4><p>")
                        .append(c.defectoConocido ? "Defecto conocido de la aplicación: el caso está marcado con <code>@defecto</code> y falla a propósito hasta que se corrija."
                                : "Fallo no esperado: puede ser una regresión de la API, un cambio de datos o un problema del entorno. Requiere análisis.")
                        .append("</p><h4 class=\"subttl\">Error reportado por Karate</h4>");
                for (String e : c.errores) {
                    sb.append("<pre class=\"err\">").append(esc(recortar(e, 1500))).append("</pre>");
                }
                sb.append("</div></article>\n");
            }
            sb.append("</section>\n");
        }

        // 05 Observaciones, recomendaciones y conclusiones
        String n = fallidosList.isEmpty() ? "04" : "05";
        sb.append(seccion("observaciones", n, "Observaciones, recomendaciones y conclusiones"))
                .append("<div class=\"callout info\"><h3><span aria-hidden=\"true\">💡</span> Observaciones</h3><ul>");
        modulos.forEach((m, v) -> {
            if (v[1] > 0) {
                sb.append("<li>El módulo <b>").append(esc(m)).append("</b> tiene ").append(v[1]).append(" de ").append(v[0] + v[1])
                        .append(" casos fallidos.</li>");
            }
        });
        sb.append(lentos.isEmpty()
                ? "<li>Ningún caso superó " + fmt1(UMBRAL_LENTO_S) + " s.</li>"
                : "<li><b>Rendimiento:</b> " + lentos.size() + " caso(s) superan " + fmt1(UMBRAL_LENTO_S) + " s: "
                + lentos.stream().map(c -> esc(c.id) + " (" + fmt1(c.segundos) + " s)").collect(Collectors.joining(", "))
                + ". Los casos corren en paralelo, así que parte del tiempo puede deberse al arranque de conexiones.</li>");
        sb.append("<li>Duración promedio por caso: ").append(fmt1(casos.stream().mapToDouble(c -> c.segundos).average().orElse(0)))
                .append(" s. Reporte técnico detallado de Karate: <code>target/karate-reports/karate-summary.html</code>.</li></ul></div>");

        sb.append("<div class=\"callout warn\"><h3><span aria-hidden=\"true\">⚠️</span> Recomendaciones</h3><ol>");
        int recs = 0;
        for (Caso c : fallidosList) {
            sb.append("<li><b>").append(c.defectoConocido ? "MEDIA" : "ALTA").append("</b> · ")
                    .append(c.defectoConocido ? "Corregir el defecto conocido que valida " : "Analizar el fallo nuevo de ")
                    .append("<code>").append(esc(c.id)).append("</code> (").append(esc(c.titulo)).append(").</li>");
            recs++;
        }
        if (!lentos.isEmpty()) {
            sb.append("<li><b>MEDIA</b> · Revisar el tiempo de respuesta de los endpoints usados en los casos lentos.</li>");
            recs++;
        }
        if (recs == 0) {
            sb.append("<li><b>BAJA</b> · Mantener la suite como regresión y ampliar la cobertura con nuevos casos.</li>");
        }
        sb.append("</ol></div>");

        sb.append("<div class=\"callout concl\"><h3><span aria-hidden=\"true\">✅</span> Conclusiones</h3><ul>")
                .append("<li>Se ejecutaron <b>").append(casos.size()).append(" casos</b> (").append(results.getScenariosTotal())
                .append(" iteraciones) con una tasa de éxito de <b>").append(tasa).append(" %</b>.</li>");
        modulos.entrySet().stream().filter(e -> e.getValue()[1] == 0).map(Map.Entry::getKey).reduce((a, b) -> a + ", " + b)
                .ifPresent(ok -> sb.append("<li>Módulos sin fallos: ").append(esc(ok)).append(".</li>"));
        sb.append("<li><b>Decisión:</b> ").append(fallidos == 0
                ? "la ejecución está <b>aprobada</b>; no hay bloqueos para liberar."
                : todosConocidos
                ? "la ejecución queda <b>observada</b>; se puede liberar si los defectos conocidos están registrados y priorizados."
                : "la ejecución <b>no está aprobada</b>; hay fallos nuevos que deben corregirse o explicarse antes de liberar.")
                .append("</li></ul></div></section>\n</main>");
        return sb.toString();
    }

    private static String seccion(String id, String num, String titulo) {
        return "<section class=\"sec\" aria-labelledby=\"sec-" + id + "\"><div class=\"sec-head\"><h2 id=\"sec-" + id
                + "\"><span class=\"num\">" + num + "</span> " + titulo + "</h2></div>\n";
    }

    private static String kpi(String clase, String valor, String texto) {
        return "<div class=\"kpi " + clase + "\"><b>" + valor + "</b><span>" + esc(texto) + "</span></div>";
    }

    // ------------------------------------------------------------------ Gráficas SVG

    private static String donut(long ok, long fail) {
        double r = 70, circ = 2 * Math.PI * r;
        long total = Math.max(1, ok + fail);
        double okLen = circ * ok / total;
        return "<svg viewBox=\"0 0 200 200\" width=\"220\" height=\"220\" role=\"img\" aria-label=\"" + ok + " aprobados, " + fail + " fallidos\">"
                + "<circle cx=\"100\" cy=\"100\" r=\"70\" fill=\"none\" stroke=\"" + (fail > 0 ? RED : GREEN) + "\" stroke-width=\"26\"/>"
                + "<circle cx=\"100\" cy=\"100\" r=\"70\" fill=\"none\" stroke=\"" + GREEN + "\" stroke-width=\"26\" stroke-dasharray=\""
                + f2(okLen) + " " + f2(circ) + "\" transform=\"rotate(-90 100 100)\"/>"
                + "<text x=\"100\" y=\"98\" text-anchor=\"middle\" font-size=\"34\" font-weight=\"700\" fill=\"#0b1020\">" + pct(ok, total) + "%</text>"
                + "<text x=\"100\" y=\"122\" text-anchor=\"middle\" font-size=\"12\" fill=\"" + INK3 + "\">éxito</text></svg>";
    }

    private static String barrasApiladas(Map<String, long[]> datos) {
        int w = 520, rowH = 34, lab = 150;
        long max = datos.values().stream().mapToLong(v -> v[0] + v[1]).max().orElse(1);
        double unit = (w - lab - 50.0) / max;
        StringBuilder sb = new StringBuilder("<svg viewBox=\"0 0 " + w + " " + (rowH * datos.size() + 10) + "\" width=\"100%\" role=\"img\">");
        int i = 0;
        for (Map.Entry<String, long[]> e : datos.entrySet()) {
            int y = i++ * rowH + 6;
            long ok = e.getValue()[0], fail = e.getValue()[1];
            sb.append("<text x=\"").append(lab - 10).append("\" y=\"").append(y + 17).append("\" text-anchor=\"end\" font-size=\"13\" fill=\"#3d4566\">")
                    .append(esc(recortar(e.getKey(), 22))).append("</text>");
            sb.append("<rect x=\"").append(lab).append("\" y=\"").append(y).append("\" width=\"").append(f2(ok * unit))
                    .append("\" height=\"22\" rx=\"5\" fill=\"").append(GREEN).append("\"/>");
            if (fail > 0) {
                sb.append("<rect x=\"").append(f2(lab + ok * unit)).append("\" y=\"").append(y).append("\" width=\"").append(f2(fail * unit))
                        .append("\" height=\"22\" rx=\"5\" fill=\"").append(RED).append("\"/>");
            }
            sb.append("<text x=\"").append(f2(lab + (ok + fail) * unit + 8)).append("\" y=\"").append(y + 16)
                    .append("\" font-size=\"12\" font-family=\"JetBrains Mono, monospace\" fill=\"#0b1020\">").append(ok).append("/").append(ok + fail).append("</text>");
        }
        return sb.append("</svg>").toString();
    }

    private static String barrasVerticales(List<Caso> casos) {
        int w = 820, h = 290, padL = 46, padB = 70, top = 18;
        double max = Math.max(UMBRAL_LENTO_S, casos.stream().mapToDouble(c -> c.segundos).max().orElse(1)) * 1.15;
        double bw = (w - padL - 10.0) / Math.max(1, casos.size());
        StringBuilder sb = new StringBuilder("<svg viewBox=\"0 0 " + w + " " + h + "\" width=\"100%\" role=\"img\">");
        for (int k = 0; k <= 4; k++) {
            double v = max * k / 4, y = sy(v, max, h, top, padB);
            sb.append("<line x1=\"").append(padL).append("\" x2=\"").append(w - 10).append("\" y1=\"").append(f2(y)).append("\" y2=\"").append(f2(y))
                    .append("\" stroke=\"#e6e8f2\"/><text x=\"").append(padL - 6).append("\" y=\"").append(f2(y + 4))
                    .append("\" text-anchor=\"end\" font-size=\"11\" fill=\"").append(INK3).append("\">").append(Math.round(v)).append("s</text>");
        }
        double yu = sy(UMBRAL_LENTO_S, max, h, top, padB);
        sb.append("<line x1=\"").append(padL).append("\" x2=\"").append(w - 10).append("\" y1=\"").append(f2(yu)).append("\" y2=\"").append(f2(yu))
                .append("\" stroke=\"").append(AMBER).append("\" stroke-dasharray=\"6 4\" stroke-width=\"2\"/><text x=\"").append(w - 12)
                .append("\" y=\"").append(f2(yu - 6)).append("\" text-anchor=\"end\" font-size=\"11\" fill=\"").append(AMBER)
                .append("\">umbral ").append(fmt1(UMBRAL_LENTO_S)).append("s</text>");
        for (int i = 0; i < casos.size(); i++) {
            Caso c = casos.get(i);
            String color = c.fallo() ? RED : c.segundos > UMBRAL_LENTO_S ? AMBER : GREEN;
            double x = padL + i * bw + bw * 0.18, y = sy(c.segundos, max, h, top, padB), cx = x + bw * 0.32;
            sb.append("<rect x=\"").append(f2(x)).append("\" y=\"").append(f2(y)).append("\" width=\"").append(f2(bw * 0.64))
                    .append("\" height=\"").append(f2(h - padB - y)).append("\" rx=\"4\" fill=\"").append(color).append("\"><title>")
                    .append(esc(c.id + " " + c.titulo)).append(": ").append(fmt1(c.segundos)).append(" s</title></rect>");
            if (casos.size() <= 30) {
                sb.append("<text x=\"").append(f2(cx)).append("\" y=\"").append(f2(y - 4))
                        .append("\" text-anchor=\"middle\" font-size=\"10\" font-family=\"JetBrains Mono, monospace\" fill=\"#0b1020\">")
                        .append(fmt1(c.segundos)).append("</text>");
            }
            sb.append("<text x=\"").append(f2(cx)).append("\" y=\"").append(h - padB + 16).append("\" text-anchor=\"end\" font-size=\"10\" fill=\"#3d4566\" transform=\"rotate(-45 ")
                    .append(f2(cx)).append(" ").append(h - padB + 16).append(")\">").append(esc(recortar(c.id, 16))).append("</text>");
        }
        return sb.append("</svg>").toString();
    }

    private static double sy(double v, double max, int h, int top, int padB) {
        return top + (h - top - padB) * (1 - v / max);
    }

    // ------------------------------------------------------------------ Utilidades

    private static String cargarPlantilla() {
        try (InputStream in = ReporteGenerator.class.getResourceAsStream("plantilla-reporte.html")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró reporte/plantilla-reporte.html en el classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String recortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static long pct(long a, long total) {
        return total == 0 ? 0 : Math.round(a * 100.0 / total);
    }

    private static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String f2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
