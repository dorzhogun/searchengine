package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Long>
{
    long countBySiteEntity(SiteEntity siteEntity);

    List<LemmaEntity> findBySiteEntity(SiteEntity siteEntityId);
    @Query(value = "SELECT l.* FROM Lemma l WHERE l.lemma IN :lemmas AND l.site_id = :site", nativeQuery = true)
    List<LemmaEntity> findLemmaListBySite(@Param("lemmas") List<String> lemmaList,
                                    @Param("site") SiteEntity site);

}
