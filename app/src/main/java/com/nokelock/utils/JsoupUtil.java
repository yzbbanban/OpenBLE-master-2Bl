package com.nokelock.utils;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class JsoupUtil {
    private static final String TAG = "JsoupUtil";

    public static List<String> downLoadData(String url) throws Exception {

        Document doc = Jsoup.connect(url).get();

        Element el = doc.getElementsByClass("table-hover").get(0);
        Elements es = el.getElementsByTag("td");
        Log.i(TAG, "downLoadData: " + es);
        List<String> name = new ArrayList<>(5);
        for (Element e : es) {
            String span = e.getElementsByTag("span").get(1).text();
            name.add(span);
            Log.i(TAG, "downLoadData: " + span);
        }
        return name;
    }
}
