package com.knowbase.ingestion.office;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTableStructureExtractorTest {

    @Test
    void extractTableTreeAssignsSeparateRegionsForNestedTables() {
        String html = """
                <table>
                  <tr><td>Outer</td><td><table><tr><td>Inner</td><td>5</td></tr></table></td></tr>
                </table>
                """;
        List<HtmlTableStructureExtractor.HtmlTableModel> models =
                HtmlTableStructureExtractor.extractTableTree(Jsoup.parse(html).selectFirst("table"), 0);
        assertEquals(2, models.size());
        assertEquals(0, models.getFirst().tableIndex());
        assertEquals(1, models.get(1).tableIndex());
        assertTrue(models.get(1).nested());
        assertEquals(0, models.get(1).parentTableRegionId());
        assertTrue(models.getFirst().rows().getFirst().cells().getFirst().text().contains("Outer"));
        assertTrue(models.getFirst().rows().getFirst().cells().get(1).text().isBlank()
                || !models.getFirst().rows().getFirst().cells().get(1).text().contains("Inner"));
    }

    @Test
    void detectsFloatingTableFromInlineStyle() {
        String html = "<table style=\"float: right;\"><tr><td>A</td></tr></table>";
        HtmlTableStructureExtractor.HtmlTableModel model =
                HtmlTableStructureExtractor.parseTable(Jsoup.parse(html).selectFirst("table"), 3, false);
        assertTrue(model.floating());
    }

    @Test
    void extractDocumentSkipsNestedFromTopLevelSelector() {
        String html = """
                <html><body>
                <table id="outer"><tr><td><table id="inner"><tr><td>X</td></tr></table></td></tr></table>
                </body></html>
                """;
        List<HtmlTableStructureExtractor.HtmlTableModel> models =
                HtmlTableStructureExtractor.extract(Jsoup.parse(html));
        Set<Integer> regionIds = models.stream().map(HtmlTableStructureExtractor.HtmlTableModel::tableIndex).collect(Collectors.toSet());
        assertEquals(2, regionIds.size());
    }
}
