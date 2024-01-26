package searchengine.dto.statistics;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StatisticsSearch
{
    private String website;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;

    public StatisticsSearch(String website, String siteName, String uri, String title, String snippet, Float relevance) {
        this.website = website;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }
}
