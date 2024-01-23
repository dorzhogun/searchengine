package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private TotalStatistics getTotal() {
        long sites = siteRepository.count();
        long pages = pageRepository.count();
        long lemmas = lemmaRepository.count();
        return new TotalStatistics(sites, pages, lemmas, true);
    }


    private DetailedStatisticsItem getDetailed(SiteEntity site) {
        String url = site.getUrl();
        String name = site.getName();
        Status status = site.getStatus();
        LocalDateTime statusTime = site.getStatusTime();
        String error = site.getLastError();
        long pages = pageRepository.countBySiteEntity(site);
        long lemmas = lemmaRepository.countBySiteEntity(site);
        return new DetailedStatisticsItem(url, name, status, statusTime, error, pages, lemmas);
    }

    private List<DetailedStatisticsItem> getDetailedList() {
        List<SiteEntity> siteList = siteRepository.findAll();
        List<DetailedStatisticsItem> result = new ArrayList<>();
        if (!siteList.isEmpty()) {
            for (SiteEntity site : siteList) {
                DetailedStatisticsItem item = getDetailed(site);
                result.add(item);
            }
        }
        return result;
    }
    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = getTotal();
        List<DetailedStatisticsItem> detailed = getDetailedList();
        return new StatisticsResponse(true, new StatisticsData(total, detailed));
    }
}
