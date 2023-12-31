package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ExcelService {
    @Value("#{'${SAVE_TO_EXCEL_DIRECTORIES}'.split(',,,,')}")
    private List<String> SAVE_TO_EXCEL_DIRECTORIES;

    public void save(List<Offer> offers, String filenamePrefix) {
        if (SAVE_TO_EXCEL_DIRECTORIES != null && SAVE_TO_EXCEL_DIRECTORIES.size() != 0) {
            for (String saveToExcelDirectory : SAVE_TO_EXCEL_DIRECTORIES) {
                String filename = saveToExcelDirectory + "/" + filenamePrefix;
                saveInternal(offers, filename);
            }
        } else {
            saveInternal(offers, filenamePrefix);
        }
    }

    private void saveInternal(List<Offer> offers, String filenamePrefix) {
        String filename = FileUtil.getFileName(filenamePrefix, ".xlsx");

        Set<String> resultExcelColumns = new LinkedHashSet<>();
        for (Offer offer : offers) {
            resultExcelColumns.addAll(offer.keySet());
        }

        try (OutputStream os = Files.newOutputStream(Path.of(filename));
             Workbook wb = new Workbook(os, "App", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");

            Map<String, Integer> columns = new TreeMap<>();
            int colNumber = 0;
            for (String column : resultExcelColumns) {
                ws.style(0, colNumber).bold();
                ws.value(0, colNumber, column);
                columns.put(column, colNumber);
                colNumber++;
            }

            int rowNumber = 1;
            for (Offer offer : offers) {
                for (String column : resultExcelColumns) {
                    colNumber = columns.get(column);
                    Object obj = offer.get(column);
                    if (obj instanceof Date) {
                        ws.value(rowNumber, colNumber, (Date) obj);
                    } else if (obj instanceof String) {
                        ws.value(rowNumber, colNumber, (String) obj);
                    } else if (obj instanceof LocalDate) {
                        ws.value(rowNumber, colNumber, (LocalDate) obj);
                    } else if (obj instanceof LocalDateTime) {
                        ws.value(rowNumber, colNumber, (LocalDateTime) obj);
                    } else if (obj instanceof Boolean) {
                        ws.value(rowNumber, colNumber, (Boolean) obj);
                    } else if (obj instanceof List) {
                        ws.value(rowNumber, colNumber, buildStringCollectionText((List<?>) obj));
                    } else if (obj instanceof String[]) {
                        ws.value(rowNumber, colNumber, buildStringCollectionText(List.of((String[]) obj)));
                    } else if (obj != null) {
                        ws.value(rowNumber, colNumber, obj.toString());
                    }
                }
                rowNumber++;
            }
        } catch (IOException e) {
            log.error("Could not save file!", e);
        }
    }

    private static String buildStringCollectionText(List<?> obj) {
        StringBuilder s = new StringBuilder();
        if (obj.size() > 0) {
            for (Object o : obj) {
                s.append(o.toString());
                s.append(",");
            }
            return s.substring(0, s.length() - 1);
        }

        return "";
    }
}
