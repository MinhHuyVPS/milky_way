package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.utils.enums.ReportType;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "analytics_reports")
public class AnalyticsReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "report_name", nullable = false)
    private String reportName;
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;
    private LocalDateTime generatedDate;
    private LocalDateTime updateDate;
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
}
