package searchengine.utils.parsers;

import searchengine.dto.statistics.DtoLemma;
import searchengine.model.SiteEntity;

import java.util.List;

public interface LemmaParser
{
    void run(SiteEntity siteEntity);
    List<DtoLemma> getDtoLemmaList();
}
