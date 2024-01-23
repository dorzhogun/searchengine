package searchengine.model;

import lombok.*;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "site")
public class SiteEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", columnDefinition = "DATETIME" , nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "siteEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PageEntity> pageEntityList = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "siteEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LemmaEntity> lemmaEntityList = new ArrayList<>();

}
