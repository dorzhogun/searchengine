package searchengine.utils.parsers;

import searchengine.dto.statistics.DtoIndex;
import searchengine.model.SiteEntity;

import java.util.List;

public interface IndexParser
{
    void run(SiteEntity siteEntity);
    List<DtoIndex> getDtoIndexList();
}
