package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma", indexes = {@Index(name = "lemma_list", columnList = "lemma")})
public class LemmaEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private SiteEntity siteEntity;

    private String lemma;
    private int frequency;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "lemmaEntity",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexSearch> indexSearchList = new ArrayList<>();

    public LemmaEntity(String lemma, int frequency, SiteEntity siteEntity) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.siteEntity = siteEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaEntity that = (LemmaEntity) o;
        return id == that.id && frequency == that.frequency &&
                siteEntity.equals(that.siteEntity) &&
                lemma.equals(that.lemma) &&
                indexSearchList.equals(that.indexSearchList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteEntity, lemma, frequency, indexSearchList);
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", sitePageId=" + siteEntity +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                ", index=" + indexSearchList +
                '}';
    }

}
