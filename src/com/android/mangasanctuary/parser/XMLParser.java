package com.android.mangasanctuary.parser;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.net.Uri;

import com.android.mangasanctuary.R;
import com.android.mangasanctuary.datas.Global;
import com.android.mangasanctuary.datas.Serie;
import com.android.mangasanctuary.datas.Tome;
import com.eightmotions.apis.tools.Log;

public class XMLParser {

    public static ArrayList<Serie> parseSeriesXML(String result)
            throws Exception {
        Elements nodeList = null;
        ArrayList<Serie> series = new ArrayList<Serie>();
        Serie serie;
        try {
            Document doc = Jsoup.parse(result);

            // table collection
            nodeList = doc.getElementsByClass("collection");

            Element table = nodeList.first();

            nodeList = table.getElementsByAttributeValueStarting("class", "color_status_");

            for (Element node : nodeList) {
                if ((serie = parseSerieXML(node)) != null) series.add(serie);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return series;
    }

    private static Serie parseSerieXML(Element node) {
        Serie serie = new Serie();
        Elements nodeList = node.children();
        int i;
        for (Element tempNode : nodeList) {
            if ("td".equalsIgnoreCase(tempNode.nodeName())
                && tempNode.hasAttr("class")) {
                String c = tempNode.attr("class");
                if ("nom_serie".equalsIgnoreCase(c)) {
                    tempNode = tempNode.child(0);
                    Log.d(Global.getLogTag(XMLParser.class), "nom_serie="
                        + tempNode.ownText());
                    serie.setName(tempNode.ownText());
                }
                else if ("volume".equalsIgnoreCase(c)) {
                    Log.d(Global.getLogTag(XMLParser.class), "volume="
                        + tempNode.ownText());
                    i = Integer.parseInt(tempNode.ownText());
                    serie.setTomeCount(i);
                }
                else if ("support".equalsIgnoreCase(c)) {
                    Elements imgs = tempNode.getElementsByTag("img");
                    for (Element img : imgs) {
                        if (img.hasAttr("id")) {
                            Log.d(Global.getLogTag(XMLParser.class), "id="
                                + img.attr("id"));
                            i = Integer.parseInt(img.attr("id"));
                            serie.setId(i);
                            break;
                        }
                    }
                }
            }
        }
        String attr = null;
        Uri uri = null;
        while (true) {
            node = node.nextElementSibling();
            if (node == null || node.attr("name") == null
                || !node.attr("name").equals(Integer.toString(serie.getId())))
                break;
            nodeList = node.children();
            int id_edition = -1;
            int volume_count = -1;
            for (Element tempNode : nodeList) {
                if ("td".equalsIgnoreCase(tempNode.nodeName())
                    && tempNode.hasAttr("class")) {
                    String c = tempNode.attr("class");
                    if ("nom_serie".equalsIgnoreCase(c)) {
                        Elements anchors = tempNode.getElementsByTag("a");
                        for (Element anchor : anchors) {
                            if (anchor.hasAttr("href")) {
                                attr = anchor.attr("href");
                                try {
                                    uri = Uri.parse(new StringBuilder().append(Global.getResources().getString(R.string.MS_ROOT)).append(attr).toString());
                                    id_edition = Integer.parseInt(uri.getQueryParameter("id_edition"));
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                    else if ("volume".equalsIgnoreCase(c)) {
                        try {
                            attr = tempNode.ownText();
                            attr = attr.substring(0, attr.indexOf('/'));
                            volume_count = Integer.parseInt(attr);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (id_edition >= 0 && volume_count >= 0) {
                serie.addEdition(id_edition, volume_count);
            }
        }
        //        Log.d(Global.TAG, "next element="+node.outerHtml());

        return serie;
    }

    public static ArrayList<Tome> parseEditionXML(String result)
            throws Exception {

        Log.w(Global.getLogTag(XMLParser.class), "parseEditionXML : \n"
            + result);

        Elements nodeList = null;
        ArrayList<Tome> tomes = new ArrayList<Tome>();
        Tome tome;
        Element child;
        String sId, sNumber, sUrl;
        int iId, iNumber;
        try {
            Document doc = Jsoup.parse(result);

            nodeList = doc.getElementsByAttributeValueStarting("class", "titre_encart_");

            for (Element node : nodeList) {
                Log.d(Global.getLogTag(XMLParser.class), "span : \n"
                    + node.outerHtml());
                tome = new Tome();
                sId = sNumber = sUrl = null;
                iId = 0;
                child = node.getElementsByTag("input").first();
                if (child.hasAttr("value")) sId = child.attr("value");
                child = node.getElementsByTag("a").first();
                sNumber = child.ownText();
                sNumber = sNumber.substring(sNumber.indexOf('#') + 1).trim();

                node = node.nextElementSibling();
                Log.d(Global.getLogTag(XMLParser.class), "li : \n"
                    + node.outerHtml());
                child = node.getElementsByTag("a").first();
                child = child.getElementsByTag("img").first();
                if (child.hasAttr("src")) sUrl = child.attr("src");

                try {
                    iId = Integer.parseInt(sId);
                    tome.setId(iId);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    iNumber = Integer.parseInt(sNumber);
                    tome.setNumber(iNumber);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                tome.setIconUrl(sUrl);
                Log.i(Global.getLogTag(XMLParser.class), "  --> " + iId + ","
                    + sNumber + "," + sUrl);

                tomes.add(tome);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return tomes;
    }

}
