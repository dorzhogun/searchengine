package searchengine.utils.parsers;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.statistics.DtoPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class RecursiveParser extends RecursiveTask<List<DtoPage>> {
    private final String currentUrl;
    private final String web;
    private final List<DtoPage> dtoPages;
    List<String> currentList;
    static ConcurrentSkipListSet<String> linksListResult = new ConcurrentSkipListSet<>();

    public RecursiveParser(String currentUrl, List<DtoPage> dtoPages, String web) {
        this.currentUrl = currentUrl;
        this.web = web;
        this.dtoPages = dtoPages;
        currentList = new ArrayList<>();
    }

    public Document getDocument(String currentUrl) {
        try {
            return Jsoup.connect(currentUrl).ignoreHttpErrors(true).ignoreContentType(true)
                    .userAgent("Edg/118.0.2088.46")
                    .timeout(100000).referrer("https://google.com").followRedirects(false).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Connection.Response getResponse(String currentUrl) {
        try {
            return Jsoup.connect(currentUrl).ignoreHttpErrors(true).ignoreContentType(true)
                    .userAgent("Edg/118.0.2088.46")
                    .timeout(100000).referrer("https://google.com").followRedirects(false).execute();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public List<String> getLinksList(String currentUrl)
    {
        try {
            Thread.sleep(150);
            Document doc = Jsoup.connect(currentUrl).ignoreHttpErrors(true).ignoreContentType(true)
                    .userAgent("Edg/118.0.2088.46")
                    .timeout(100000).referrer("https://google.com").followRedirects(false).get();

            Elements elements = doc.select("body").select("a");
            for (Element element : elements) {
                String nextLink = element.absUrl("href").toLowerCase();
                if (isLink(nextLink)) {
                    currentList.add(nextLink);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return currentList;
    }

    @Override
    protected List<DtoPage> compute() {
        try {
            Thread.sleep(150);
            String html = getDocument(currentUrl).outerHtml();
            int code = getResponse(currentUrl).statusCode();
            dtoPages.add(new DtoPage(currentUrl, html, code));
            List<String> parserLinks = getLinksList(currentUrl);
            List<RecursiveParser> taskList = new ArrayList<>();
            for (String link : parserLinks) {
                if (!linksListResult.contains(link)) {
                    linksListResult.add(link);
                    RecursiveParser task = new RecursiveParser(link, dtoPages, web);
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

    public boolean isLink(String path) {
        return path.startsWith(web) && !path.isBlank() && !(path.length() < 3) && !path.contains(".zip") && !path.contains(".nc")
                && !path.contains(".php") && !path.contains(".jpg") && !path.contains("#") && !path.contains("$")
                && !path.contains(".png") && !path.contains(".pdf") && !path.contains("=") && !path.contains("%")
                && !path.contains(".docx") && !path.contains(".xlsx") && !path.contains(".fig") && !path.contains(".doc")
                && !path.contains(".pptx") && !path.contains(".jpeg");
    }
}