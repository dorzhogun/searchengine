package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class SearchResult
{
    private boolean result;
    private int count;
    private List<StatisticsSearch> data;

    public SearchResult(boolean result, int count, List<StatisticsSearch> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }
}
