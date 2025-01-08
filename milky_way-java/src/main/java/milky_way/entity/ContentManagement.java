package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "content_managements")
public class ContentManagement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "page_name", nullable = false)
    private String pageName;
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User creator;
}
