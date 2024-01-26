package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Data
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "index_search", indexes = {@Index(
        name = "page_id_list", columnList = "page_id"),
        @javax.persistence.Index(name = "lemma_id_list", columnList = "lemma_id")})
public class IndexSearch implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private PageEntity pageEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private LemmaEntity lemmaEntity;

    @Column(nullable = false, name = "index_rank")
    private float rank;

    public IndexSearch(PageEntity pageEntity, LemmaEntity lemmaEntity, float rank) {
        this.pageEntity = pageEntity;
        this.lemmaEntity = lemmaEntity;
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexSearch that = (IndexSearch) o;
        return id == that.id && Float.compare(that.rank, rank) == 0 && pageEntity.equals(that.pageEntity)
                && lemmaEntity.equals(that.lemmaEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pageEntity, lemmaEntity, rank);
    }

    @Override
    public String toString() {
        return "IndexEntity{" +
                "id=" + id +
                ", page=" + pageEntity +
                ", lemma=" + lemmaEntity +
                ", rank=" + rank +
                '}';
    }
}
