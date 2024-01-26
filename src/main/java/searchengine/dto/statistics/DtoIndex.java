package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DtoIndex {
    long pageID;
    long lemmaID;
    float rank;
}
