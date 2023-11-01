package com.bervan.shopwebscraper.file;

import com.bervan.shopwebscraper.Offer;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class JsonService {
    public void save(List<Offer> offers, String filenamePrefix) {
        String filename = FileUtil.getFileName(filenamePrefix, ".json");
        System.out.println("Saving " + filename + "...");
        Gson gson = new Gson();
        String json = gson.toJson(offers);
        try (FileOutputStream file = new FileOutputStream(filename)) {
            file.write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Saved " + filename + ".");
    }
}
