package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JsonService {
    @Value("#{'${SAVE_TO_JSON_DIRECTORIES}'.split(',,,,')}")
    private List<String> SAVE_TO_JSON_DIRECTORIES;

    public void save(List<Offer> offers, String filenamePrefix) {
        if (SAVE_TO_JSON_DIRECTORIES != null && SAVE_TO_JSON_DIRECTORIES.size() != 0) {
            for (String saveToJsonDirectory : SAVE_TO_JSON_DIRECTORIES) {
                String filename = saveToJsonDirectory + "/" + filenamePrefix;
                saveInternal(offers, filename);
            }
        } else {
            saveInternal(offers, filenamePrefix);
        }
        saveInternal(offers, filenamePrefix);
    }

    private static void saveInternal(List<Offer> offers, String filenamePrefix) {
        String filename = FileUtil.getFileName(filenamePrefix, ".json");
        Gson gson = new Gson();
        String json = gson.toJson(offers);
        try (FileOutputStream file = new FileOutputStream(filename)) {
            file.write(json.getBytes());
        } catch (IOException e) {
            log.error("Could not save file!", e);
        }
    }

    public List<Offer> load(String filename) {
        List<Offer> offers = new ArrayList<>();
        Gson gson = new Gson();
        try (FileInputStream file = new FileInputStream(filename)) {
            JsonReader jsonReader = new JsonReader(new FileReader(filename));
            offers = Arrays.asList(gson.fromJson(jsonReader, Offer[].class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return offers;
    }
}
