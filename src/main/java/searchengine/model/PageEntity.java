package searchengine.model;

import lombok.*;
import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "`path`"))
public class PageEntity implements Serializable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 1000, name = "`path`", columnDefinition = "VARCHAR(515)", nullable = false)
    private String path;

    @Column(name = "code", columnDefinition = "INT", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT CHARSET utf8mb4 COLLATE utf8mb4_general_ci",
            nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntity;
}
