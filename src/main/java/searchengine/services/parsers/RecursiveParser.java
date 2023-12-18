package searchengine.services.parsers;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.statistics.DtoPage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class RecursiveParser extends RecursiveTask<List<DtoPage>> {
    private final String currentUrl;
    private final List<String> linksList;
    private final List<DtoPage> dtoPages;

    public RecursiveParser(String currentUrl, List<DtoPage> dtoPages, List<String> linksList) {
        this.currentUrl = currentUrl;
        this.dtoPages = dtoPages;
        this.linksList = linksList;
    }

    private Document getDocument(String currentUrl) {
        try {
            return Jsoup.connect(currentUrl).ignoreHttpErrors(true).ignoreContentType(true)
                    .userAgent("Edg/118.0.2088.46")
                    .timeout(100000).referrer("https://google.com").followRedirects(false).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Connection.Response getResponse(String currentUrl) {
        try {
            return Jsoup.connect(currentUrl).response();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    protected List<DtoPage> compute() {
        try {
            Thread.sleep(150);
            Document doc = getDocument(currentUrl);
            String html = doc.outerHtml();
            int statusCode = getResponse(currentUrl).statusCode();
            DtoPage dtoPage = new DtoPage(currentUrl, html, statusCode);
            dtoPages.add(dtoPage);
            Elements elements =doc.select("body").select("a");
            List<RecursiveParser> taskList = new ArrayList<>();
            for (Element el : elements) {
                String link = el.absUrl("href");
                if (isLink(link, el) && !linksList.contains(link)) {
                    linksList.add(link);
                    RecursiveParser task = new RecursiveParser(link, dtoPages, linksList);
                    task.fork();
                    taskList.add(task);
                }
            }
            taskList.forEach(ForkJoinTask::join);
        } catch (Exception e) {
            log.error("Parsing error with link : " + currentUrl + " exception message : " + e.getMessage());
            DtoPage dtoPage = new DtoPage(currentUrl, "", 500);
            dtoPages.add(dtoPage);
        }
        return dtoPages;
    }

    public boolean isLink(String path, Element el) {
        return path.startsWith(el.baseUri()) && !path.endsWith(".php") && !path.endsWith(".jpg") && !path.endsWith("#")
                && !path.endsWith(".png") && !path.endsWith(".pdf") && !path.endsWith("=");
    }
}