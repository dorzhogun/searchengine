package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    @Query("SELECT p FROM PageEntity p WHERE p.path = :path")
    PageEntity findPageByPath(@Param("path") String path);

    Iterable<PageEntity> findBySiteEntity(SiteEntity siteEntity);

    long countBySiteEntity(SiteEntity siteEntityId);

    @Query(value = "SELECT p.* FROM Page p JOIN index_search i ON p.id = i.page_id WHERE i.lemma_id IN :lemmas", nativeQuery = true)
    List<PageEntity> findByLemmaList(@Param("lemmas") Collection<LemmaEntity> lemmaListId);
}
