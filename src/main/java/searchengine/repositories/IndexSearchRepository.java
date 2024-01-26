package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.IndexSearch;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

public interface IndexSearchRepository extends JpaRepository<IndexSearch, Long>
{
    @Query(value = "SELECT i.* FROM index_search i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages", nativeQuery = true)
    List<IndexSearch> findByPagesAndLemmas(@Param("lemmas") List<LemmaEntity> lemmaListId,
                                           @Param("pages") List<PageEntity> pageListId);

}
