package com.bervan.shopwebscraper.save;

import com.bervan.shopwebscraper.Offer;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class JsonService {
    @Value("${SAVE_TO_JSON_DIRECTORY:}")
    private String SAVE_TO_JSON_DIRECTORY;

    public void save(List<Offer> offers, String filenamePrefix) {
        if (!Strings.isNullOrEmpty(SAVE_TO_JSON_DIRECTORY)) {
            filenamePrefix = SAVE_TO_JSON_DIRECTORY + "/" + filenamePrefix;
        }
        String filename = FileUtil.getFileName(filenamePrefix, ".json");
        Gson gson = new Gson();
        String json = gson.toJson(offers);
        try (FileOutputStream file = new FileOutputStream(filename)) {
            file.write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
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
