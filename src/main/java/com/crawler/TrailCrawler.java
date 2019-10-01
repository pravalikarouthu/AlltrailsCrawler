package com.crawler;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.SamplePro.HikingDetails;
import com.SamplePro.HikingDetails.DifficultyLevel;
import com.SamplePro.HikingRepository;

@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = HikingRepository.class)
public class TrailCrawler implements CommandLineRunner {

    private static final String BASE_URL = "https://www.alltrails.com/sitemap/secure/rails/atrails4.xml";

    @Autowired
    public HikingRepository repository;

    private List<String> getUrls() {
        Document doc = null;
        List<String> list = new ArrayList<String>();
        try {
            doc = Jsoup.connect(BASE_URL).get();
            Elements ele = doc.getElementsByTag("loc");
            for (int i = 0; i < ele.size(); i++) {
                Element e = ele.get(i);
                String a = e.html();
                if (a.contains("/washington/")) {
                    list.add(a);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private HikingDetails getPage(String url) {
        final Document doc;
        final HikingDetails hikingDetails;
        try {
                doc = Jsoup.connect(url).get();
                String hikeName = this.getHikeName(doc);
                Double distance = this.getDistance(doc);
                Integer elevation = this.getElevation(doc);

                String[] latLong = this.getLongitudeLatitude(doc);
                final Double lat;
                final Double longitude;
                if(latLong == null) {
                    lat = null;
                    longitude = null;
                } else {
                    lat = Double.valueOf(latLong[0]);
                    longitude = Double.valueOf(latLong[1]);
                }
                Double rating = this.getRating(doc);
                DifficultyLevel difficultyLevel = this.getDifficulty(doc);
                hikingDetails = new HikingDetails(distance, elevation, rating, longitude, lat, hikeName, difficultyLevel, url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return hikingDetails;
    }

    private String getHikeName(Document doc) {
        
        Elements ele = doc.getElementsByAttributeValue("itemprop", "name");
        for(int i=0;i<ele.size();i++) {
            Element e = ele.get(i);
            if(e.attr("class").equals("xlate-none") && e.tagName().equals("h1")) {
                return e.html();
            }
        }
        return null;

    }

    private Double getDistance(Document doc) {
        Elements ele = doc.getElementsByClass("detail-data xlate-none");
        String a = doc.getElementsByClass("detail-data xlate-none").get(0).html();
        for (int i = 0; i < ele.size(); i++) {
            if (a.contains("miles")) {
                a = a.replace("miles", "").trim();
                Double d = Double.valueOf(a);
                return d;
            }
        }
        return null;
    }

    private Integer getElevation(Document doc) {
        Elements ele = doc.getElementsByClass("detail-data xlate-none");
        String a = doc.getElementsByClass("detail-data xlate-none").get(0).html();
        for (int i = 0; i < ele.size(); i++) {
            if (a.contains("feet")) {
                a = a.replace("feet", "").trim();
                Integer d = Integer.valueOf(a);
                return d;
            }
        }
        return null;

    }

    private String[] getLongitudeLatitude(Document doc) {
        Elements ele = doc.getElementsByClass("bar-icon trail-directions");
        System.out.println(ele);
        for (int i = 0; i < ele.size(); i++) {
            if(!ele.get(i).attr("class").equals("icon-title")) {
               continue; 
            }
            String a = ele.attr("href");
            System.out.println(a);
            if (a != null && a.contains("google")) {
                String[] splits = a.split("//");
                String[] coordinates = splits[splits.length - 1].split(",");
                return coordinates;
            }

        }
        return null;
    }

    private Double getRating(Document doc) {
        Element ele =doc.getElementsByAttributeValue("itemprop", "aggregateRating").get(0);
        String a =ele.getElementsByAttributeValue("itemprop", "ratingvalue").get(0).attr("content");
        Double d =Double.valueOf(a);
        return d;

    }

    private DifficultyLevel getDifficulty(Document doc) {
        Element ele = doc.getElementById("title-and-difficulty");
        Elements elements = ele.getElementsByClass("diff hard selected");
        Elements elements2 = ele.getElementsByClass("diff moderate selected");

        if (elements.size() > 0) {
            return DifficultyLevel.DIFFICULT;

        } else if (elements2.size() > 0) {
            return DifficultyLevel.MODERATE;
        }

        else {
            return DifficultyLevel.EASY;
        }

    }

    public void saveToDatabase(final HikingDetails hikingDetails) throws Exception {
        HikingDetails hikeDetails = repository.findByHikeName(hikingDetails.getHikeName());
        if (hikeDetails == null) {
            System.out.println(hikingDetails);
            repository.save(hikingDetails);
        }
        Thread.sleep(2000);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(TrailCrawler.class, args);
    }

    public void run(String... args) throws Exception {
        List<String> list = null;
        list = this.getUrls();

        for(String url:list) {
            HikingDetails hikingDetails = this.getPage(url);
            this.saveToDatabase(hikingDetails);
        }
    }
}
